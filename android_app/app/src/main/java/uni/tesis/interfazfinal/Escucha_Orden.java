package uni.tesis.interfazfinal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.icu.util.Calendar;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Escucha_Orden extends AppCompatActivity {

    private String TAG = "TAG";
    private final String USERS_COLLECTION = "User";
    private final String EJERCICIOS_COLLECTION="Ejercicios";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private DocumentReference hearOrderDocument;
    private DocumentReference ejercicioDoc;
    private DocumentReference userDocRef;
    private int currentLevel = 1;
    private int currentPatternIndex = 0, currentPatternIndex2 = 0;
    private int successCount = 0, faultCount = 0;
    private int[] pointsToPassLevelList = {3, 3, 4, 5};
    private int[][] rangeMs = {
            {1000, 500}, // minMsL1, maxMsL1, waitTimeL1
            {500, 300},  // minMsL2, maxMsL2, waitTimeL1
            {200, 200},
            {100, 200}
    };
    private int maxLevel = 4;

    // UI Variables
    private Button leftButton, rightButton, backButton;
    private TextView levelTextView, scoreTextView;

    // Audio Variables
    private AudioTrack audioTrack;
    private int sampleRate = 8000, waitTime = 500, bufferSize;
    private int[] freqToneList = {300, 300, 300, 300};
    private short[] left, right, tone;
    private double gain = 1;
    private long tiempoInicio;

    private Points points;

    private long responseTime;
    private int[][][] patterns; //Col 1: Canal, Col 2: Duration
    private int[][] resultsLevel; // Col0: Patron Col1: Resultados

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escucha_orden);
        tiempoInicio = System.currentTimeMillis();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());

        points = (Points) getApplication();
        points.updateMainActivityUI();

        String nombreEjercicio = "Ejercicio_" + currentLevel;
        ejercicioDoc = db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);

        Intent intent = getIntent();
        ArrayList<String> nivel1 = intent.getStringArrayListExtra("Nivel 1");
        ArrayList<String> nivel2 = intent.getStringArrayListExtra("Nivel 2");
        ArrayList<String> nivel3 = intent.getStringArrayListExtra("Nivel 3");
        ArrayList<String> nivel4 = intent.getStringArrayListExtra("Nivel 4");

        if (nivel1 == null || nivel2 == null || nivel3 == null || nivel4 == null){
            Log.d(TAG, "Dato NULL");
        }else {
            Log.d(TAG,"Entra");
            for (int i = 0;i<maxLevel;i++){
                int duracionTono = 0, freqTono = 0, qtyTonos = 0;
                switch (i){
                    case 0:
                        duracionTono = Integer.parseInt(nivel1.get(0));
                        freqTono = Integer.parseInt(nivel1.get(1));
                        qtyTonos = Integer.parseInt(nivel1.get(2));
                        break;
                    case 1:
                        duracionTono = Integer.parseInt(nivel2.get(0));
                        freqTono = Integer.parseInt(nivel2.get(1));
                        qtyTonos = Integer.parseInt(nivel2.get(2));
                        break;
                    case 2:
                        duracionTono = Integer.parseInt(nivel3.get(0));
                        freqTono = Integer.parseInt(nivel3.get(1));
                        qtyTonos = Integer.parseInt(nivel3.get(2));
                        break;
                    case 3:
                        duracionTono = Integer.parseInt(nivel4.get(0));
                        freqTono = Integer.parseInt(nivel4.get(1));
                        qtyTonos = Integer.parseInt(nivel4.get(2));
                        break;
                }

                //Guardar en variables globales
                rangeMs[i][0] = duracionTono;
                freqToneList[i] = freqTono;
                pointsToPassLevelList[i] = qtyTonos;
            }
        }

        leftButton = findViewById(R.id.left_button);
        rightButton = findViewById(R.id.right_button);
        levelTextView = findViewById(R.id.level_text);
        scoreTextView = findViewById(R.id.score_text);
        backButton = findViewById(R.id.backButton);

        leftButton.setOnClickListener(v -> checkPattern(1));
        rightButton.setOnClickListener(v -> checkPattern(2));
        backButton.setOnClickListener(v -> {
            Intent goEscucha = new Intent(Escucha_Orden.this, Escucha_Frame.class);
            startActivity(goEscucha);
            finish();
        });

        Toast.makeText(Escucha_Orden.this, "Comienza en 3 seg", Toast.LENGTH_SHORT).show();

        waitTime(3000);
        setPatterns();
        startLevel();

    }

    private void startLevel() {
        Log.d(TAG,"Inicia Level " + currentLevel);
        successCount = 0;
        faultCount = 0;
        currentPatternIndex = 0;

        PatternRunnable patternRunnable = new PatternRunnable();
        new Thread(patternRunnable).start();

        levelTextView.setText("Level " + currentLevel);
        scoreTextView.setText("Score " + successCount);
    }
    private void checkPattern(int channel){
        Log.d(TAG,"Inicia checkPattern");
        resultsLevel[currentPatternIndex2][1] = channel;
        if (channel == patterns[currentLevel-1][currentPatternIndex2][0]){
            successCount++;
        }else {
            faultCount++;
        }
        if (successCount+faultCount >= pointsToPassLevelList[currentLevel-1]){
            responseTime = System.currentTimeMillis() - responseTime;
            sendDataBase(user.getUid(),hearOrderDocument, resultsLevel, currentLevel, pointsToPassLevelList[currentLevel-1]);
            if (successCount == pointsToPassLevelList[currentLevel-1]){
                currentLevel++;
                if (currentLevel <= maxLevel){
                    Toast.makeText(Escucha_Orden.this, "PASAS DE NIVEL", Toast.LENGTH_SHORT).show();
                    waitTime(2000);
                    setPatterns();
                    startLevel();
                    addPoints( currentLevel);
                }
                else {
                    Toast.makeText(Escucha_Orden.this, "Fin del juego", Toast.LENGTH_SHORT).show();
                    Intent goEscucha = new Intent(Escucha_Orden.this, Escucha_Frame.class);
                    startActivity(goEscucha);
                }
            }else {
                Toast.makeText(Escucha_Orden.this, "REPITES NIVEL", Toast.LENGTH_SHORT).show();
                waitTime(2000);
                setPatterns();
                startLevel();
            }
            // waitTime(3000);
        }
        currentPatternIndex2++;
    }

    private void addPoints( int level) {
        String sectionName = "Escucha_Orden";
        ((Points) getApplication()).addPoints(sectionName, level);
    }
    private void playPattern(){
        boolean isRunning = true;
        int top = 0, played = 0;

        while (isRunning){
            if (currentPatternIndex < pointsToPassLevelList[currentLevel-1]){
                if (played == top) {
                    waitTime(1000);
                    if (patterns[currentLevel-1][currentPatternIndex][0] == 1) {
                        left = setTone(gain, patterns[currentLevel-1][currentPatternIndex][1], freqToneList[currentLevel-1], sampleRate);
                        right = new short[left.length];
                    } else {
                        right = setTone(gain, patterns[currentLevel-1][currentPatternIndex][1], freqToneList[currentLevel-1], sampleRate);
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
                    waitTime(rangeMs[currentLevel-1][1]);
                    audioTrack.play();
                    currentPatternIndex++;
                }
            }else {
                if (played == top){
                    currentPatternIndex2 = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leftButton.setEnabled(true);
                            rightButton.setEnabled(true);
                            scoreTextView.setText("Score " + successCount);
                        }
                    });
                    isRunning = false;
                }
            }
            played = audioTrack.getPlaybackHeadPosition();
        }
        responseTime = System.currentTimeMillis();
    }
    private void setPatterns(){
        Random random = new Random();

        //AQUI
        patterns = new int[maxLevel][pointsToPassLevelList[currentLevel-1]][2];
        resultsLevel = new int[pointsToPassLevelList[currentLevel-1]][2];

        leftButton.setEnabled(false);
        rightButton.setEnabled(false);

        for (int j = 0; j<pointsToPassLevelList[currentLevel-1];j++){
            patterns[currentLevel-1][j][0] = random.nextInt(2)+1;
            patterns[currentLevel-1][j][1] = rangeMs[currentLevel-1][0];
            resultsLevel[j][0] = patterns[currentLevel-1][j][0];
        }
        // Imprimir
        Log.d(TAG, "Set Level " + currentLevel);
        for (int j = 0; j < pointsToPassLevelList[currentLevel-1]; j++) {
            Log.d(TAG, "" + patterns[currentLevel-1][j][0] + "    " + patterns[currentLevel-1][j][1]);
        }
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
    private void waitTime(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    class PatternRunnable implements Runnable {
        @Override
        public void run() {
            playPattern();
        }
    }

    private DocumentReference getEjercicioDocument(int currentLevel) {
        currentLevel=currentLevel+7;
        String nombreEjercicio = "Ejercicio_" + currentLevel;
        return db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);
    }
    private void sendDataBase(String userId,DocumentReference userdoc, int[][] matrix, int level, int pointsWIN){
        DocumentReference ejercicioDoc = getEjercicioDocument(level);

        Map<String, Object> mapa = new HashMap<>();
        List<String> resultado = new ArrayList<>();
        List<String> patron = new ArrayList<>();
        int puntos1 = 0;
        for(int i = 0;i<pointsWIN;i++){
            if (matrix[i][0] == 1) patron.add("L");
            else patron.add("R");
            if (matrix[i][1] == 1) resultado.add("L");
            else resultado.add("R");
            // Comparación entre Result Level y Pattern Level
            if (resultado.equals(patron)) {
                puntos1=1;
            }

        }

        mapa.put("Result Level " + level, resultado);
        mapa.put("Time Level " + level, responseTime);
        mapa.put("Pattern Level " + level, patron);
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
        stopAudioPlayback();
        Log.d("Escucha_Ord", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("Escucha_Ord", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }

    public void onBackPressed() {
        super.onBackPressed();
        stopAudioPlayback();
        finish();

    }

    private void stopAudioPlayback() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        stopAudioPlayback();
    }

}