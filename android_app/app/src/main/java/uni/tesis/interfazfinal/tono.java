package uni.tesis.interfazfinal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import java.util.Locale;
import java.util.Map;

public class tono extends AppCompatActivity {


    private TextView tono;
    private int puntos = 0;

    private static int MIC_PERMISSION_CODE = 200;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack=null;
    private int intMicBufferSize, intStereoBufferSize;
    private short[] micData, stereoData;
    private double[] fftData;
    private long tiempoSilencioPorIntento = 0;
    private long tiempoHablaPorIntento = 0;
   private long tiempoAcierto = 0;
    private String TAG = "TAG";

    private ImageView indicador;
    private boolean isActive = false;
    private DoubleFFT_1D fft;
    private Thread thread;
    private Handler handler;
    private ImageView start;
    private int intRecordSampleRate = 8000;

    private boolean isRecording = false;

    private boolean isGraveTone = false;

    private TextView puntostono;
    private ImageView home;
    private  int aciertos = 0;
    private  int fallos = 0;
    private long tiempoDeRespuesta = 0;
    private  int numIntentos = 0;
    private boolean isVolupVisible = false;
    private final String USERS_COLLECTION = "User";
    private final String EJERCICIOS_COLLECTION="Ejercicios";

    private final String TALK_COLLECTION = "TALK";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private CollectionReference talkCollection;
    private DocumentReference talktonoDocument;
    private int tiempoDuracionVoz = 5, num = 5, tiempoSilencio = 5;


    private double umbral = 1000;

