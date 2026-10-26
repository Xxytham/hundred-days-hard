package app.hundreddayshard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Locale;

/** Home-screen widget: challenge day, streak, calories and protein against their goals, and the next goal step. */
public class Widget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        refresh(c);
    }

    @Override
    public void onEnabled(Context c) {
        SyncJob.schedule(c);
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, Widget.class));
        if (ids == null || ids.length == 0) return;
        JSONObject s = Today.read(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        int day = Today.dayNumber(s), total = s.optInt("total", 100);
        boolean setUp = s.has("startDate");
        v.setTextViewText(R.id.w_day, setUp ? "Day " + Math.max(1, Math.min(day, total)) : "100 Days Hard");
        v.setTextViewText(R.id.w_of, setUp ? "of " + total : "");
        int streak = Today.today().equals(s.optString("date")) ? s.optInt("streak", 0) : s.optInt("streakYesterday", s.optInt("streak", 0));
        v.setTextViewText(R.id.w_streak, streak > 0 ? streak + "-day streak" : "");

        double kcal = Today.num(s, "kcal"), goal = s.optDouble("calorieGoal", 2500), protein = Today.num(s, "protein"), pg = s.optDouble("proteinGoal", 150);
        boolean lose = !"build".equals(s.optString("aim"));
        double scale = lose ? (goal + 100) * 1.1 : goal;
        String state = lose ? (kcal > goal + 100 ? "over" : kcal >= goal - 100 ? "met" : "under") : (kcal >= goal ? "met" : "under");
        v.setTextViewText(R.id.w_kcal, String.format(Locale.US, "%,d", Math.round(kcal)));
        v.setTextViewText(R.id.w_kcal_goal, lose ? String.format(Locale.US, " / %,d–%,d", Math.round(goal - 100), Math.round(goal + 100)) : String.format(Locale.US, " / %,d", Math.round(goal)));
        v.setProgressBar(R.id.w_kcal_bar, 1000, (int) Math.min(1000, scale > 0 ? kcal / scale * 1000 : 0), false);
        v.setTextViewText(R.id.w_protein, Math.round(protein) + " g");
        v.setTextViewText(R.id.w_protein_goal, " / " + Math.round(pg) + " g");
        v.setProgressBar(R.id.w_protein_bar, 1000, (int) Math.min(1000, pg > 0 ? protein / pg * 1000 : 0), false);
        if (Build.VERSION.SDK_INT >= 31) {
            int color = "over".equals(state) ? 0xFFFF5263 : "met".equals(state) ? 0xFF4CC489 : 0xFF5E9CFF;
            v.setColorStateList(R.id.w_kcal_bar, "setProgressTintList", ColorStateList.valueOf(color));
        }
        String next = s.optString("nextGoal", "");
        v.setTextViewText(R.id.w_next, next.isEmpty() ? "" : "Next: " + next);
        v.setViewVisibility(R.id.w_next, next.isEmpty() ? View.GONE : View.VISIBLE);

        PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.w_root, open);
        m.updateAppWidget(ids, v);
    }
}
