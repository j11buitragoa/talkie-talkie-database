package uni.tesis.interfazfinal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class duracion extends AppCompatActivity {
    private boolean isDetectionEnabled = false;
    private List<Long> tiemposPorRepeticion = new ArrayList<>();

    private static int MIC_PERMISSION_CODE = 200;
    private String TAG = "TAG";
    private TextView textViewStatus;
    private double energyThreshold = 10000000.0;
    private double energyThreshold2 = 100000000000.0;//1000000.0;
    private TextView vocalSt;
    private EditText editTextGainFactor;
    private ImageView buttonStart;
    private ImageView silenceImg;
    private Button buttonStop;
    private int intRecordSampleRate = 16000;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack = null;

    private int intMicBufferSize, intStereoBufferSize;
    private short[] micData, stereoData;

    private int intGain = 1;
    private boolean isActive = false;
    private ProgressBar progressBarvocal;
    private ProgressBar silencioBar;
    private boolean isVowelADetected = false; // Variable para el seguimiento de la vocal "a"
    private final int windowSize = 8000; // Tamaño de la ventana (ajusta según tus necesidades)
    private double[] audioWindow = new double[windowSize];
    private int audioWindowIndex = 0;
    private Thread thread;
    private ImageView imagenPuntos;
    private EditText TimeEdit;
    private EditText Silencio;
    private TextView puntos;
    private int puntos1 = 0;
    private String vocalT;
    private ImageView stop;
    private ImageView seguir;

    private int aciertos = 0;
    private int fallos = 0;
    private int intentos = 0;
    private long tiempoTotalEjercicio = 0;
    private long tiempoTotalEjercicioSeconds = 0;
    private long tiempoRespuesta = 0;
    private long tiempoFin = 0;
    private int silencioProgress = 0;
    private long tiempoInactividad = 0;
    private boolean abandono = false;
    private int numeroIntentos = 0;

    private ImageView home;
    private long startTime;
    private final String USERS_COLLECTION = "User";
    private final String EJERCICIOS_COLLECTION="Ejercicios";
    private DocumentReference ejercicioDoc;
    private DocumentReference userDocRef;
    private int level;
    private final String TALK_COLLECTION = "TALK";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private long tiempoInicioDeteccionVocal = 0;
    private CollectionReference talkCollection;
    private DocumentReference talkdurDocument;

    private Points points;
    private int tiempoSilencio = 5, tiempoDuracionVoz = 5, num = 5;

    private boolean is80PercentReached = false; // Agrega esta variable

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duracion);
        puntos = findViewById(R.id.puntos);
        buttonStart = findViewById(R.id.button);
        imagenPuntos = findViewById(R.id.imagenPuntos);
        home = findViewById(R.id.home);
        stop = findViewById(R.id.stop);
        seguir = findViewById(R.id.seguir);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());

        points = (Points) getApplication();
        points.updateMainActivityUI();

        String nombreEjercicio = "Ejercicio_" + 13;
        ejercicioDoc = db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);

        Intent intent = getIntent();
        ArrayList<String> nivel1 = intent.getStringArrayListExtra("Nivel 1");

        if (nivel1 == null){
            Log.d(TAG, "Dato NULL");
        }else {
            tiempoSilencio = Integer.parseInt(nivel1.get(0));
            tiempoDuracionVoz = Integer.parseInt(nivel1.get(1));
            num = Integer.parseInt(nivel1.get(2));
            Log.d(TAG,"tiempoSilencio " + tiempoSilencio);
            Log.d(TAG,"tiempoDuracionVoz " + tiempoDuracionVoz);
            Log.d(TAG,"num " + num);
        }


        silenceImg = findViewById(R.id.silenceImg);
        intRecordSampleRate = 16000; // Ajusta la frecuencia de muestreo según tus necesidades
        intMicBufferSize = AudioRecord.getMinBufferSize(intRecordSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, intRecordSampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, intMicBufferSize, AudioTrack.MODE_STREAM);
        intStereoBufferSize = intMicBufferSize * 2; // Tamaño del búfer para AudioTrack

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, intRecordSampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, intStereoBufferSize, AudioTrack.MODE_STREAM);
        progressBarvocal = findViewById(R.id.progressBarVocal);
        silencioBar = findViewById(R.id.silencioBar);
        getMicPermission();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isActive)
                    return;
                threadLoop();
            }
        });


        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(duracion.this, Habla_Frame.class);
                startActivity(intent);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detenerActividad();

            }
        });

        SharedPreferences preferences = getSharedPreferences("mis_datos_dur", MODE_PRIVATE);
        numeroIntentos = preferences.getInt("intentos", 0);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isActive) {
                    isDetectionEnabled = true;
                    isActive = true;
                    buttonStart.setVisibility(View.GONE);
                    numeroIntentos++;
                    guardarNumeroIntentos();
                    thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            threadLoop();
                        }
                    });


                    thread.start();

                }
            }
        });


    }

    private void getMicPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
            return;
        }
    }

    private void guardarNumeroIntentos() {
        SharedPreferences preferences = getSharedPreferences("mis_datos_dur", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("intentos", numeroIntentos);
        editor.apply();
    }

    private void detenerActividad() {
        isActive = false;

        if (audioRecord != null) {
            audioRecord.stop();
        }
        if (audioTrack != null) {
            audioTrack.stop();
        }


    }

    @SuppressLint("MissingPermission")
    private void threadLoop() {

        isActive = true;
        boolean isVowelADetected = false;
        int windowStep = 512;
        aciertos = 0;
        fallos = 0;
        intentos = 0;
        tiempoTotalEjercicio = 0;
        int windowSize = 1024; // Tamaño de la ventana (ajusta según sea necesario)
        double[] audioWindow = new double[windowSize];
        // Sample Rates 8000, 11025, 16000, 22050, 44100
        int intRecordSampleRate = 16000;

        intMicBufferSize = AudioRecord.getMinBufferSize(intRecordSampleRate, AudioFormat.CHANNEL_IN_MONO
                , AudioFormat.ENCODING_PCM_16BIT);

        micData = new short[intMicBufferSize];

        Log.d(TAG, "MicBufferSize " + String.valueOf(intMicBufferSize));
        Log.d(TAG, "StereoBufferSize " + String.valueOf(intStereoBufferSize));

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , intRecordSampleRate
                , AudioFormat.CHANNEL_IN_MONO
                , AudioFormat.ENCODING_PCM_16BIT
                , intMicBufferSize);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC
                , intRecordSampleRate
                , AudioFormat.CHANNEL_OUT_STEREO
                , AudioFormat.ENCODING_PCM_16BIT
                , intMicBufferSize
                , AudioTrack.MODE_STREAM);


        audioRecord.startRecording();
        audioTrack.play();

        long startTime = System.currentTimeMillis();
        long durationInMilliseconds = getDurationFromEditText();
        int numRepetitions = getNumRepetitionsFromEditText(); // Número de repeticiones
        long silenceDuration = getSilenceDurationFromEditText(); // Duración del silencio en segundos

        for (int repetition = 0; repetition < numRepetitions && isActive; repetition++) {
            //Log.d("AudioThread", "silenceDuration0: " + silenceDuration);
            Log.d("TiempoRepetición", "Iniciando la primera repetición");
            long tiempoRepeticionActual = System.currentTimeMillis() - startTime;
            Log.d("TiempoRepetición", "Tiempo de repetición actual: " + tiempoRepeticionActual);

            tiemposPorRepeticion.add(tiempoRepeticionActual);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    silencioBar.setProgress(0);
                }
            });
            startTime = System.currentTimeMillis();
            while (isActive && System.currentTimeMillis() - startTime < durationInMilliseconds) {
                audioRecord.read(micData, 0, intMicBufferSize);
                for (int i = 0; i < intMicBufferSize; i++) {
                    micData[i] = (short) Math.min(micData[i], Short.MAX_VALUE);

                }


                stereoData = stereoSound(micData, setTone(0.3, 400, intRecordSampleRate), 2 * intMicBufferSize);
                audioTrack.write(stereoData, 0, stereoData.length);
                double[] micDataDouble = new double[micData.length];
                for (int i = 0; i < micData.length; i++) {
                    micDataDouble[i] = (double) micData[i];
                }

                boolean detectedVowel = isVowelA(micDataDouble, energyThreshold);
                if (detectedVowel) {

                    long currentTime = System.currentTimeMillis();

                    long elapsedTime = currentTime - startTime;
                    int progress = (int) (100 * elapsedTime / durationInMilliseconds);
                    if (progress >= 0 && progress <= 100) {
                        progressBarvocal.setProgress(progress);

                        if (progress >= 80 && !is80PercentReached) {
                            puntos1++;
                            aciertos++;

                            is80PercentReached = true;
                            Log.d("Puntos", "" + puntos1);
                            addPoints(puntos1);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    puntos.setText("Puntos: " + puntos1);
                                    imagenPuntos.setImageResource(R.drawable.star2);
                                    imagenPuntos.setVisibility(View.VISIBLE); // Esto hará visible la imagen de los puntos

                                }
                            });
                        }


                    }
                }
            }

            performFFT(micData);
            //Log.d("AudioThread", "silenceDuration0: " + silenceDuration);
            silenceDuration = getSilenceDurationFromEditText();
            startTime = System.currentTimeMillis();
            silencioBar.setMax((int) silenceDuration);
            while (isActive && System.currentTimeMillis() - startTime < silenceDuration) {
                progressBarvocal.setProgress(0);
                is80PercentReached = false;

                audioTrack.write(new short[intMicBufferSize], 0, intMicBufferSize);
                double energy = calculateEnergy(micData);
                short sampleValue = micData[0]; // Escoge un índice para visualizar su valor

                //Log.d("Prueba", "Valor de micData: " + sampleValue);


                //Log.d("Prueba", "Silencio." + energy);
                if (energy < energyThreshold2) {
                    silencioProgress++;
                    int currentProgress = (int) (System.currentTimeMillis() - startTime);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imagenPuntos.setVisibility(View.GONE);
                            silencioBar.setProgress(currentProgress);
                            silenceImg.setImageResource(R.drawable.silenciop);
                            silenceImg.setVisibility(View.VISIBLE); // Esto hará visible la imagen de los puntos

                        }
                    });

                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    silenceImg.setVisibility(View.GONE); // Esto hará visible la imagen de los puntos

                }
            });

        }
        tiempoTotalEjercicio = 0;
        for (Long tiempoRepeticion : tiemposPorRepeticion) {
            tiempoTotalEjercicio += tiempoRepeticion;
        }