    private boolean shouldShowButton = true;
    private DocumentReference ejercicioDoc;
    private DocumentReference userDocRef;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tono);

        intRecordSampleRate = 8000;
        intMicBufferSize = AudioRecord.getMinBufferSize(intRecordSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());

        String nombreEjercicio = "Ejercicio_" + 14;
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

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, intRecordSampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, intMicBufferSize, AudioTrack.MODE_STREAM);
        intStereoBufferSize = intMicBufferSize * 2;
        home=findViewById(R.id.home);
        start=findViewById(R.id.start);
        puntostono=findViewById(R.id.puntostono);
        indicador=findViewById(R.id.indicador);
        tono=findViewById(R.id.tono);
        fft = new DoubleFFT_1D(intMicBufferSize);
        micData = new short[intMicBufferSize];
        stereoData = new short[2 * intMicBufferSize];
        fftData = new double[intMicBufferSize* 2];
        isGraveTone = false;
        handler = new Handler();
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
                Intent intent = new Intent(tono.this,Habla_Frame.class);
                startActivity(intent);
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isActive = true;
                isRecording = true;
                start.setVisibility(View.GONE);
                isVolupVisible=false;

                thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        threadLoop();
                    }
                });
                thread.start();
            }
        });
    }



    private Runnable inactivityTimer = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                stopRecording();
                shouldShowButton = true; // Detener la grabación si ya está en curso
            }
            start.setVisibility(View.VISIBLE); // Mostrar el botón "start" nuevamente
        }
    };




    private void getMicPermission (){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
            return;
        }
    }
    private void stopRecording() {

        isActive = false;
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }


    }

    @SuppressLint("MissingPermission")
    private void threadLoop() {
        isActive = true;

        isRecording = true;

        int intRecordSampleRate = 8000;




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
        long TiempoSilencioValue = getSilenceDurationFromEditText();
        long TiempoVozValue = getDurationFromEditText();
        int numVValue= getNumRepetitionsFromEditText();
        int rotation;

        long tiempoInicioHabla = 0;
        SharedPreferences preferences = getSharedPreferences("tono", Context.MODE_PRIVATE);
        String selectedItem = preferences.getString("selectedItem", "");
        SharedPreferences preferences2 = getSharedPreferences("MyPrefsLog", MODE_PRIVATE);
        String generoSeleccionado = preferences2.getString("Genero", "ValorPredeterminado");
        for (int repetition = 0; repetition < numVValue && isActive; repetition++) {
            startTime = System.currentTimeMillis();
            tiempoInicioHabla = startTime;
            double sampleRateInHz ;
            double dominantFrequency ;
            double magnitude=0;
            double maxAmplitude = 0;
            int dominantFrequencyIndex = -1;

            while (isActive && System.currentTimeMillis()-startTime<TiempoVozValue) {
                audioRecord.read(micData, 0, intMicBufferSize);
                double[] shortToDoubleData = shortToDouble(micData);
                double[] filteredMicData = applyLowPassFilter(shortToDoubleData, 0.1);
                for (int i = 0; i < intMicBufferSize; i++) {
                    micData[i] = (short) Math.min(micData[i], Short.MAX_VALUE);
                }

                if (selectedItem.trim().equals("Grave")) {
                    stereoData = stereoSound(micData, setTone(0.3, 300, intRecordSampleRate), 2 * intMicBufferSize);
                    audioTrack.write(stereoData, 0, stereoData.length);
                } else if (selectedItem.trim().equals("Agudo")) {
                    stereoData = stereoSound(micData, setTone(0.3, 1000, intRecordSampleRate), 2 * intMicBufferSize);
                    audioTrack.write(stereoData, 0, stereoData.length);
                }

                for (int i = 0; i < intMicBufferSize; i++) {
                    fftData[i] = filteredMicData[i]; // Utiliza el buffer de audio
                }
                fft.realForward(fftData);
                for (int i = 0; i < intMicBufferSize; i++) {
                    magnitude = Math.sqrt(fftData[2 * i] * fftData[2 * i] + fftData[2 * i + 1] * fftData[2 * i + 1]);
                    if (magnitude > maxAmplitude) {
                        maxAmplitude = magnitude;
                        dominantFrequencyIndex = i;
                    }
                }
                sampleRateInHz = intRecordSampleRate;
                dominantFrequency = dominantFrequencyIndex * sampleRateInHz / intMicBufferSize;
                if (generoSeleccionado.equals("Masculino")) {
                    if (dominantFrequency >= 80 && dominantFrequency <= 120) {
                        String toneText="Grave";
                        rotation=360;
                        showImageAndRotate(toneText,rotation);
                        Log.d("Prueba","graveeeeMasc");
                        if (selectedItem.equals("Grave") && System.currentTimeMillis() - startTime >= TiempoVozValue-1) {
                            puntos++;
                            aciertos++;
                            tiempoAcierto = System.currentTimeMillis() - tiempoInicioHabla; // Calcula el tiempo de acierto
                            tiempoDeRespuesta = System.currentTimeMillis() - startTime;
                            numIntentos++;
                            Log.d("Puntos", "Puntos actuales: " + puntos);
                            puntostono.setText("Puntos: " + puntos);

                        }

                    } else if (dominantFrequency >130 ) {
                        String toneText = "Agudo";
                        rotation = 180;
                        showImageAndRotate(toneText, rotation);
                        Log.d("Prueba","agudoooooMasc");

                        if (selectedItem.equals("Agudo") && System.currentTimeMillis() - startTime >= TiempoVozValue-1) {
                            puntos++;
                            aciertos++;
                            tiempoAcierto = System.currentTimeMillis() - tiempoInicioHabla;
                            tiempoDeRespuesta = System.currentTimeMillis() - startTime;
                            numIntentos++;
                            Log.d("Puntos", "Puntos actuales: " + puntos);
                            puntostono.setText("Puntos: " + puntos);

                        }
                    }  else{
                        String toneText="Indefinido";
                        rotation=90;
                        showImageAndRotate(toneText,rotation);
                        if (System.currentTimeMillis() - startTime >= TiempoVozValue - 1) {
                            fallos++;
                            numIntentos++;
                        }

                    }
                }
                if (generoSeleccionado.equals("Femenino")) {
                    if (  dominantFrequency>80&&dominantFrequency < 225) {
                        String toneText="Grave";
                        rotation=360;
                        showImageAndRotate(toneText,rotation);
                        Log.d("Prueba","graveeeeFem");
                        if (selectedItem.equals("Grave") && System.currentTimeMillis() - startTime >= TiempoVozValue-1) {
                            puntos++;
                            aciertos++;
                            tiempoAcierto = System.currentTimeMillis() - tiempoInicioHabla;
                            tiempoDeRespuesta = System.currentTimeMillis() - startTime;
                            numIntentos++;
                            Log.d("Puntos", "Puntos actuales: " + puntos);
                            puntostono.setText("Puntos: " + puntos);

                        }

                    } else if (dominantFrequency > 225 && dominantFrequency <= 300) {
                        String toneText = "Agudo";
                        rotation = 180;
                        showImageAndRotate(toneText, rotation);
                        Log.d("Prueba","agudoooooFem");

                        if (selectedItem.equals("Agudo") && System.currentTimeMillis() - startTime >= TiempoVozValue-1) {
                            puntos++;
                            aciertos++;
                            tiempoAcierto = System.currentTimeMillis() - tiempoInicioHabla;
                            tiempoDeRespuesta = System.currentTimeMillis() - startTime;
                            numIntentos++;
                            Log.d("Puntos", "Puntos actuales: " + puntos);
                            puntostono.setText("Puntos: " + puntos);

                        }
                    }  else{
                        String toneText="Indefinido";
                        rotation=90;
                        showImageAndRotate(toneText,rotation);
                        if (System.currentTimeMillis() - startTime >= TiempoVozValue - 1) {
                            fallos++;
                            numIntentos++;
                        }

                    }
                }


            }
            tiempoHablaPorIntento = System.currentTimeMillis() - tiempoInicioHabla;

            startTime = System.currentTimeMillis();
            while (isActive && System.currentTimeMillis() - startTime < TiempoSilencioValue) {
                audioTrack.write(new short[intMicBufferSize], 0, intMicBufferSize);
                showSilenceImage();
                String toneText="Indefinido";
                rotation=90;
                showImageAndRotate(toneText,rotation);

            }
            hideSilenceImage();
            tiempoSilencioPorIntento = System.currentTimeMillis() - startTime;


        }
        String toneText="Indefinido";
        rotation=90;
        showImageAndRotate(toneText,rotation);
        isActive = false;
        audioRecord.stop();
        audioTrack.stop();

        sendDataBase(talktonoDocument);


    }

    private void sendDataBase(DocumentReference doc){

        SharedPreferences preferences = getSharedPreferences("tono", Context.MODE_PRIVATE);
        String selectedItem = preferences.getString("selectedItem", "");
        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("aciertos", aciertos);
        datosUsuario.put("fallos", fallos);
        datosUsuario.put("Tiempo de silencio ", tiempoSilencioPorIntento);
        datosUsuario.put("Tono",selectedItem);
        datosUsuario.put("Ejercio",ejercicioDoc);
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
    double[] shortToDouble(short[] input) {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (double) input[i];
        }
        return output;
    }
    private double[] applyLowPassFilter(double[] inputData, double alpha) {
        double[] outputData = new double[inputData.length];

        outputData[0] = inputData[0];
        for (int i = 1; i < inputData.length; i++) {
            outputData[i] = alpha * inputData[i] + (1 - alpha) * outputData[i - 1];
        }

        return outputData;
    }
    private void showImageAndRotate(final String toneText, final int rotation) {
        updateTonoText(toneText);
        indicador.setPivotX(indicador.getWidth() / 2);
        indicador.setPivotY(indicador.getHeight() / 2);
        indicador.setRotation(rotation);

        SharedPreferences preferences = getSharedPreferences("tono", Context.MODE_PRIVATE);
        String selectedItem = preferences.getString("selectedItem", "");

        if (selectedItem.trim().equals(toneText)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImageView starImageView = findViewById(R.id.star);
                    starImageView.setImageResource(R.drawable.star2);
                    starImageView.setVisibility(View.VISIBLE);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            starImageView.setVisibility(View.GONE);
                        }
                    }, 1000);
                }
            });
        }
    }
    private void updateTonoText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tono.setText(text);
            }
        });
    }


    private void showSilenceImage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView silenceImageView = findViewById(R.id.silenciop);
                silenceImageView.setImageResource(R.drawable.silenciop);
                silenceImageView.setVisibility(View.VISIBLE);
            }
        });
    }
    private void hideSilenceImage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView silenceImageView = findViewById(R.id.silenciop);
                silenceImageView.setVisibility(View.GONE);
            }
        });
    }

    private short[] setTone(double gain, int freq, int sampleRate)
    {
        //int toneSize = (int)(sampleRate * durationTimeMs * 0.001);
        int toneSize = intMicBufferSize;
        short[] tone = new short[toneSize];
        for (int i = 0; i < toneSize; i++) {
            double t = (double) i / sampleRate;
            tone[i] = (short) (gain * Math.sin(2 * Math.PI * freq * t) * Short.MAX_VALUE);
        }
        return tone;
    }

    private short[] stereoSound(short[] left, short[] right, int stereoArraySize){

        short[] stereoSoundArray = new short[stereoArraySize];

        for (int i = 0; i<stereoArraySize; i++){
            stereoSoundArray[i]=0;
        }
        // LEFT
        for (int i = 0; i<left.length; i++){
            stereoSoundArray[2*i] = left[i];
        }
        // RIGHT
        for (int i = 0; i<left.length; i++){
            stereoSoundArray[2*i+1] = right[i];
        }

        return stereoSoundArray;
    }
    private long getSilenceDurationFromEditText() {
        /*
        SharedPreferences preferences = getSharedPreferences("tono", MODE_PRIVATE);
        int tiempoSilencio = preferences.getInt("tsilencioValue", 0);
         */
        return tiempoSilencio*1000 ;
    }
    private int getNumRepetitionsFromEditText() {
        /*
        SharedPreferences preferences = getSharedPreferences("tono", MODE_PRIVATE);
        int num = preferences.getInt("vecesValue", 0);
         */
        return num;
    }
    private long getDurationFromEditText() {
        /*
        SharedPreferences preferences = getSharedPreferences("tono", MODE_PRIVATE);
        int tiempoDuracionVoz = preferences.getInt("sostenidoValue", 0);
         */
        return tiempoDuracionVoz * 1000;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;

        }
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;

        }
    }
}
