package uni.tesis.interfazfinal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class TimeT {
    public static void guardarTiempoAcumulado(Context context, long tiempo) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long tiempoAcumulado = sharedPreferences.getLong("tiempo_acumulado", 0);
        tiempoAcumulado += tiempo;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("tiempo_acumulado", tiempoAcumulado);
        editor.apply();
    }

    public static long obtenerTiempoAcumulado(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong("tiempo_acumulado", 0);
    }
    public static void limpiarTiempoAcumulado(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("tiempo_acumulado");
        editor.apply();
    }
}
