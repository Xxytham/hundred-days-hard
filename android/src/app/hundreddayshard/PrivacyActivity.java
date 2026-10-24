package app.hundreddayshard;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shown by Health Connect when you ask why 100 Days Hard wants health data. */
public class PrivacyActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView t = new TextView(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        t.setPadding(pad, pad, pad, pad);
        t.setTextSize(16);
        t.setText("How 100 Days Hard uses your health data\n\n"
                + "The app reads your daily nutrition (calories and protein), body weight, steps, sleep and workouts (with the calories burned and distance covered during them) "
                + "from Health Connect, so you don't have to type in what Samsung Health already knows, "
                + "and so your daily tasks can tick themselves.\n\n"
                + "It only reads. It never writes to Health Connect. The numbers stay on this phone, in the app and in "
                + "its backup file in Download/100 Days Hard. Nothing is sent to any server.\n\n"
                + "You can turn access off at any time in Health Connect.");
        ScrollView s = new ScrollView(this);
        s.addView(t);
        setContentView(s);
    }
}
