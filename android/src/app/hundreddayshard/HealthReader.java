package app.hundreddayshard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
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
 * body weight, steps, sleep and exercise sessions (with the calories burned during them). Each type is read only if its permission was granted.
 */
final class HealthReader {
    static final String NUTRITION = "android.permission.health.READ_NUTRITION";
    static final String WEIGHT = "android.permission.health.READ_WEIGHT";
    static final String STEPS = "android.permission.health.READ_STEPS";
    static final String SLEEP = "android.permission.health.READ_SLEEP";
    static final String EXERCISE = "android.permission.health.READ_EXERCISE";
    static final String ACTIVE_CAL = "android.permission.health.READ_ACTIVE_CALORIES_BURNED";
    static final String TOTAL_CAL = "android.permission.health.READ_TOTAL_CALORIES_BURNED";
    static final String[] PERMISSIONS = {NUTRITION, WEIGHT, STEPS, SLEEP, EXERCISE, ACTIVE_CAL, TOTAL_CAL};
    private static final String[] NAMES = {"food", "weight", "steps", "sleep", "workouts", "active calories", "calories burned"};

    private static final Map<Integer, String> TYPES = new HashMap<>();
    // Session types that are lifting rather than cardio. One of these on the same day as a workout saved
    // in the app counts as that same workout.
    private static final List<Integer> STRENGTH = new ArrayList<>();
    static {
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING, "Biking");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY, "Stationary bike");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_POOL, "Swimming");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER, "Open-water swim");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_ROWING, "Rowing");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_ROWING_MACHINE, "Rowing machine");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_RACQUETBALL, "Racquetball");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_TENNIS, "Tennis");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_SKIING, "Skiing");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_SNOWBOARDING, "Snowboarding");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_HIKING, "Hiking");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING, "Walking");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING, "Running");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING_TREADMILL, "Treadmill run");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_ELLIPTICAL, "Elliptical");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING, "Stairs");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE, "Stair machine");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING, "HIIT");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_YOGA, "Yoga");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_PILATES, "Pilates");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON, "Badminton");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_SQUASH, "Squash");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_TABLE_TENNIS, "Table tennis");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_BASKETBALL, "Basketball");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_SOCCER, "Soccer");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_VOLLEYBALL, "Volleyball");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_BOXING, "Boxing");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_MARTIAL_ARTS, "Martial arts");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_DANCING, "Dancing");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_GOLF, "Golf");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT, "Workout");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_STRENGTH_TRAINING, "Strength training");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING, "Weightlifting");
        TYPES.put(ExerciseSessionType.EXERCISE_SESSION_TYPE_CALISTHENICS, "Calisthenics");
        STRENGTH.add(ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT);
        STRENGTH.add(ExerciseSessionType.EXERCISE_SESSION_TYPE_STRENGTH_TRAINING);
        STRENGTH.add(ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING);
        STRENGTH.add(ExerciseSessionType.EXERCISE_SESSION_TYPE_CALISTHENICS);
    }
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
        // day -> app that wrote it -> sessions
        Map<String, Map<String, JSONArray>> workouts = new TreeMap<>();

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
                        readAll(c, hcm, EXERCISE, ExerciseSessionRecord.class, range, ex, sessions ->
                        readAll(c, hcm, ACTIVE_CAL, ActiveCaloriesBurnedRecord.class, range, ex, active ->
                        readAll(c, hcm, TOTAL_CAL, TotalCaloriesBurnedRecord.class, range, ex, total -> {
                            for (ExerciseSessionRecord w : sessions) {
                                try {
                                    JSONObject o = new JSONObject();
                                    int type = w.getExerciseType();
                                    o.put("name", TYPES.containsKey(type) ? TYPES.get(type) : "Workout");
                                    o.put("strength", STRENGTH.contains(type));
                                    o.put("start", w.getStartTime().toEpochMilli());
                                    o.put("min", Duration.between(w.getStartTime(), w.getEndTime()).toMinutes());
                                    double kcal = activeDuring(active, w);
                                    if (kcal <= 0) kcal = totalDuring(total, w);
                                    if (kcal > 0) o.put("kcal", Math.round(kcal));
                                    o.put("auto", w.getMetadata().getRecordingMethod() == Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED);
                                    String day = w.getStartTime().atZone(zone).toLocalDate().toString();
                                    String origin = w.getMetadata().getDataOrigin().getPackageName();
                                    workouts.computeIfAbsent(day, k -> new HashMap<>()).computeIfAbsent(origin == null ? "" : origin, k -> new JSONArray()).put(o);
                                } catch (Exception ignored) { }
                            }
                            done.accept(toJson(food, weightKg, steps, sleep, workouts, fromIso));
                        })));
                    });
                });
            });
        });
    }

    // Calories recorded inside a session's time window (a minute of slack either side).
    private static boolean inside(Instant s, Instant e, ExerciseSessionRecord w) {
        return !s.isBefore(w.getStartTime().minusSeconds(60)) && !e.isAfter(w.getEndTime().plusSeconds(60));
    }

    private static double activeDuring(List<ActiveCaloriesBurnedRecord> list, ExerciseSessionRecord w) {
        double sum = 0;
        for (ActiveCaloriesBurnedRecord r : list) if (inside(r.getStartTime(), r.getEndTime(), w)) sum += r.getEnergy().getInCalories() / 1000.0;
        return sum;
    }

    private static double totalDuring(List<TotalCaloriesBurnedRecord> list, ExerciseSessionRecord w) {
        double sum = 0;
        for (TotalCaloriesBurnedRecord r : list) if (inside(r.getStartTime(), r.getEndTime(), w)) sum += r.getEnergy().getInCalories() / 1000.0;
        return sum;
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
                                 Map<String, Map<String, double[]>> steps, Map<String, Map<String, double[]>> sleep,
                                 Map<String, Map<String, JSONArray>> workouts, String fromIso) {
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
            for (Map.Entry<String, Map<String, JSONArray>> e : workouts.entrySet()) {
                Map<String, JSONArray> origins = e.getValue();
                JSONArray list = origins.containsKey(SAMSUNG_HEALTH) ? origins.get(SAMSUNG_HEALTH) : new JSONArray();
                if (!origins.containsKey(SAMSUNG_HEALTH)) for (JSONArray a : origins.values()) for (int i = 0; i < a.length(); i++) list.put(a.get(i));
                day(days, e.getKey()).put("sessions", list);
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
