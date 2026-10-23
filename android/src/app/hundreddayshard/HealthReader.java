package app.hundreddayshard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.WeightRecord;
import android.os.Build;
import android.os.OutcomeReceiver;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Reads daily food totals and body weight that Samsung Health shares through Health Connect (Android 14+). */
final class HealthReader {
    static final String NUTRITION = "android.permission.health.READ_NUTRITION";
    static final String WEIGHT = "android.permission.health.READ_WEIGHT";
    static final String[] PERMISSIONS = {NUTRITION, WEIGHT};
    private static final String SAMSUNG_HEALTH = "com.sec.android.app.shealth";

    private HealthReader() { }

    static String status(Context c) {
        if (Build.VERSION.SDK_INT < 34) return "unavailable";
        try {
            if (c.getSystemService(HealthConnectManager.class) == null) return "unavailable";
        } catch (Throwable t) {
            return "unavailable";
        }
        return c.checkSelfPermission(NUTRITION) == PackageManager.PERMISSION_GRANTED ? "granted" : "not_granted";
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

        ReadRecordsRequestUsingFilters<NutritionRecord> food =
                new ReadRecordsRequestUsingFilters.Builder<>(NutritionRecord.class).setTimeRangeFilter(range).setPageSize(5000).build();
        hcm.readRecords(food, ex, new OutcomeReceiver<ReadRecordsResponse<NutritionRecord>, HealthConnectException>() {
            @Override
            public void onResult(ReadRecordsResponse<NutritionRecord> res) {
                // day -> app that wrote it -> {calories, protein grams, has protein}
                Map<String, Map<String, double[]>> byDay = new TreeMap<>();
                for (NutritionRecord n : res.getRecords()) {
                    String day = n.getStartTime().atZone(zone).toLocalDate().toString();
                    String origin = n.getMetadata().getDataOrigin().getPackageName();
                    double[] t = byDay.computeIfAbsent(day, k -> new HashMap<>())
                            .computeIfAbsent(origin == null ? "" : origin, k -> new double[3]);
                    if (n.getEnergy() != null) t[0] += n.getEnergy().getInCalories();
                    if (n.getProtein() != null) { t[1] += n.getProtein().getInGrams(); t[2] = 1; }
                }
                if (c.checkSelfPermission(WEIGHT) == PackageManager.PERMISSION_GRANTED) {
                    readWeight(hcm, range, zone, ex, byDay, done);
                } else {
                    done.accept(toJson(byDay, new HashMap<>()));
                }
            }

            @Override
            public void onError(HealthConnectException e) {
                done.accept(error(e));
            }
        });
    }

    private static void readWeight(HealthConnectManager hcm, TimeInstantRangeFilter range, ZoneId zone, Executor ex,
                                   Map<String, Map<String, double[]>> food, Consumer<String> done) {
        ReadRecordsRequestUsingFilters<WeightRecord> req =
                new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class).setTimeRangeFilter(range).setPageSize(1000).build();
        hcm.readRecords(req, ex, new OutcomeReceiver<ReadRecordsResponse<WeightRecord>, HealthConnectException>() {
            @Override
            public void onResult(ReadRecordsResponse<WeightRecord> res) {
                // Latest weigh-in of each day, in kg.
                Map<String, Object[]> latest = new HashMap<>();
                for (WeightRecord w : res.getRecords()) {
                    String day = w.getTime().atZone(zone).toLocalDate().toString();
                    Object[] cur = latest.get(day);
                    if (cur == null || w.getTime().isAfter((Instant) cur[0])) {
                        latest.put(day, new Object[]{w.getTime(), w.getWeight().getInGrams() / 1000.0});
                    }
                }
                Map<String, Double> kg = new HashMap<>();
                for (Map.Entry<String, Object[]> e : latest.entrySet()) kg.put(e.getKey(), (Double) e.getValue()[1]);
                done.accept(toJson(food, kg));
            }

            @Override
            public void onError(HealthConnectException e) {
                done.accept(toJson(food, new HashMap<>()));
            }
        });
    }

    private static String toJson(Map<String, Map<String, double[]>> food, Map<String, Double> weightKg) {
        try {
            JSONObject days = new JSONObject();
            for (Map.Entry<String, Map<String, double[]>> e : food.entrySet()) {
                // Prefer Samsung Health's numbers so food logged in two apps isn't counted twice.
                Map<String, double[]> origins = e.getValue();
                double cal = 0, protein = 0;
                boolean hasProtein = false;
                if (origins.containsKey(SAMSUNG_HEALTH)) {
                    double[] t = origins.get(SAMSUNG_HEALTH);
                    cal = t[0]; protein = t[1]; hasProtein = t[2] > 0;
                } else {
                    for (double[] t : origins.values()) { cal += t[0]; protein += t[1]; hasProtein |= t[2] > 0; }
                }
                JSONObject d = new JSONObject();
                d.put("kcal", Math.round(cal / 1000.0)); // Health Connect stores energy in small calories
                if (hasProtein) d.put("protein", Math.round(protein));
                days.put(e.getKey(), d);
            }
            for (Map.Entry<String, Double> e : weightKg.entrySet()) {
                JSONObject d = days.optJSONObject(e.getKey());
                if (d == null) { d = new JSONObject(); days.put(e.getKey(), d); }
                d.put("weightKg", e.getValue());
            }
            JSONObject out = new JSONObject();
            out.put("ok", true);
            out.put("days", days);
            return out.toString();
        } catch (Exception ex) {
            return "{\"ok\":false,\"error\":\"format\"}";
        }
    }

    private static String error(HealthConnectException e) {
        String code = e.getErrorCode() == HealthConnectException.ERROR_SECURITY ? "permission" : "read";
        return "{\"ok\":false,\"error\":\"" + code + "\"}";
    }
}
