package uni.tesis.interfazfinal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private Points myApp;
    private TextView valuePoints;
    private String TAG = "TAG";
    private FirebaseAuth mAuth;
    private static final String PREFS_NAME = "TimeTrackerPrefs";
    private static final String TOTAL_TIME_KEY = "totalTime";
    private static final String START_TIME_KEY = "startTime";
    private FirebaseFirestore db;
    private FirebaseUser user,currentUser;
    private long startTime;
    private final String USERS_COLLECTION = "User";
    private DocumentReference userDocRef;
    private Button escuchaButton, hablaButton, vibrometriaButton, addButton;
    private TextView nameTittle;
    private long tiempoInicio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myApp = (Points) getApplication();
        myApp.startSession();
        tiempoInicio = System.currentTimeMillis();

        valuePoints=findViewById(R.id.valuePoints);
        myApp.addMainActivity("MainActivity",this);
        updatePointsTextView(myApp.getTotalPoints());



        escuchaButton = findViewById(R.id.sensButton);
        hablaButton = findViewById(R.id.talkButton);
        vibrometriaButton = findViewById(R.id.eqButton);
        addButton = findViewById(R.id.addButton);
        nameTittle = findViewById(R.id.mainTittle);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());

        ImageView logout=findViewById(R.id.logout);
        ImageView about=findViewById(R.id.about);
        ImageView confiInt=findViewById(R.id.confiInt);
        ImageView escucInt=findViewById(R.id.escucInt);
        ImageView hablaInt=findViewById(R.id.hablaInt);

        logout.setOnClickListener(view -> {
            // Llamada a la función de log out
            logout();
        });

        about.setOnClickListener(view -> {
            String urlVideo = "https://youtu.be/e8tVHJpOgEc";

            // Crear un Intent con la acción ACTION_VIEW
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // Establecer la URL del video en el Intent
            intent.setData(Uri.parse(urlVideo));

            // Intentar iniciar la actividad para abrir el video
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // Manejar la excepción si no se encuentra una actividad para manejar la intención
                e.printStackTrace();

                // Puedes mostrar un mensaje de error o proporcionar una alternativa aquí
            }
        });

        confiInt.setOnClickListener(v ->
                Toast.makeText(getApplicationContext(), "Esta sección te permitirá familiarizarte mejor las vibraciones de los altavoces ", Toast.LENGTH_SHORT).show());
        escucInt.setOnClickListener(v ->
                Toast.makeText(getApplicationContext(), "Esta sección te permitirá hacer un entrenamiento de la percepción de las vibraciones con los altavoces ", Toast.LENGTH_LONG).show());
        hablaInt.setOnClickListener(v ->
                Toast.makeText(getApplicationContext(), "Esta sección te permitirá entrenar el habla ", Toast.LENGTH_SHORT).show());

        if(mAuth.getCurrentUser().getEmail().equals("admin@app.com")){
        }

        userDocRef.get().addOnSuccessListener(documentSnapshot ->
                nameTittle.setText("Hola " + extraerNombre(documentSnapshot.getString("nombre"))));

        escuchaButton.setOnClickListener(view -> {
            Intent goEscucha = new Intent(this, Escucha_Frame.class);
            startActivity(goEscucha);
        });

        hablaButton.setOnClickListener(view -> {
            Intent goHabla = new Intent(this, Habla_Frame.class);
            startActivity(goHabla);
        });

        vibrometriaButton.setOnClickListener(view -> {
            Intent goVibrometria = new Intent(this, Vibrometria.class);
            startActivity(goVibrometria);
        });

        addButton.setOnClickListener(view -> {
            Intent goAdd = new Intent(this, grabaciones.class);
            startActivity(goAdd);
        });



    }
    @Override
    protected void onPause() {
        super.onPause();

    }
    @Override
    protected void onStop() {
        super.onStop();
        myApp.endSession();

    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();
        myApp.endSession();
        myApp.removeMainActivity("MainActivity");
        long tiempoSesionPrincipal = System.currentTimeMillis() - tiempoInicio;
        Log.d("MainActivity", "onDestroy - Tiempo de actividad principal: " + tiempoSesionPrincipal);
        TimeT.guardarTiempoAcumulado(this, tiempoSesionPrincipal);
        Log.d("MainActivity", "onDestroy - Tiempo acumulado antes de guardar: " + TimeT.obtenerTiempoAcumulado(this));

        // Restablecer el tiempo de inicio para la próxima sesión
        tiempoInicio = System.currentTimeMillis();

        // Limpiar el tiempo acumulado para la próxima sesión
        TimeT.limpiarTiempoAcumulado(this);
    }
    private String formatTime(long millis) {
        if (millis < 0) {
            return "00:00:00.000";
        }
        long totalMillis = millis % 1000;
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        String formattedTime = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds,totalMillis);

        return formattedTime;
    }

    private void logout() {

        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("TIEMPO TOTAL", "Tiempo total de uso antes del logout: " + tiempoSesionActual);
        Log.d("TIEMPO TOTAL", "Total de uso antes del logout: " +formatTime(tiempoSesionActual));
        Log.d("TIEMPO TOTAL", "Total acumulado: " + formatTime(TimeT.obtenerTiempoAcumulado(this)));
        String totalAcumulado = formatTime(TimeT.obtenerTiempoAcumulado(this));
        Log.d("TIEMPO TOTAL", "Total acumulado: " + totalAcumulado);


        // Restablecer el tiempo de inicio para la próxima sesión
        tiempoInicio = System.currentTimeMillis();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        //long tiempoTotal = tiempoUsoApp.obtenerTiempoTotal();
        if (currentUser != null) {




            Map<String, Object> mapa = new HashMap<>();
            myApp.removeMainActivity("MainActivity");
            int puntosGanados = myApp.getTotalPoints(); // Obtener puntos acumulados

            mapa.put("puntos_ganados", puntosGanados);
            mapa.put("total_acumulado", totalAcumulado);
            //mapa.put("tiempo_total", totalTimeInSeconds);
            //String userId = currentUser.getUid();
            DocumentReference docUsers = db.collection(USERS_COLLECTION).document(currentUser.getEmail());
            mapa.put("usuario",docUsers);
            Date fechaActual = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String fechaHora = dateFormat.format(fechaActual);
            mapa.put("fecha",fechaHora);
            db.collection("Tiempo").get().addOnSuccessListener(queryDocumentSnapshots -> {
                int num = queryDocumentSnapshots.size();
                String numT="tiempo_"+(num+1);
                db.collection("Tiempo").document(numT).set(mapa).addOnSuccessListener(documentReference->{
                    Log.d(TAG, "Datos del tiempo enviados correctamente");
                }).addOnSuccessListener(e ->{
                    //Log.e(TAG, "Error al enviar datos del intento", e);
                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al obtener la cantidad de intentos", e);
                    });
        }
        TimeT.limpiarTiempoAcumulado(this);

        mAuth.signOut();

        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Cierra la actividad actual

    }
    public void updatePointsTextView(int totalPoints) {
        // Actualizar el contenido del TextView con la puntuación total
        valuePoints.setText(myApp.getTotalPointsAsString());
    }

    private String extraerNombre(String nombreCompleto){
        String resultado;
        String[] palabras = nombreCompleto.split(" ");
        if (palabras.length > 0) {
            resultado =  palabras[0];
        }else {
            resultado = nombreCompleto;
        }
        return resultado;
    }

}