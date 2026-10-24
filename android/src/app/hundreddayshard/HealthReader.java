package app.hundreddayshard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.WeightRecord;
import android.os.Build;
import android.os.OutcomeReceiver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Reads what Samsung Health shares through Health Connect (Android 14+): daily food totals,
 * body weight, steps and sleep. Each type is read only if its permission was granted.
 */
final class HealthReader {
    static final String NUTRITION = "android.permission.health.READ_NUTRITION";
    static final String WEIGHT = "android.permission.health.READ_WEIGHT";
    static final String STEPS = "android.permission.health.READ_STEPS";
    static final String SLEEP = "android.permission.health.READ_SLEEP";
    static final String[] PERMISSIONS = {NUTRITION, WEIGHT, STEPS, SLEEP};
    private static final String[] NAMES = {"food", "weight", "steps", "sleep"};
    private static final String SAMSUNG_HEALTH = "com.sec.android.app.shealth";

    private HealthReader() { }

    private static boolean has(Context c, String permission) {
        return c.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    /** "granted" once any type is shared, so a partial grant still syncs what it can. */
    static String status(Context c) {
        if (Build.VERSION.SDK_INT < 34) return "unavailable";
        try {
            if (c.getSystemService(HealthConnectManager.class) == null) return "unavailable";
        } catch (Throwable t) {
            return "unavailable";
        }
        for (String p : PERMISSIONS) if (has(c, p)) return "granted";
        return "not_granted";
    }

    /** Names of the types not shared yet, as a JSON array. */
    static String missing(Context c) {
        JSONArray out = new JSONArray();
        if (!"unavailable".equals(status(c))) {
            for (int i = 0; i < PERMISSIONS.length; i++) if (!has(c, PERMISSIONS[i])) out.put(NAMES[i]);
        }
        return out.toString();
    }

    /** Reads every page of one record type, or nothing if its permission is missing. */
    private static <T extends Record> void readAll(Context c, HealthConnectManager hcm, String permission, Class<T> type,
                                                   TimeInstantRangeFilter range, Executor ex, Consumer<List<T>> done) {
        List<T> acc = new ArrayList<>();
        if (!has(c, permission)) {
            done.accept(acc);
            return;
        }
        readPage(hcm, type, range, ex, acc, -1, done);
    }

    private static <T extends Record> void readPage(HealthConnectManager hcm, Class<T> type, TimeInstantRangeFilter range,
                                                    Executor ex, List<T> acc, long token, Consumer<List<T>> done) {
        ReadRecordsRequestUsingFilters.Builder<T> b =
                new ReadRecordsRequestUsingFilters.Builder<>(type).setTimeRangeFilter(range).setPageSize(5000);
        if (token != -1) b.setPageToken(token);
        hcm.readRecords(b.build(), ex, new OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException>() {
            @Override
            public void onResult(ReadRecordsResponse<T> res) {
                acc.addAll(res.getRecords());
                long next = res.getNextPageToken();
                if (next != -1 && acc.size() < 200000) readPage(hcm, type, range, ex, acc, next, done);
                else done.accept(acc);
            }

            @Override
            public void onError(HealthConnectException e) {
                done.accept(acc);
            }
        });
    }

    static void read(Context c, String fromIso, String toIso, Executor ex, Consumer<String> done) {
        if (!"granted".equals(status(c))) {
            done.accept("{\"ok\":false,\"error\":\"permission\"}");
            return;
        }
        HealthConnectManager hcm = c.getSystemService(HealthConnectManager.class);
        ZoneId zone = ZoneId.systemDefault();
        Instant start = LocalDate.parse(fromIso).atStartOfDay(zone).toInstant();
        Instant end = LocalDate.parse(toIso).plusDays(1).atStartOfDay(zone).toInstant();
        TimeInstantRangeFilter range = new TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build();
        // Sleep that ends on the first day started the evening before.
        TimeInstantRangeFilter sleepRange = new TimeInstantRangeFilter.Builder()
                .setStartTime(start.minus(Duration.ofDays(1))).setEndTime(end).build();

        // day -> app that wrote it -> totals
        Map<String, Map<String, double[]>> food = new TreeMap<>();
        Map<String, Map<String, double[]>> steps = new TreeMap<>();
        Map<String, Map<String, double[]>> sleep = new TreeMap<>();
        Map<String, Double> weightKg = new HashMap<>();

        readAll(c, hcm, NUTRITION, NutritionRecord.class, range, ex, nutrition -> {
            for (NutritionRecord n : nutrition) {
                double[] t = slot(food, n.getStartTime(), zone, n.getMetadata().getDataOrigin().getPackageName(), 3);
                if (n.getEnergy() != null) t[0] += n.getEnergy().getInCalories();
                if (n.getProtein() != null) { t[1] += n.getProtein().getInGrams(); t[2] = 1; }
            }
            readAll(c, hcm, WEIGHT, WeightRecord.class, range, ex, weights -> {
                Map<String, Instant> latest = new HashMap<>();
                for (WeightRecord w : weights) {
                    String day = w.getTime().atZone(zone).toLocalDate().toString();
                    if (!latest.containsKey(day) || w.getTime().isAfter(latest.get(day))) {
                        latest.put(day, w.getTime());
                        weightKg.put(day, w.getWeight().getInGrams() / 1000.0);
                    }
                }
                readAll(c, hcm, STEPS, StepsRecord.class, range, ex, stepRecords -> {
                    for (StepsRecord s : stepRecords) {
                        slot(steps, s.getStartTime(), zone, s.getMetadata().getDataOrigin().getPackageName(), 1)[0] += s.getCount();
                    }
                    readAll(c, hcm, SLEEP, SleepSessionRecord.class, sleepRange, ex, nights -> {
                        for (SleepSessionRecord n : nights) {
                            // Count sleep on the morning it ends, minus time awake.
                            double min = Duration.between(n.getStartTime(), n.getEndTime()).toMinutes();
                            for (SleepSessionRecord.Stage st : n.getStages()) {
                                int type = st.getType();
                                if (type == SleepSessionRecord.StageType.STAGE_TYPE_AWAKE
                                        || type == SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_IN_BED
                                        || type == SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_OUT_OF_BED) {
                                    min -= Duration.between(st.getStartTime(), st.getEndTime()).toMinutes();
                                }
                            }
                            slot(sleep, n.getEndTime(), zone, n.getMetadata().getDataOrigin().getPackageName(), 1)[0] += Math.max(0, min);
                        }
                        done.accept(toJson(food, weightKg, steps, sleep, fromIso));
                    });
                });
            });
        });
    }

    private static double[] slot(Map<String, Map<String, double[]>> map, Instant when, ZoneId zone, String origin, int size) {
        String day = when.atZone(zone).toLocalDate().toString();
        return map.computeIfAbsent(day, k -> new HashMap<>()).computeIfAbsent(origin == null ? "" : origin, k -> new double[size]);
    }

    /** Samsung Health's numbers when it wrote any that day, so data copied into two apps isn't counted twice. */
    private static double[] pick(Map<String, double[]> origins, int size) {
        if (origins.containsKey(SAMSUNG_HEALTH)) return origins.get(SAMSUNG_HEALTH);
        double[] sum = new double[size];
        for (double[] t : origins.values()) for (int i = 0; i < size; i++) sum[i] = i == 2 ? Math.max(sum[i], t[i]) : sum[i] + t[i];
        return sum;
    }

    private static JSONObject day(JSONObject days, String key) throws Exception {
        JSONObject d = days.optJSONObject(key);
        if (d == null) { d = new JSONObject(); days.put(key, d); }
        return d;
    }

    private static String toJson(Map<String, Map<String, double[]>> food, Map<String, Double> weightKg,
                                 Map<String, Map<String, double[]>> steps, Map<String, Map<String, double[]>> sleep, String fromIso) {
        try {
            JSONObject days = new JSONObject();
            for (Map.Entry<String, Map<String, double[]>> e : food.entrySet()) {
                double[] t = pick(e.getValue(), 3);
                JSONObject d = day(days, e.getKey());
                d.put("kcal", Math.round(t[0] / 1000.0)); // Health Connect stores energy in small calories
                if (t[2] > 0) d.put("protein", Math.round(t[1]));
            }
            for (Map.Entry<String, Double> e : weightKg.entrySet()) day(days, e.getKey()).put("weightKg", e.getValue());
            for (Map.Entry<String, Map<String, double[]>> e : steps.entrySet()) day(days, e.getKey()).put("steps", Math.round(pick(e.getValue(), 1)[0]));
            for (Map.Entry<String, Map<String, double[]>> e : sleep.entrySet()) {
                if (e.getKey().compareTo(fromIso) >= 0) day(days, e.getKey()).put("sleepMin", Math.round(pick(e.getValue(), 1)[0]));
            }
            JSONObject out = new JSONObject();
            out.put("ok", true);
            out.put("days", days);
            return out.toString();
        } catch (Exception ex) {
            return "{\"ok\":false,\"error\":\"format\"}";
        }
    }
}
