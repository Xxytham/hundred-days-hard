package app.hundreddayshard;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * About once an hour, reads today's numbers from Samsung Health (Health Connect) while the app is closed, so the
 * widget stays current and goal notifications can go out. Needs Health Connect's "background" permission on Android 15+.
 */
public class SyncJob extends JobService {
    private static final int ID = 100;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    static void schedule(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null || js.getPendingJob(ID) != null) return;
        js.schedule(new JobInfo.Builder(ID, new ComponentName(c, SyncJob.class))
                .setPeriodic(60 * 60 * 1000L, 20 * 60 * 1000L)
                .setPersisted(true)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        String today = Today.today();
        try {
            HealthReader.read(this, today, today, IO, json -> {
                Today.fromHealth(this, json);
                jobFinished(params, false);
            });
        } catch (Throwable t) { // not allowed to read in the background, or Health Connect missing
            Widget.refresh(this);
            return false;
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
