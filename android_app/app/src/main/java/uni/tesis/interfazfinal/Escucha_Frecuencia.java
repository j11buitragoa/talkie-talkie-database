package uni.tesis.interfazfinal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Escucha_Frecuencia extends AppCompatActivity {

    private final String USERS_COLLECTION = "User";
    private final String EJERCICIOS_COLLECTION="Ejercicios";
    private long tiempoInicio;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private DocumentReference ejercicioDoc;
    private DocumentReference userDocRef;
    private boolean stopPlayback = false;

    private int currentLevel = 1, successPoints, faultPoints, currentPatternIndex;
    private int maxLevel = 4, pointsToPass = 5;
    private int[][] rangeFreq = {{500, 1000}, {350, 500}, {500, 1000}, {350, 500}};
    private final int freqCentral = 300;
    private int[][] freqDiffArray = new int[maxLevel][pointsToPass];
    private int checkCondition = 0;
    private int duration = 1000;
    private Points points;
    private long responseTime;
    private DocumentReference hearFreqDocument;
    private int[][] resultsLevel = new int[pointsToPass][4]; // Col0: resultado Col1: tiempoRespuesta Col2:freq1 Col3:freq2

    String TAG = "TAG";
    private Button sameButton, diffButton, highButton, lowButton, backButton;
    private TextView levelText, scoreText;
    private boolean instructionsVisible = false;
    private boolean instructionsHandled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escucha_frecuencia);
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
            duration = Integer.parseInt(nivel1.get(0));
            pointsToPass = Integer.parseInt(nivel1.get(3));

            for (int i = 0;i<maxLevel;i++){
                int freqMin = 0, freqMax = 0;
                switch (i){
                    case 0:
                        freqMin = Integer.parseInt(nivel1.get(1));
                        freqMax = Integer.parseInt(nivel1.get(2));
                        break;
                    case 1:
                        freqMin = Integer.parseInt(nivel2.get(1));
                        freqMax = Integer.parseInt(nivel2.get(2));
                        break;
                    case 2:
                        freqMin = Integer.parseInt(nivel3.get(1));
                        freqMax = Integer.parseInt(nivel3.get(2));
                        break;
                    case 3:
                        freqMin = Integer.parseInt(nivel4.get(1));
                        freqMax = Integer.parseInt(nivel4.get(2));
                        break;
                }
                rangeFreq[i][0] = freqMin;
                rangeFreq[i][1] = freqMax;
            }

        }

        CardView instructionsCardView = findViewById(R.id.instructionsCardView);
        Button okButton = findViewById(R.id.okf2);
        sameButton = findViewById(R.id.same_button);
        diffButton = findViewById(R.id.diff_button);
        highButton = findViewById(R.id.high_button);
        lowButton = findViewById(R.id.low_button);
        backButton = findViewById(R.id.backButton);

        levelText = findViewById(R.id.level_text);
        scoreText = findViewById(R.id.score_text);

        startLevel();

        sameButton.setOnClickListener(v -> {
            checkAnswer(0);
        });

        diffButton.setOnClickListener(v -> {
            checkAnswer(1);
        });

        highButton.setOnClickListener(v -> {
            checkAnswer(2);
        });

        lowButton.setOnClickListener(v -> {
            checkAnswer(1);
        });

        backButton.setOnClickListener(v -> {
            Intent goEscucha = new Intent(Escucha_Frecuencia.this, Escucha_Frame.class);
            startActivity(goEscucha);
            finish();

        });
    }

    private void startLevel() {

        Log.d(TAG, "Nivel " + currentLevel);
        updateUI();

        if (currentLevel <= 2){
            diffButton.setVisibility(View.VISIBLE);
            highButton.setVisibility(View.GONE);
            lowButton.setVisibility(View.GONE);

        } else {
            if (!instructionsVisible && !instructionsHandled) {
                showInstr();
                instructionsVisible = true; // Marcar las instrucciones como mostradas
            }
            diffButton.setVisibility(View.GONE);
            highButton.setVisibility(View.VISIBLE);
            lowButton.setVisibility(View.VISIBLE);
        }

        successPoints = 0;
        faultPoints = 0;
        setFrequencies();
        functionRunnablePlayFreq();
    }

    private void showInstr(){
        Log.d(TAG, "Mostrando instrucciones");

        CardView instructionsCardView = findViewById(R.id.instructionsCardView);
        Button okButton = findViewById(R.id.okf2);
        Log.d(TAG, "Visibilidad antes: " + instructionsCardView.getVisibility());

        instructionsCardView.setVisibility(View.VISIBLE);
        Log.d(TAG, "Visibilidad después: " + instructionsCardView.getVisibility());

        okButton.setOnClickListener(v -> {
            instructionsCardView.setVisibility(View.GONE);
            if (currentLevel != 4) {
                instructionsHandled = true; // Marcar las instrucciones como manejadas
                functionRunnablePlayFreq();
            }
        });
    }

    private void setFrequencies(){
        int min, max;
        for (int i = 0; i<pointsToPass;i++){
            min = rangeFreq[currentLevel-1][0]/10;
            max = rangeFreq[currentLevel-1][1]/10;
            freqDiffArray[currentLevel-1][i] = 10*getRandomInt(min, max);
            Log.d(TAG, "freqDiffArray[" + String.valueOf(currentLevel-1) + "][" + i + "] = " + freqDiffArray[currentLevel-1][i]);
        }
    }

    private void playFreq(){
        boolean isRunning =true;
        int top = 0, played = 0, gain = 1, bufferSize, times = 0, freqTone, freqTone1, freqTone2, aux;
        int sampleRate = 8000;
        int waitTime = 200;
        // int channel = getRandomInt(1, 2);
        int channel = 1;
        short[] left, right, tone;
        AudioTrack audioTrack = null;

        freqTone1 = freqCentral;
        if (getRandomInt(0,1) == 0){
            freqTone2 = freqCentral;
        }else {
            freqTone2 = freqDiffArray[currentLevel-1][currentPatternIndex];
            if (getRandomInt(0,1) == 0){
                aux = freqTone1;
                freqTone1 = freqTone2;
                freqTone2 = aux;
            }
        }

        resultsLevel[currentPatternIndex][2] = freqTone1;
        resultsLevel[currentPatternIndex][3] = freqTone2;

        if (currentLevel <= 2){
            if (freqTone1 == freqTone2) checkCondition = 0;
            else checkCondition = 1;
        }else {
            if (freqTone1 == freqTone2) checkCondition = 0;
            else if (freqTone2 < freqTone1) checkCondition = 1;
            else checkCondition = 2;
        }

        while (isRunning ){
            if (times < 2){
                if (played == top) {
                    if (times == 0){
                        freqTone = freqTone1;
                    }else {
                        freqTone = freqTone2;
                    }
                    Log.d(TAG, "Times " + times + "    Freq " + freqTone);
                    if (channel == 1) {
                        left = setTone(gain, duration, freqTone, sampleRate);
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
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
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
                    Log.d(TAG, "Entra");
                }
            }
            played = audioTrack.getPlaybackHeadPosition();
        }
        audioTrack.release();
        responseTime = System.currentTimeMillis();
    }

    private void functionRunnablePlayFreq (){
        PlayFreqRunnable playFreqRunnable = new PlayFreqRunnable();
        new Thread(playFreqRunnable).start();
    }

    private void updateUI(){
        if (levelText != null && scoreText != null) {
            levelText.setText("Level " + currentLevel);
            scoreText.setText("Score " + successPoints);
        }
    }

    private void checkAnswer(int option){
        responseTime = System.currentTimeMillis() - responseTime;
        resultsLevel[currentPatternIndex][1] = (int) responseTime;
        if (option == checkCondition){
            resultsLevel[currentPatternIndex][0] = 1;
            successPoints++;
        }else {
            resultsLevel[currentPatternIndex][0] = 0;
            faultPoints++;
        }

        updateUI();
        Log.d(TAG, "Success Points " + successPoints + "\nFault Points " + faultPoints);
        currentPatternIndex++;
        if (currentPatternIndex < pointsToPass){
            functionRunnablePlayFreq();
        }else {
            sendDataBase(user.getUid(),hearFreqDocument, resultsLevel, currentLevel, pointsToPass);
            currentPatternIndex = 0;
            if (successPoints == pointsToPass){
                // Pasa nivel
                currentLevel++;
                if (currentLevel <= maxLevel) {
                    Log.d(TAG, "Pasa de nivel");
                    addPoints(successPoints, currentLevel);
                }
            }else {
                // Repite nivel
                Log.d(TAG, "Repite nivel");
            }
            if (currentLevel <= maxLevel) {
                startLevel();
            }else {
                currentLevel = 0;
                Log.d(TAG, "Fin del juego");
                Intent goEscucha = new Intent(Escucha_Frecuencia.this, Escucha_Frame.class);
                startActivity(goEscucha);
                finish();
            }
        }
    }

    private void addPoints(int points, int level) {
        String sectionName = "Escucha_Frecuencia";
        ((Points) getApplication()).addPoints(sectionName, points);
    }
    class PlayFreqRunnable implements Runnable {
        @Override
        public void run() {
            playFreq();
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
    private int getRandomInt (int min, int max){
        int num;
        Random random = new Random();
        num = random.nextInt((max-min)+1)+min;
        return num;
    }

    private DocumentReference getEjercicioDocument(int currentLevel) {
        currentLevel=currentLevel+3;
        String nombreEjercicio = "Ejercicio_" +currentLevel;
        return db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);
    }

    private void sendDataBase(String userId, DocumentReference userdoc, int[][] matrix, int currentLevel, int pointsWIN){
        DocumentReference ejercicioDoc = getEjercicioDocument(currentLevel);
        Map<String, Object> mapa = new HashMap<>();
        // String tmp1, tmp2;
        List<Integer> tiemposRespuesta = new ArrayList<>();
        List<String> resultado = new ArrayList<>();
        List<String> frecuencias = new ArrayList<>();
        int puntos1 = 0;

        for(int i = 0;i<pointsWIN;i++){
            if (matrix[i][0] == 1) {resultado.add("Acierto");
                puntos1++;
            }
            else{ resultado.add("Falla");}
            tiemposRespuesta.add(matrix[i][1]);
            frecuencias.add(matrix[i][2] + " , " + matrix[i][3]);

        }

        mapa.put("Result Level " + currentLevel, resultado);
        mapa.put("Time Level " + currentLevel, tiemposRespuesta);
        mapa.put("ejercicio",ejercicioDoc);
        mapa.put("Selected Freq " + currentLevel, frecuencias);
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());
        mapa.put("usuario",userDocRef);
        Date fechaActual = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String fechaHora = dateFormat.format(fechaActual);
        mapa.put("fecha",fechaHora);
        mapa.put("puntos", puntos1);

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
        Log.d("Escucha_Frec", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("Escucha_Frec", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }

    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
    protected void onDestroy() {
        super.onDestroy();
    }
}