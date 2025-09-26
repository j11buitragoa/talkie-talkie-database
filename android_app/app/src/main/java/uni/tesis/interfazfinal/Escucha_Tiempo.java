package uni.tesis.interfazfinal;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Calendar;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;


import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Escucha_Tiempo extends AppCompatActivity {

    private TextView levelTextView, pointsTextView, failsTextView;
    private Button readyButton, toneButton1, toneButton2, backButton;
    private String TAG = "TAG";
    private final String USERS_COLLECTION = "User";
    private  final String EJERCICIOS_COLLECTION="Ejercicios";
    private final String TALK_COLLECTION = "TALK";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private CollectionReference hearCollection;
    private DocumentReference hearTimeDocument;

    private int level = 1;
    private int points = 0, currentIndex = 0;
    private int fails = 0;

    private boolean isReadyLevel = false;
    private boolean enableLevel = false;
    private boolean enableTones = false;
    private long tiempoInicio;


    //private boolean isPlaying = false;

    private final int SAMPLE_RATE = 4025;
    private final int NUM_LEVELS = 3;
    private int POINTS_TO_WIN = 4;
    private int durationInMs = 500;
    private int timeBetweenTones = 1000;
    private int freqTone = 300;
    private int numTones = 2;
    private long responseTime;
    private DocumentReference ejercicioDoc;
    private DocumentReference userDocRef;
    private Points myApp;


    private int[][] resultsLevel = new int[POINTS_TO_WIN][2]; // Col0: resultado Col1: tiempoRespuesta


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escucha_tiempo);
        tiempoInicio = System.currentTimeMillis();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());

        String nombreEjercicio = "Ejercicio_" + level;
         ejercicioDoc = db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);

        Log.d(TAG, "user " + user.getDisplayName() + "\nID " + user.getUid());
        myApp = (Points) getApplication();
        myApp.updateMainActivityUI();

        Intent intent = getIntent();
        ArrayList<String> nivel1 = intent.getStringArrayListExtra("Nivel 1");
        ArrayList<String> nivel2 = intent.getStringArrayListExtra("Nivel 2");
        ArrayList<String> nivel3 = intent.getStringArrayListExtra("Nivel 3");

        if (nivel1 == null || nivel2 == null || nivel3 == null){
            Log.d(TAG, "Dato NULL");
        }else {
            durationInMs = Integer.parseInt(nivel1.get(0));
            freqTone = Integer.parseInt(nivel1.get(1));
            timeBetweenTones = Integer.parseInt(nivel1.get(2));
            POINTS_TO_WIN = Integer.parseInt(nivel1.get(3));
        }

        levelTextView = findViewById(R.id.levelTextView);
        pointsTextView = findViewById(R.id.pointsTextView);
        // failsTextView = findViewById(R.id.failsTextView);
        readyButton = findViewById(R.id.readyButton);
        toneButton1 = findViewById(R.id.toneButton1);
        toneButton2 = findViewById(R.id.toneButton2);
        backButton = findViewById(R.id.backButton);

        // Set click listeners for buttons
        readyButton.setOnClickListener(v -> startGame());

        toneButton1.setOnClickListener(v -> checkAnswer(1));

        toneButton2.setOnClickListener(v -> checkAnswer(2));

        backButton.setOnClickListener(v -> {
            Intent goEscuchaMenu = new Intent(Escucha_Tiempo.this, Escucha_Frame.class);
            startActivity(goEscuchaMenu);
            finish();
        });
    }
    private void startGame() {
        points = 0;
        fails = 0;
        isReadyLevel = true;
        currentIndex = 0;
        calculateTimeBetweenTones();
        updateUI();
        //readyButton.setEnabled(false);
        startLevel();
    }
    private void startLevel(){
        enableLevel = false;
        enableTones = true;
        updateUI();
        playTones();
    }
    private void playTones(){

        Log.d(TAG, "Current Index " + currentIndex);
        boolean isRunning =true;
        int top = 0, played = 0, gain = 1, bufferSize, times = 0;
        numTones = getRandomInt(1, 2);
        int sampleRate = 8000;
        int duration = 1000, waitTime = 200;
        // int channel = getRandomInt(1, 2);
        int channel = 1;
        short[] left, right, tone;
        AudioTrack audioTrack = null;

        while (isRunning){
            if (times < numTones){
                if (played == top) {
                    if (times == 0){
                        waitTime = 2000;
                    }else {
                        waitTime = timeBetweenTones;
                    }
                    Log.d(TAG, "Times " + times + "    Freq " + freqTone);
                    if (channel == 1) {
                        left = setTone(gain, durationInMs, freqTone, sampleRate);
                        right = new short[left.length];
                    } else {
                        right = setTone(gain, duration, freqTone, sampleRate);
                        left = new short[right.length];
                    }
                    tone = stereoSound(left, right);
                    bufferSize = 2 * tone.length;
                    top = (int) (bufferSize * 0.25);

                    audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize,
                            AudioTrack.MODE_STATIC);

                    audioTrack.write(tone, 0, tone.length);
                    waitTime(waitTime);
                    audioTrack.play();

                    times++;
                }
            }else {
                if (played == top){
                    /*
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leftButton.setEnabled(true);
                            rightButton.setEnabled(true);
                            scoreTextView.setText("Score " + successCount);
                        }
                    });

                     */
                    isRunning = false;
                }
            }
            played = audioTrack.getPlaybackHeadPosition();
        }
        responseTime = System.currentTimeMillis();
    }
    private void waitTime(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private int getRandomInt (int min, int max){
        int num;
        Random random = new Random();
        num = random.nextInt((max-min)+1)+min;
        return num;
    }
    private short[] setTone(double gain, int durationTimeMs, int freq, int sampleRate) {
        int toneSize = (int)(sampleRate * durationTimeMs * 0.001);
        short[] tone = new short[toneSize];
        for (int i = 0; i < toneSize; i++) {
            double t = (double) i / sampleRate;
            tone[i] = (short) (gain * Math.sin(2 * Math.PI * freq * t) * Short.MAX_VALUE);
        }
        return tone;
    }
    private short[] stereoSound(short[] left, short[] right){

        int stereoSoundSize;
        //asignacion tamaño array estereo
        if (left.length > right.length){
            stereoSoundSize = 2*left.length;
        }else {
            stereoSoundSize = 2*right.length;
        }

        short[] stereoSoundArray = new short[stereoSoundSize];

        //inicializar array
        for (int i = 0; i<stereoSoundSize; i++){
            stereoSoundArray[i]=0;
        }
        //left
        for (int i = 0; i<left.length; i++){
            stereoSoundArray[2*i]=left[i];
        }
        //right
        for (int i = 0; i<right.length; i++){
            stereoSoundArray[2*i+1]=right[i];
        }

        return stereoSoundArray;
    }
    private void calculateTimeBetweenTones () {
        int initialSeparation = 1000;  //
        int separationChangePerLevel = 450;  // Tasa de cambio por nivel.

        // Calcula la separación en función del nivel actual.
        timeBetweenTones = initialSeparation - (level - 1) * separationChangePerLevel;

        // Asegúrate de que la separación no sea menor que un valor mínimo.
        int minSeparation = 50;  // Valor mínimo de separación permitido.
        if (timeBetweenTones < minSeparation) {
            timeBetweenTones = minSeparation;
        }
    }
    private void addPoints(int points, int level) {
        Log.d("Points", "Adding points: " + points + " for level: " + level);
        String sectionName = "Escucha_Tiempo";
        ((Points) getApplication()).addPoints(sectionName, points);
    }
    private void checkAnswer(int selected) {

        responseTime = System.currentTimeMillis() - responseTime;
        resultsLevel[currentIndex][1] = (int) responseTime;

        if (selected == numTones) {
            resultsLevel[currentIndex][0] = 1;
            points++;
        } else {
            resultsLevel[currentIndex][0] = 0;
            fails++;
        }

        if (currentIndex+1 == POINTS_TO_WIN) {
            currentIndex = 0;
            sendDataBase(user.getUid(),hearTimeDocument, resultsLevel, level, POINTS_TO_WIN);
            if (points == POINTS_TO_WIN){
                // Pasa de nivel
                addPoints(POINTS_TO_WIN, level);
                level++;
                points = 0;
                enableTones = false;
                isReadyLevel = false;
                enableLevel = true;
            }else {
                // Repite nivel
            }
        }else{
            currentIndex++;
        }

        updateUI();

        if (level > NUM_LEVELS) {
            // El usuario ha completado el nivel 3 con éxito
            showCongratulationsDialog();
            Intent goEscuchaMenu = new Intent(Escucha_Tiempo.this, Escucha_Frame.class);
            startActivity(goEscuchaMenu);
        }else {
            startLevel();
        }
    }
    private void updateUI() {
        levelTextView.setText("Level " + level);
        pointsTextView.setText("Score " + points);
        // failsTextView.setText("Fallos: " + fails);

        if (level <= NUM_LEVELS && points < POINTS_TO_WIN) {
            // readyButton.setText("Listo para el nivel " + level);

        } else {
            readyButton.setText("Juego completado");
        }

        readyButton.setEnabled(enableLevel);
        // Disable tone buttons when not ready or playing
        toneButton1.setEnabled(enableTones);
        toneButton2.setEnabled(enableTones);
    }
    private void showCongratulationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("¡Felicitaciones!");
        builder.setMessage("Has completado el juego. ¿Quieres jugar de nuevo?");
        builder.setPositiveButton("Reiniciar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                level = 1;
                startGame();
            }
        });
        builder.setNegativeButton("Salir", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // finish(); // Cierra la aplicación
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private DocumentReference getEjercicioDocument(int level) {
        String nombreEjercicio = "Ejercicio_" + level;
        return db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);
    }
    private void sendDataBase(String userId, DocumentReference userdoc, int[][] matrix, int level, int pointsWIN){
        DocumentReference ejercicioDoc = getEjercicioDocument(level);
        Map<String, Object> mapa = new HashMap<>();

        // String tmp1, tmp2;
        List<Integer> tiemposRespuesta = new ArrayList<>();
        List<String> resultado = new ArrayList<>();
        int puntos1 = 0;
        for(int i = 0;i<pointsWIN;i++){
            if (matrix[i][0] == 1) {
                resultado.add("Acierto");
                puntos1++;
            }
            else {resultado.add("Falla");}
            tiemposRespuesta.add(matrix[i][1]);

        }

        mapa.put("Result Level " + level, resultado);
        mapa.put("Time Level " + level, tiemposRespuesta);
        mapa.put("ejercicio",ejercicioDoc);
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());
        mapa.put("usuario",userDocRef);
        Date fechaActual = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String fechaHora = dateFormat.format(fechaActual);
        mapa.put("fecha",fechaHora);
        mapa.put("puntos", puntos1);

        // Agrega el nuevo intento a la colección "Intentos"
        db.collection("Intentosf")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int numIntentos = queryDocumentSnapshots.size();
                    String intentoNombre = "Intento_" + (numIntentos + 1);

                    db.collection("Intentosf")
                            .document(intentoNombre)
                            .set(mapa)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Datos del intento enviados correctamente");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al enviar datos del intento", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener la cantidad de intentos", e);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Escucha_Time", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("Escucha_Time", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myApp.removeMainActivity("Escucha_Tiempo");
    }
}