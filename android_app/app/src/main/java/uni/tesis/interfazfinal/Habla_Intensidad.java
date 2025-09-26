package uni.tesis.interfazfinal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

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

public class Habla_Intensidad extends AppCompatActivity {

    private final String USERS_COLLECTION = "User";
    private final String EJERCICIOS_COLLECTION="Ejercicios";

    private final String HEAR_COLLECTION = "HEAR";
    private final String TALK_COLLECTION = "TALK";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private CollectionReference talkCollection;
    private DocumentReference talkIntensityDocument;
    private DocumentReference ejercicioDoc;
    private DocumentReference userDocRef;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 1;
    String TAG = "TAG";

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private CircleDrawer circleDrawer;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private int circleSizePRE = 0;

    private long successStartTime = 0;  // Tiempo en que se alcanzó el objetivo
    private long successDuration = 500;
    private boolean isSuccessful = false;  // Indicador de si se alcanzó el objetivo durante el tiempo especificado
    //private static final long SUCCESS_DURATION = 1000;  // Duración en milisegundos para mostrar el éxito

    private Button backButton;
    private int okCount = 0;

    private boolean isAdminMode = false;
    private boolean bpoints=false;
    private int puntos;
    private long responseTime,responseT;
    private long startTime;
    private int currentIndex = 0, contDown = 0, contUp = 0;
    private int POINTS_TO_WIN = 10;
    private int ringSize = 300, ringWidth = 50;
    private Points points;
    private int[][] resultsLevel = new int[POINTS_TO_WIN][6]; //Col0: successDuration Col1: sizeRing Col2: widthRing Col3:durationIntento Col4: cantUP col5:cantDown
    List<int[][]> resultsLevelsList = new ArrayList<>();
    List<Long> tiemposOkList = new ArrayList<>();
    private boolean isPointCounted = false;
    private long tiempoInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habla_intensidad);
        tiempoInicio = System.currentTimeMillis();

        points = (Points) getApplication();
        points.startSession();
        startTime = System.currentTimeMillis();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());
        String nombreEjercicio = "Ejercicio_" + 12;
        ejercicioDoc = db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);

        points = (Points) getApplication();
        points.updateMainActivityUI();

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        circleDrawer = new CircleDrawer(surfaceHolder);
        backButton = findViewById(R.id.backButton);

        surfaceView.setZOrderMediaOverlay(true);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);

        backButton.setOnClickListener(v -> {
            //okCount = 0;
            Log.d(TAG, "Puntos " + puntos) ;

            if (!resultsLevelsList.isEmpty()) {
                // Envía los datos acumulados a Firebase
                for (int i = 0; i < resultsLevelsList.size(); i++) {
                    int[][] resultsLevel = resultsLevelsList.get(i);
                    sendDataBase(user.getUid(), talkIntensityDocument, resultsLevelsList);
                }
                // Limpia la lista después de enviar los datos
                resultsLevelsList.clear();
            }

            Intent goHablaMenu = new Intent(this, Habla_Frame.class);
            startActivity(goHablaMenu);
            finish();
        });

        Intent intent = getIntent();
        ArrayList<String> nivel1 = intent.getStringArrayListExtra("Nivel 1");

        if (nivel1 == null){
            Log.d(TAG, "Dato NULL");
        }else {
            successDuration = Long.parseLong(nivel1.get(0));
            ringSize = Integer.parseInt(nivel1.get(1));
            ringWidth = Integer.parseInt(nivel1.get(2));
        }

        circleDrawer.drawRingWithSize(ringSize);
        circleDrawer.drawRingWithWidth(ringWidth);


        // Check and request audio permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            startAudioCapture();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Habla_Int", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("Habla_Int", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudioCapture();
    }

    @SuppressLint("MissingPermission")
    private void startAudioCapture() {
        int sampleRate = 44100;  // Standard audio sample rate
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);

        isRecording = true;
        audioRecord.startRecording();
        Log.d(TAG, "Entra");
        new Thread(new AudioCaptureRunnable()).start();
    }

    private void stopAudioCapture() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private class AudioCaptureRunnable implements Runnable {
        private static final int MAX_INTENSITY = 32767;  // Maximum value for 16-bit audio

        @Override
        public void run() {
            short[] audioBuffer = new short[1024];
            while (isRecording) {
                int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead > 0) {
                    // Calculate intensity of the audio signal
                    double intensity = calculateIntensity(audioBuffer, bytesRead);

                    // Adjust the size of the circle based on intensity
                    adjustCircleSize(intensity);
                }
            }
        }

        private double calculateIntensity(short[] audioBuffer, int bytesRead) {
            long sum = 0;
            for (int i = 0; i < bytesRead; i++) {
                sum += Math.abs(audioBuffer[i]);
            }
            double avg = (double) sum / bytesRead;
            return avg / MAX_INTENSITY;
        }

        private void adjustCircleSize(double intensity) {
            final int factorCircleSize = 3000;  // Maximum circle size in pixels
            final int minCircleSize = 10;   // Minimum circle size in pixels
            final int difCircleSize = 20;
            int circleSizePOS = (int) (minCircleSize + intensity * factorCircleSize);

            if (Math.abs(circleSizePOS - circleSizePRE) > difCircleSize)
            {
                runOnUiThread(() -> {
                    circleDrawer.drawCircle(circleSizePOS);

                });
                circleSizePRE = circleSizePOS;
            }
        }
    }

    private class CircleDrawer {
        private SurfaceHolder surfaceHolder;
        private SuccessMessage successMessage;
        private int ringSize = 0;
        private int ringWidth = 0;

        CircleDrawer(SurfaceHolder holder) {
            surfaceHolder = holder;
            successMessage = new SuccessMessage(holder);
        }

        void drawCircle(int size){
            successMessage.drawSuccessMessage(size,ringSize,ringWidth);
        }

        void drawRingWithSize(int size){
            ringSize = size;
            successMessage.drawSuccessMessage(0,ringSize,ringWidth);
        }

        void drawRingWithWidth(int width){
            ringWidth = width;
            successMessage.drawSuccessMessage(0,ringSize,ringWidth);
        }
    }
    private DocumentReference getEjercicioDocument(int level) {
        String nombreEjercicio = "Ejercicio_" + 12;
        return db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);
    }
    private void sendDataBase(String userId, DocumentReference userdoc, List<int[][]> resultsLevelsList){
        for (int i = 0; i < resultsLevelsList.size(); i++) {
            int[][] resultsLevel = resultsLevelsList.get(i);
            DocumentReference ejercicioDoc = getEjercicioDocument(i);
            Map<String, Object> mapa = new HashMap<>();
            // String tmp1, tmp2;
            List<Long> duration = new ArrayList<>();
            List<String> size = new ArrayList<>();
            List<String> width = new ArrayList<>();

            for (int j = 0; j < resultsLevel.length; j++) {
                duration.add((long) resultsLevel[j][0]);
                size.add(String.valueOf(resultsLevel[j][1]));
                width.add(String.valueOf(resultsLevel[j][2]));
            }

            mapa.put("Duration", duration);
            mapa.put("Size Ring", size);
            mapa.put("Width Ring", width);
            mapa.put("ejercicio", ejercicioDoc);
            mapa.put("puntos",puntos);
            Log.d(TAG, "Puntos en add" + puntos) ;
            mapa.put("tiemposOkList", tiemposOkList);

            Log.d(TAG, "Duration: " + duration.toString());
            Log.d(TAG, "Size Ring: " + size.toString());
            Log.d(TAG, "Width Ring: " + width.toString());
            Log.d(TAG, "Duration desde el mapa: " + mapa.get("Duration"));
            Log.d(TAG, "Size Ring desde el mapa: " + mapa.get("Size Ring"));
            Log.d(TAG, "tiemposOkList desde el mapa: " + mapa.get("tiemposOklist"));
            DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());
            mapa.put("usuario", userDocRef);
            Date fechaActual = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String fechaHora = dateFormat.format(fechaActual);
            mapa.put("fecha", fechaHora);
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
    }

    private class SuccessMessage {

        //private static final int RING_WIDTH = 0;  // Ancho del anillo
        //private static final int SUCCESS_THRESHOLD = 600;  // Tamaño objetivo del círculo
        private long tiempoObtencionPunto = 0;

        private SurfaceHolder surfaceHolder;

        SuccessMessage(SurfaceHolder holder) {
            surfaceHolder = holder;
        }

        void drawSuccessMessage(int circleSize, int ringSize, int ringWidth) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.WHITE);
                int centerX = canvas.getWidth() / 2;
                int centerY = canvas.getHeight() / 2;

                // Dibujar círculo
                Paint circlePaint = new Paint();
                circlePaint.setColor(Color.argb(255,119,158,203));
                canvas.drawCircle(centerX, centerY, circleSize / 2, circlePaint);

                // Dibujar anillo
                if (ringSize > 0){
                    Paint ringPaint = new Paint();
                    ringPaint.setColor(Color.argb(100,189,236,182));
                    ringPaint.setStyle(Paint.Style.STROKE);
                    ringPaint.setStrokeWidth(ringWidth);
                    canvas.drawCircle(centerX, centerY, ringSize / 2, ringPaint);
                }

                if ((circleSize >= (ringSize - ringWidth/2)) && ((circleSize <= (ringSize + ringWidth/2)))) {
                    contDown = 0;
                    contUp = 0;
                    if (!isSuccessful) {

                        successStartTime = System.currentTimeMillis();

                        isSuccessful = true;
                        isPointCounted = false; // Reiniciar la bandera de conteo de punto
                    } else if (System.currentTimeMillis() - successStartTime >= successDuration) {
                        // Cumplido, sigiuente nivel
                        bpoints=true;
                        if (!isPointCounted) {  // Verificar si ya se contó un punto
                            puntos++; // Incrementar puntos solo si no se ha contado antes
                            isPointCounted = true; // Marcar que ya se contó un punto
                            long tiempoActual = System.currentTimeMillis();

                            tiempoObtencionPunto = tiempoActual - tiempoInicio;
                            Log.d(TAG, "Tiempo obtencion punto: " + tiempoObtencionPunto);
                            tiemposOkList.add(tiempoObtencionPunto);

                            tiempoInicio = tiempoActual;


                        }
                        drawSuccessTextOK(canvas, centerX, centerY);

                        int[][] currentResult = new int[POINTS_TO_WIN][3];
                        currentResult[currentIndex][0] = (int) successDuration;
                        currentResult[currentIndex][1] = ringSize;
                        currentResult[currentIndex][2] = ringWidth;

                        // Incrementa el conteo de "OK"


                        resultsLevelsList.add(currentResult);
                        //okCount = 0;

                        currentIndex++;

                        /*resultsLevel[0][0] = (int) successDuration;
                        resultsLevel[0][1] = ringSize;
                        resultsLevel[0][2] = ringWidth;
                        sendDataBase(user.getUid(),talkIntensityDocument, resultsLevel, 1, POINTS_TO_WIN);*/
                            if(currentIndex>=POINTS_TO_WIN){

                                //sendDataBase(user.getUid(),talkIntensityDocument, resultsLevel, currentIndex, POINTS_TO_WIN);
                            }
                        else {
                            currentIndex = 0;
                        }
                    }
                } else if (circleSize > (ringSize + ringWidth/2)) {
                    // abajo
                    drawSuccessTextDOWN(canvas, centerX, centerY);
                    contDown++;
                    isSuccessful = false;
                    isSuccessful = false;
                    if(bpoints){
                        Log.d(TAG, "Puntos en add" + puntos) ;
                        bpoints=false;
                        addPoints(puntos);
                    }
                }else {
                    // arriba
                    contUp++;
                    drawSuccessTextUP(canvas, centerX, centerY);
                    isSuccessful = false;
                }

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        private void addPoints(int points) {
            String sectionName = "Escucha_Intensidad";
            ((Points) getApplication()).addPoints(sectionName, points);
            //sendDataBase(user.getUid(),talkIntensityDocument, resultsLevel, 1, POINTS_TO_WIN);

        }

        private void drawSuccessTextOK(Canvas canvas, int centerX, int centerY) {
            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(80);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("OK", centerX, centerY, textPaint);

        }

        private void drawSuccessTextUP(Canvas canvas, int centerX, int centerY) {
            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(40);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("UP", centerX, centerY, textPaint);
        }

        private void drawSuccessTextDOWN(Canvas canvas, int centerX, int centerY) {
            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(40);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("DOWN", centerX, centerY, textPaint);
        }
    }

}