// Calcular el tiempo total en segundos
        tiempoTotalEjercicioSeconds = tiempoTotalEjercicio ;

        Log.d("TiempoTotal", "Tiempo total del ejercicio en segundos: " + tiempoTotalEjercicioSeconds);
        sendDataBase(userDocRef,level);
        isActive = false;
        audioRecord.stop();
        audioTrack.stop();



    }

    private void addPoints(int points) {
        String sectionName = "Escucha_Duracion";
        ((Points) getApplication()).addPoints(sectionName, points);
    }

    private DocumentReference getEjercicioDocument(int level) {
        String nombreEjercicio = "Ejercicio_" + 13;
        return db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);
    }
    private void sendDataBase( DocumentReference userdoc,int level){
        DocumentReference ejercicioDoc = getEjercicioDocument(level);

        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("Tiempo de silencio", getSilenceDurationFromEditText());
        datosUsuario.put("Tiempo sostenido", getDurationFromEditText());
        datosUsuario.put("Número de veces", getNumRepetitionsFromEditText());
        datosUsuario.put("Aciertos", aciertos);
        datosUsuario.put("Ejercicio",ejercicioDoc);
        datosUsuario.put("Tiempo total ejercicio", tiempoTotalEjercicio);
        //datosUsuario.put("Tiempo de respuesta", tiempoRespuesta);
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());
        datosUsuario.put("User",userDocRef);
        Date fechaActual = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String fechaHora = dateFormat.format(fechaActual);
        datosUsuario.put("fecha",fechaHora);
        // Agrega el nuevo intento a la colección "Intentos"
        db.collection("Intentos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int numIntentos = queryDocumentSnapshots.size();
                    String intentoNombre = "Intento_" + (numIntentos + 1);

                    db.collection("Intentos")
                            .document(intentoNombre)
                            .set(datosUsuario)
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

    private double calculateEnergy(short[] audioData) {
        double energy = 0.0;
        for (int i = 0; i < audioData.length; i++) {
            energy += Math.pow(audioData[i], 2);
        }
        return energy;
    }

    private void performFFT(short[] audioData) {
        double[] doubleAudioData = new double[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            doubleAudioData[i] = (double) audioData[i];
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(doubleAudioData.length);
        fft.realForward(doubleAudioData);

        boolean isVowelA = isVowelA(doubleAudioData, energyThreshold); // Implementa esta función

        final boolean finalIsVowelA = isVowelA;

    }

    private boolean isVowelA(double[] fftData, double energyThreshold) {

        double[] magnitudes = new double[fftData.length / 2];
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = Math.sqrt(fftData[2 * i] * fftData[2 * i] + fftData[2 * i + 1] * fftData[2 * i + 1]);
        }

        int maxMagnitudeIndex = 0;
        for (int i = 1; i < magnitudes.length; i++) {
            if (magnitudes[i] > magnitudes[maxMagnitudeIndex]) {
                maxMagnitudeIndex = i;
            }
        }

        int sampleRate = 8000;
        double energy = magnitudes[maxMagnitudeIndex] * magnitudes[maxMagnitudeIndex];

        return energy >= energyThreshold;

    }

    private long getSilenceDurationFromEditText() {
        /*
        SharedPreferences preferences = getSharedPreferences("mis_datos", MODE_PRIVATE);
        int tiempoSilencio = preferences.getInt("tiempo_sil", 0);
         */
        String tiempoSilencioF = String.valueOf(tiempoSilencio);
        long silenceInSeconds = 1;

        try {
            silenceInSeconds = Long.parseLong(tiempoSilencioF);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return silenceInSeconds * 1000;
    }

    private int getNumRepetitionsFromEditText() {
        /*
        SharedPreferences preferences = getSharedPreferences("mis_datos", MODE_PRIVATE);
        int num = preferences.getInt("num", 0);
         */
        String numText = String.valueOf(num);
        int numRepetitions = 1;

        try {
            numRepetitions = Integer.parseInt(numText);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return numRepetitions;
    }

    private long getDurationFromEditText() {
        /*
        SharedPreferences preferences = getSharedPreferences("mis_datos", MODE_PRIVATE);
        int tiempoDuracionVoz = preferences.getInt("tiempo_voz", 0);
         */
        String timeText = String.valueOf(tiempoDuracionVoz);

        long durationInSeconds = 5;

        try {
            durationInSeconds = Long.parseLong(timeText);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return durationInSeconds * 1000;
    }

    private short[] setTone(double gain, int freq, int sampleRate) {
        int toneSize = intMicBufferSize;
        short[] tone = new short[toneSize];
        for (int i = 0; i < toneSize; i++) {
            double t = (double) i / sampleRate;
            tone[i] = (short) (gain * Math.sin(2 * Math.PI * freq * t) * Short.MAX_VALUE);
        }
        return tone;
    }

    private short[] stereoSound(short[] left, short[] right, int stereoArraySize) {

        if (left.length != right.length) {
            throw new IllegalArgumentException("Los arrays left y right deben tener la misma longitud.");
        }

        short[] stereoSoundArray = new short[left.length * 2];

        for (int i = 0; i < left.length; i++) {
            stereoSoundArray[2 * i] = left[i];
            stereoSoundArray[2 * i + 1] = right[i];
        }

        return stereoSoundArray;
    }
}
