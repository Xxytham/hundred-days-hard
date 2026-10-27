package app.hundreddayshard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Today's numbers for the widget and goal notifications, kept outside the page so they work while the app is closed.
 * The page writes a snapshot whenever it changes (day, targets, food, streak, next goal step); the hourly background
 * sync (SyncJob) adds Samsung Health's latest food, steps and sleep, then says so when one of them reaches its goal.
 */
final class Today {
    private static final String PREFS = "app", KEY = "today", SENT = "goalsSent", CHANNEL = "goals";

    private Today() { }

    static JSONObject read(Context c) {
        try {
            return new JSONObject(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static void write(Context c, JSONObject snap) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, snap.toString()).apply();
    }

    static String today() { return LocalDate.now().toString(); }

    /** Challenge day for today, counted from the snapshot's start date. */
    static int dayNumber(JSONObject s) {
        try {
            return (int) ChronoUnit.DAYS.between(LocalDate.parse(s.getString("startDate")), LocalDate.now()) + 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /** The snapshot's food and activity numbers only count on the day they were written. */
    static double num(JSONObject s, String key) {
        return today().equals(s.optString("date")) ? s.optDouble(key, 0) : 0;
    }

    /** From the page: save it, refresh the widget, and treat goals already met as told (the user saw them in the app). */
    static void fromPage(Context c, String json) {
        try {
            JSONObject s = new JSONObject(json);
            write(c, s);
            for (String g : met(s)) markSent(c, g);
            Widget.refresh(c);
        } catch (Exception ignored) { }
    }

    /** From the background sync: merge today's Samsung Health numbers, refresh the widget, notify new goals. */
    static void fromHealth(Context c, String json) {
        try {
            JSONObject res = new JSONObject(json);
            if (!res.optBoolean("ok")) return;
            JSONObject d = res.getJSONObject("days").optJSONObject(today());
            if (d == null) return;
            JSONObject s = read(c);
            if (!today().equals(s.optString("date"))) { // first numbers of a new day
                s.put("date", today());
                for (String k : new String[] {"kcal", "protein", "steps", "sleepMin", "tasksDone"}) s.remove(k);
                s.put("kcalSrc", "");
            }
            if (!"manual".equals(s.optString("kcalSrc"))) {
                if (d.has("kcal")) s.put("kcal", d.getDouble("kcal"));
                if (d.has("protein")) s.put("protein", d.getDouble("protein"));
            }
            if (d.has("steps")) s.put("steps", d.getDouble("steps"));
            if (d.has("sleepMin")) s.put("sleepMin", d.getDouble("sleepMin"));
            write(c, s);
            Widget.refresh(c);
            if (!s.optBoolean("notify") || MainActivity.visible) return;
            for (String g : met(s)) {
                if (wasSent(c, g)) continue;
                markSent(c, g);
                notify(c, g, s);
            }
        } catch (Exception ignored) { }
    }

    /** Which of today's goals are met: protein, calories, steps, sleep. */
    static java.util.List<String> met(JSONObject s) {
        java.util.List<String> out = new java.util.ArrayList<>();
        double kcal = num(s, "kcal"), goal = s.optDouble("calorieGoal", 0);
        if (num(s, "protein") > 0 && num(s, "protein") >= s.optDouble("proteinGoal", Double.MAX_VALUE)) out.add("protein");
        if (kcal > 0 && goal > 0 && ("build".equals(s.optString("aim")) ? kcal >= goal : kcal >= goal - 100 && kcal <= goal + 100)) out.add("calories");
        if (num(s, "steps") > 0 && num(s, "steps") >= s.optDouble("stepsTarget", Double.MAX_VALUE)) out.add("steps");
        if (num(s, "sleepMin") > 0 && num(s, "sleepMin") >= s.optDouble("sleepTarget", 99) * 60) out.add("sleep");
        return out;
    }

    /** Today's goals already announced, for the page (so it doesn't congratulate twice). */
    static String sentToday(Context c) {
        String sent = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SENT, ""), out = "";
        for (String g : new String[] {"protein", "calories", "steps", "sleep"}) if (sent.contains("|" + today() + ":" + g + "|")) out += (out.isEmpty() ? "" : ",") + g;
        return out;
    }

    private static boolean wasSent(Context c, String goal) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SENT, "").contains("|" + today() + ":" + goal + "|");
    }

    private static void markSent(Context c, String goal) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String sent = p.getString(SENT, "");
        if (!sent.startsWith("|" + today())) sent = ""; // only today's are kept
        if (!sent.contains("|" + today() + ":" + goal + "|")) sent += "|" + today() + ":" + goal + "|";
        p.edit().putString(SENT, sent).apply();
    }

    private static void notify(Context c, String goal, JSONObject s) {
        if (c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Goals hit", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("A congratulations when you hit your protein, calories, steps or sleep goal");
            nm.createNotificationChannel(ch);
        }
        // A different line each day, so it never reads like the same alert twice.
        int v = LocalDate.now().getDayOfYear() % 2;
        String title, text, kcal = String.format(java.util.Locale.US, "%,d", Math.round(num(s, "kcal"))), steps = String.format(java.util.Locale.US, "%,d", Math.round(num(s, "steps")));
        switch (goal) {
            case "protein": title = "Protein goal hit \uD83D\uDCAA";
                text = v == 0 ? Math.round(num(s, "protein")) + " g of protein today. That's how muscle gets built. Nice work!"
                        : "You reached " + Math.round(s.optDouble("proteinGoal")) + " g of protein. Your next workout will thank you."; break;
            case "calories":
                if ("build".equals(s.optString("aim"))) { title = "Calorie goal reached \uD83C\uDFAF"; text = kcal + " calories in. That's fuel for growing. Well done!"; }
                else { title = "Right in your calorie range \uD83C\uDFAF"; text = v == 0 ? kcal + " calories so far, right where you want to be. Nicely judged!" : kcal + " calories and on target. Hold it there and today's a win."; }
                break;
            case "steps": title = "Step goal crushed \uD83D\uDC5F";
                text = v == 0 ? steps + " steps today. Every one of them counts. Great job!" : steps + " steps and counting. Way to keep moving!"; break;
            default: long m = Math.round(num(s, "sleepMin")); title = "Well rested \uD83D\uDE34";
                text = (m / 60) + " h " + (m % 60) + " m of sleep last night. A great start to the day!"; break;
        }
        PendingIntent open = PendingIntent.getActivity(c, 3, new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_stat_plate).setColor(0xFFCF2539)
                .setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build();
        nm.notify(goal.hashCode(), n);
    }
}
