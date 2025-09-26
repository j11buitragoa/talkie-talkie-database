package uni.tesis.interfazfinal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TimeTracker {
    public static final String PREFS_NAME = "TimeTrackerPrefs";
    private static final String TOTAL_TIME_KEY = "totalTime";
    public static final String START_TIME_KEY = "startTime";
    private static long currentStartTime = 0;

    public static void startTracking(Context context, String activityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Solo inicia el tiempo si no se ha iniciado previamente
        if (currentStartTime == 0) {
            currentStartTime = SystemClock.elapsedRealtime();
            editor.putLong(getStartTimeKey(activityName), currentStartTime);
        }
        editor.apply();
    }
    public static void stopTracking(Context context, String activityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long startTime = prefs.getLong(getStartTimeKey(activityName), 0);

        if (startTime != 0) {
            long endTime = SystemClock.elapsedRealtime();
            long elapsedTime = endTime - startTime;

            // Obtenemos la clave específica de la actividad para el tiempo total
            String totalTimeKey = getTotalTimeKey(activityName);

            SharedPreferences.Editor editor = prefs.edit();

            long totalMillis = prefs.getLong(totalTimeKey, 0) + elapsedTime;
            editor.putLong(totalTimeKey, totalMillis); // Actualiza el tiempo total en milisegundos
            editor.putLong(getStartTimeKey(activityName), 0);
            editor.apply();
        }
    }
    public static String getTotalTimeKey(String activityName) {
        return TOTAL_TIME_KEY + "_" + activityName;
    }
    public static String getStartTimeKey(String activityName) {
        return START_TIME_KEY + "_" + activityName;
    }
    public static long getTotalTime(Context context, String activityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(getTotalTimeKey(activityName), 0);
    }

    public static void resetTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Obtén todas las claves de tiempo total y restablécelas a 0
        Map<String, ?> allPrefs = prefs.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            if (entry.getKey().startsWith(TOTAL_TIME_KEY)) {
                editor.putLong(entry.getKey(), 0);
            }
        }

        editor.apply();
    }
}
