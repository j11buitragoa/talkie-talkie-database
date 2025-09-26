package uni.tesis.interfazfinal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class duracion_staccato extends AppCompatActivity {
    private TextView resultTextView, silenceCountTextView, message, puntos;
    private ImageView pato;
    private int contadorRepeticiones = 0;
    private LinearLayout patoContainer;
    private boolean conteoEnProgreso = false;
    private List<ImageView> listaPatitos = new ArrayList<>();
    private long startTimeSpeaking;
    private long startTimeSpeaking2 = 0;

    private long lastSpeechEndTime;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private int silenceCount = 0;
    private int totalsilence=0;
    private int puntosS = 0;
    private ProgressBar progressBar;
    private ImageView silence;
    private long lastVoiceActivityTime = 0;
    private int maxSilence;
    private int intMicBufferSize, intStereoBufferSize;
    private short[] micData, stereoData;
    private CountDownTimer silenceCountdownTimer;
    private static final double RMS_THRESHOLD = 0.5; // Umbral ajustable
    private boolean isSilent = true;
    short[] zeroVector ;
    private boolean isAudioThreadRunning = false;

    private boolean isActive = false;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread thread;
    private Thread audioThread;
    private DocumentReference userDocRef;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private int repeticionestotal=0;
    private FirebaseUser user;
    private final String USERS_COLLECTION = "User";
    private DocumentReference ejercicioDoc;
    private ArrayList<Long> elapsedTimes = new ArrayList<>();
    long tiempoRespuesta=0;
    private int level;
    private Points points;
    private  final String EJERCICIOS_COLLECTION="Ejercicios";
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duracion_staccato);
        resultTextView = findViewById(R.id.resultTextView);
        Button startSpeechBtn = findViewById(R.id.startSpeechBtn);
        patoContainer = findViewById(R.id.patoContainer);
        silence=findViewById(R.id.silence);
        listaPatitos = new ArrayList<>();
        silenceCountTextView = findViewById(R.id.silenceCountTextView);
        silenceCountdownTimer = createSilenceCountdownTimer();
        progressBar = findViewById(R.id.progressBar);
        message = findViewById(R.id.message);
        puntos = findViewById(R.id.puntos);


        SharedPreferences preferences = getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        String fonema = preferences.getString("fonema", "LA");
        String repeticiones = preferences.getString("repeticiones", "5");
        String silencio = preferences.getString("silencio", "1");
        String nombreGrabacion = preferences.getString("nombreGrabacion", "");

        Log.d("Envio", "veces: " + repeticiones);
        Log.d("Envio", "fonema: " + fonema);
        Log.d("Envio", "silencio: " + silencio);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());
        String nombreEjercicio = "Ejercicio_" + 16;
        ejercicioDoc = db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);
        Intent intent = getIntent();
        ArrayList<String> nivel1 = intent.getStringArrayListExtra("Nivel 1");
        Log.d(TAG, "user " + user.getDisplayName() + "\nID " + user.getUid());
        points = (Points) getApplication();
        points.updateMainActivityUI();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                int colorFonema = ContextCompat.getColor(getApplicationContext(), R.color.purple);
                String mensaje = "Pronuncia: " + fonema + " corto una vez";
                Spannable spannable = new SpannableString(mensaje);
                spannable.setSpan(new ForegroundColorSpan(colorFonema), mensaje.indexOf(fonema), mensaje.indexOf(fonema) + fonema.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                message.setText(spannable);
                message.setVisibility(View.VISIBLE);

                ConstraintLayout.LayoutParams layoutParams6 = (ConstraintLayout.LayoutParams) message.getLayoutParams();
                layoutParams6.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams6.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams6.leftMargin = 10;  // Ajusta según tus necesidades
                layoutParams6.topMargin = 100;  // Ajusta según tus necesidades
                message.setLayoutParams(layoutParams6);


                // Establece las posiciones y dimensiones para text1
                ConstraintLayout.LayoutParams layoutParams1 = (ConstraintLayout.LayoutParams) puntos.getLayoutParams();
                layoutParams1.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams1.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams1.leftMargin = 50;  // Ajusta según tus necesidades
                layoutParams1.topMargin = 30;  // Ajusta según tus necesidades
                puntos.setLayoutParams(layoutParams1);

                // Establece las posiciones y dimensiones para text2
                ConstraintLayout.LayoutParams layoutParams3 = (ConstraintLayout.LayoutParams) resultTextView.getLayoutParams();
                layoutParams3.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams3.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams3.leftMargin = 80;  // Ajusta según tus necesidades
                layoutParams3.topMargin = 450;  // Ajusta según tus necesidades
                resultTextView.setLayoutParams(layoutParams3);


                //**
                // Obtén las referencias a las restricciones del TextView
                ConstraintLayout.LayoutParams layoutParams2 = (ConstraintLayout.LayoutParams) silenceCountTextView.getLayoutParams();

                // Establece las nuevas restricciones de posición (ajusta los valores según tus necesidades)
                layoutParams2.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID; // o establece la restricción izquierda con respecto a otra vista
                layoutParams2.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;   // o establece la restricción superior con respecto a otra vista
                layoutParams2.leftMargin = 80;  // Coordenada X
                layoutParams2.topMargin = 520;   // Coordenada Y

                // Aplica los nuevos parámetros de diseño al TextView
                silenceCountTextView.setLayoutParams(layoutParams2);

                // Configura las restricciones para posicionar la ProgressBar (ajusta los valores según tus necesidades)
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) progressBar.getLayoutParams();

                // Establece las restricciones de posición (X, Y)
                layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID; // Puedes ajustar estas restricciones según tus necesidades
                layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams.leftMargin = 1; // Coordenada X
                layoutParams.topMargin = 580; // Coordenada Y
                // Aplica los parámetros de diseño a la ProgressBar
                progressBar.setLayoutParams(layoutParams);
                maxSilence = Integer.parseInt(silencio);
                progressBar.setMax(maxSilence);
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(duracion_staccato.this);
                speechRecognizer.setRecognitionListener(new MyRecognitionListener());

            }
        });

        audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isActive)
                    return;
                threadLoop();
            }
        });


        startSpeechBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isListening) {
                    startSpeechBtn.setVisibility(View.INVISIBLE);
                    tiempoRespuesta=0;
                    isActive = true;
                    startTimeSpeaking2 = System.currentTimeMillis();
                    //audioThread.start();
                    startSpeechRecognitionOnUiThread();

                } else {
                    stopSpeechRecognition();
                }
            }
        });
    }
    private void startSpeechRecognitionOnUiThread() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                startSpeechRecognition();
            }
        });
    }
    private DocumentReference getEjercicioDocument(int level) {
        String nombreEjercicio = "Ejercicio_" + 16;
        return db.collection(EJERCICIOS_COLLECTION).document(nombreEjercicio);
    }

    private void sendDataBase(DocumentReference userdoc,int level){
        DocumentReference ejercicioDoc = getEjercicioDocument(level);
        Map<String, Object> mapa = new HashMap<>();
        SharedPreferences preferences = getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        String fonema = preferences.getString("fonema", "LA");
        String repeticiones = preferences.getString("repeticiones", "5");
        String silencio = preferences.getString("silencio", "1");


        int rep=Integer.parseInt(repeticiones);
        int sil=Integer.parseInt(silencio);
        int vez=rep*2;
        // String tmp1, tmp2;
        mapa.put("fonema", fonema);
        mapa.put("puntos Posibles", vez);
        mapa.put("aciertos", puntosS);
        mapa.put("duración de silencio",sil);
        mapa.put("tiempo de Respuesta:" , elapsedTimes);
        mapa.put("ejercicio",ejercicioDoc);

        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(user.getEmail());
        mapa.put("usuario",userDocRef);
        Date fechaActual = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String fechaHora = dateFormat.format(fechaActual);
        mapa.put("fecha",fechaHora);
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
    private void threadLoop() {
        // Configurar AudioRecord para la adquisición de voz
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        int intRecordSampleRate = 8000;
        intMicBufferSize = AudioRecord.getMinBufferSize(intRecordSampleRate, AudioFormat.CHANNEL_IN_MONO
                , AudioFormat.ENCODING_PCM_16BIT);
        micData = new short[intMicBufferSize];
        zeroVector = new short[intMicBufferSize];
        Arrays.fill(zeroVector, (short) 0);

        Log.d(TAG, "Entro al threadloop");
        Log.d("AudioBuffer", "Tamaño del búfer de audio: " + intMicBufferSize);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                intRecordSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                intMicBufferSize);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC
                , intRecordSampleRate
                , AudioFormat.CHANNEL_OUT_STEREO
                , AudioFormat.ENCODING_PCM_16BIT
                , intMicBufferSize
                , AudioTrack.MODE_STREAM);

        audioRecord.startRecording();
        audioTrack.play();

        while (isActive) {
            // Log.d(TAG, "Entro al while is active");
            audioRecord.read(micData, 0, intMicBufferSize);
            for (int i = 0; i < intMicBufferSize; i++) {
                micData[i] = (short) Math.min(micData[i] , Short.MAX_VALUE);
            }
            // Crear señal estéreo solo con el canal izquierdo
            stereoData = stereoSound(micData, zeroVector, 2 * intMicBufferSize);
            audioTrack.write(stereoData, 0, stereoData.length);
        }
        // Detener grabación y reproducción al salir del bucle
        audioRecord.stop();
        audioTrack.stop();
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
    private void startSpeechRecognition() {
        isAudioThreadRunning = true;

        isListening = true;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es"); // Cambia a tu idioma preferido


        speechRecognizer.startListening(intent);

    }

    private void stopSpeechRecognition() {
        isListening = false;
        //resultTextView.setText("Detenido.");
        speechRecognizer.stopListening();
    }
    private class MyRecognitionListener implements RecognitionListener {
        SharedPreferences preferences=getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d("SpeechRecognizer", "Ready for speech");
            startTimeSpeaking = System.currentTimeMillis();


        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("SpeechRecognizer", "Beginning of speech");
            // Calcula el tiempo transcurrido desde el inicio hasta aquí
            //long elapsedTime = System.currentTimeMillis() - startTimeSpeaking;
            //Log.d("SpeechRecognizer", "Tiempo hasta comenzar a hablar: " + elapsedTime + " ms");
            lastSpeechEndTime = System.currentTimeMillis();

            // elapsedTimes.add(elapsedTime);
            message.setVisibility(View.GONE);

            isSilent = false;
        }

        @Override
        public void onRmsChanged(float v) {
            // Verifica si el nivel de sonido es mayor que el umbral establecido
            if (v > RMS_THRESHOLD) {
                lastSpeechEndTime = System.currentTimeMillis(); // Actualiza el tiempo de la última actividad de voz
                // La persona está hablando, pero solo incrementa la barra si isSilent es falso
                if (!conteoEnProgreso) {
                    isSilent = false;
                }
            } else {
                // La persona está en silencio
                isSilent = true;
            }
        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {
            lastSpeechEndTime = System.currentTimeMillis();

        }

        @Override
        public void onError(int error) {
            //resultTextView.setText("Error en el reconocimiento de voz. Código: " + error);
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    startSpeechRecognition();
                }
            });

        }

        @Override
        public void onResults(Bundle results) {
            String repeticiones=preferences.getString("repeticiones","5");
            Integer repeticionest=Integer.parseInt(repeticiones);
            Integer repeticionestt=repeticionest*2;
            if (puntosS < repeticionestt && contadorRepeticiones <= repeticionestt) {
                updateResultView(results);

                Log.d(TAG, "Puntos onresults" + puntosS);

                // Reinicia la escucha para la adquisición continua
                if (isListening) {
                    startSpeechRecognition();

                }

            }



        }

        @Override
        public void onPartialResults(Bundle bundle) {

            updateResultView(bundle);
            Log.d(TAG, "Puntos onpartial" + puntosS);

        }


        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    }
    private void updateResultView(Bundle results) {
        SharedPreferences preferences=getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        String fonema=preferences.getString("fonema","LA");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            Log.d(TAG, "Puntos update1" + puntosS);
            String repeticiones=preferences.getString("repeticiones","5");
            Integer repeticionest=Integer.parseInt(repeticiones);
            Integer repeticionestt=repeticionest*2;
            String result = matches.get(0);
            if (result.equalsIgnoreCase(fonema)&& puntosS<=repeticionestt ) {
                contadorRepeticiones++;
                repeticionestotal++;
                Log.d(TAG, "Puntos update2" + puntosS);
                //resultTextView.setText("Texto reconocido: " + result);
                long tiempoActual = System.currentTimeMillis();

                tiempoRespuesta = tiempoActual - startTimeSpeaking2;
                Log.d(TAG, "Tiempo respuesta" + tiempoRespuesta);
                elapsedTimes.add(tiempoRespuesta);

                tiempoRespuesta =0;
                puntosS++;
                puntos.setText("Puntos: " + Integer.toString(puntosS));

                startTimeSpeaking2 = tiempoActual;

                Log.d(TAG, "Puntos update" + puntosS);
                resultTextView.setVisibility(View.GONE);
                message.setVisibility(View.GONE);
                mostrarImagenesSegunRepeticiones();


            } else {
                resultTextView.setText("No coincidió con el fonema.");
                resultTextView.setVisibility(View.VISIBLE);


            }

        }
    }
    private void mostrarImagenesSegunRepeticiones() {
        SharedPreferences preferences = getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        String repeticiones = preferences.getString("repeticiones", "5");
        String silencio = preferences.getString("silencio", "1");
        Integer repeticionest = Integer.parseInt(repeticiones);
        Integer repeticionestt = repeticionest * 2;
        ConstraintLayout.LayoutParams containerParams = (ConstraintLayout.LayoutParams) patoContainer.getLayoutParams();
        // Cambiar la posición en el eje X (horizontal)
        containerParams.leftMargin = 50;  // Ajusta el margen izquierdo según tus necesidades

        // Cambiar la posición en el eje Y (vertical)
        containerParams.topMargin = 1000;   // Ajusta el margen superior según tus necesidades

        // Aplicar los nuevos parámetros de diseño al contenedor
        patoContainer.setLayoutParams(containerParams);


        Log.d("mostrarImagenes", "Mostrando imagen para repeticiones: " + contadorRepeticiones);
        // Obtén el ID del recurso de la imagen dinámicamente
        int resourceId = getResources().getIdentifier("pato2", "drawable", getPackageName());
        // Verifica si aún puedes mostrar más patos
        if (contadorRepeticiones <=Integer.parseInt(repeticiones)) {
            Log.d("mostrarImagenes", "Mostrando imagen para repeticiones2: " + contadorRepeticiones);
            // Crea un nuevo ImageView para cada pato
            ImageView nuevoPato = new ImageView(this);
            // Configura los parámetros de diseño con márgenes y dimensiones proporcionales a la pantalla
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            int patoWidth = screenWidth / 5; // Ajusta según sea necesario
            int patoHeight = screenHeight / 5; // Ajusta según sea necesario

            // Configura los parámetros de diseño con márgenes (ajusta estos valores según tus necesidades)
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    patoWidth, // Ancho
                    patoHeight  // Altura
            );

            // Ajusta los márgenes para lograr la disposición en forma de escalera ascendente
            //int leftMargin = 50 + (contadorRepeticiones - 1) * 30;  // Ajusta el valor según tus necesidades
            int rightMargin = 100 - (contadorRepeticiones - 1) * 45;  // Ajusta el valor según tus necesidades
            int topMargin = 0 + (contadorRepeticiones - 1) * 1;   // Ajusta el valor según tus necesidades

            layoutParams.setMargins(rightMargin, topMargin, 150, 10);
            nuevoPato.setLayoutParams(layoutParams);

            // Establece la imagen en el ImageView
            nuevoPato.setImageResource(resourceId);

            // Agrega el nuevo pato al contenedor
            patoContainer.addView(nuevoPato);


            resetConteoYBarra();


            // Iniciar el temporizador de silencio solo si el contadorRepeticiones es menor o igual al número de silencio deseado
            if (contadorRepeticiones <= Integer.parseInt(silencio) && !conteoEnProgreso) {
                startSilenceCountdown();
                conteoEnProgreso = true;
            }
            Log.d(TAG, "total" + repeticionestotal);
            Log.d(TAG, "totals" + totalsilence);


        }

        else {
            stopSpeechRecognition();

        }


    }
    private void startSilenceCountdown() {
        silenceCount = 0;
        silenceCountdownTimer.start();
    }

    private void resetConteoYBarra() {
        progressBar.setProgress(0);
        silenceCountdownTimer.cancel();
        silenceCount = 0;
        updateSilenceCountTextView(silenceCount);
        conteoEnProgreso = false;

    }

    private void ejecutarMostrarResultadoConDelay(long delayMillis) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mostrarResultado();
            }
        }, delayMillis);
    }
    private void mostrarResultado() {
        Log.d(TAG, "Bloque else ejecutado mostrar result ");

        SharedPreferences preferences=getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        String repeticiones=preferences.getString("repeticiones","5");
        Integer repeticionest=Integer.parseInt(repeticiones);
        Integer repeticionestt=repeticionest*2;
        Log.d("Repeticiones","Rep"+ repeticionestt+repeticionest+repeticiones);
        // Inflar el layout personalizado
        View resultadoView = getLayoutInflater().inflate(R.layout.layout_result, null);

        // Obtener el TextView del layout
        TextView textResultado = resultadoView.findViewById(R.id.puntostt);
        TextView textPosibles = resultadoView.findViewById(R.id.puntospos);
        ImageView imageResultado = resultadoView.findViewById(R.id.imageView);
        TextView textMessage = resultadoView.findViewById(R.id.textMessage);
        LinearLayout.LayoutParams layoutParamsTextResultado = (LinearLayout.LayoutParams) textResultado.getLayoutParams();
        layoutParamsTextResultado.leftMargin = 20;  // Ajusta la coordenada X
        layoutParamsTextResultado.topMargin = 15;  // Ajusta la coordenada Y
        textResultado.setLayoutParams(layoutParamsTextResultado);

        LinearLayout.LayoutParams layoutParamsTextResultado2 = (LinearLayout.LayoutParams) textPosibles.getLayoutParams();
        layoutParamsTextResultado2.leftMargin = 20;  // Ajusta la coordenada X
        layoutParamsTextResultado2.topMargin = 1;  // Ajusta la coordenada Y
        textPosibles.setLayoutParams(layoutParamsTextResultado2);

        LinearLayout.LayoutParams layoutParamsTextResultado3 = (LinearLayout.LayoutParams) textMessage.getLayoutParams();
        layoutParamsTextResultado3.leftMargin = 50;  // Ajusta la coordenada X
        layoutParamsTextResultado3.topMargin = 0;  // Ajusta la coordenada Y
        textMessage.setLayoutParams(layoutParamsTextResultado3);

        LinearLayout.LayoutParams layoutParamsImage = (LinearLayout.LayoutParams) imageResultado.getLayoutParams();
        layoutParamsImage.leftMargin = 50;  // Ajusta la coordenada X
        layoutParamsImage.topMargin = 1;  // Ajusta la coordenada Y
        // Establecer el número de puntos en el TextView (ajusta esto según tu lógica)
        textResultado.setText("Puntos Obtenidos: " + puntosS);
        textPosibles.setText("Puntos Posibles: " +repeticionestt);
        // Lógica para determinar la imagen según la cantidad de puntos
        if (puntosS == repeticionestt) {
            // Si obtuvo la mayor cantidad de puntos posibles
            imageResultado.setImageResource(R.drawable.star5);
            textMessage.setText("Muy bien completaste el ejercicio correctamente");
            layoutParamsImage.width = 400;  // Ajusta el ancho según tus necesidades
            layoutParamsImage.height = 200;
            imageResultado.setLayoutParams(layoutParamsImage);

        } else if (puntosS >= repeticionestt / 2) {
            // Si obtuvo al menos la mitad de los puntos posibles
            imageResultado.setImageResource(R.drawable.star3);
            textMessage.setText("Por poco  \n ¡Intentemoslo de nuevo y consigamos\n 5 estrellas!");
            layoutParamsImage.width = 500;  // Ajusta el ancho según tus necesidades
            layoutParamsImage.height = 300;
            imageResultado.setLayoutParams(layoutParamsImage);
        } else {
            // Si obtuvo menos de la mitad de los puntos posibles
            imageResultado.setImageResource(R.drawable.star1);
            textMessage.setText("Podemos hacerlo mejor \n ¡Intentemoslo de nuevo!");
            layoutParamsImage.width = 600;  // Ajusta el ancho según tus necesidades
            layoutParamsImage.height = 500;
            imageResultado.setLayoutParams(layoutParamsImage);
        }


        // Mostrar el layout resultado en un AlertDialog o en cualquier otro contenedor deseado
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(resultadoView);
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(duracion_staccato.this, Habla_Frame.class);
                startActivity(intent);
                finish();
            }
        }); // Puedes agregar botones adicionales o acciones según sea necesario
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void mostrarImagenEspecial() {
        Log.d(TAG, "Bloque else ejecutado trofeo");

        // Aquí puedes mostrar la imagen especial, por ejemplo, cambiar la imagen de un ImageView
        ImageView imageView = findViewById(R.id.trofeo);
        // Establecer las dimensiones deseadas (ajusta los valores según tus necesidades)
        int widthInPixels = 400;  // Ancho en píxeles
        int heightInPixels = 400; // Altura en píxeles



        ConstraintLayout.LayoutParams layoutParams11 = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams11.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams11.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams11.leftMargin = 100;  // Ajusta según tus necesidades
        layoutParams11.topMargin = 400;  // Ajusta según tus necesidades
        layoutParams11.width = widthInPixels;
        layoutParams11.height = heightInPixels;
        // Establecer los parámetros de diseño en el ImageView
        imageView.setLayoutParams(layoutParams11);
        imageView.setImageResource(R.drawable.trofeo);
        addPoints(puntosS);
        imageView.setVisibility(View.VISIBLE);;

        // También puedes realizar otras acciones necesarias después de la última repetición
    }
    private void addPoints(int points) {
        String sectionName = "Habla_Staccato";
        ((Points) getApplication()).addPoints(sectionName, points);
    }
    private CountDownTimer createSilenceCountdownTimer() {
        SharedPreferences preferences=getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        String silencio=preferences.getString("silencio","1");
        String repeticiones=preferences.getString("repeticiones","5");
        String fonema=preferences.getString("fonema","LA");

        return new CountDownTimer(Integer.parseInt(silencio) * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                silenceCount++;
                repeticionestotal++;
                updateSilenceCountTextView(silenceCount);
                // Obtén los parámetros de diseño actuales del ConstraintLayout
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) silence.getLayoutParams();

                // Establece las nuevas dimensiones (ancho y alto) en píxeles (ajusta según tus necesidades)
                layoutParams.width = 200;  // Ancho
                layoutParams.height = 200; // Alto

                // Establece las nuevas restricciones de posición (izquierda y arriba)
                layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID; // o establece la restricción izquierda con respecto a otra vista
                layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;   // o establece la restricción superior con respecto a otra vista

                // Establece las nuevas coordenadas (márgenes izquierdo y superior) en píxeles (ajusta según tus necesidades)
                layoutParams.leftMargin = 200;  // Márgen izquierdo
                layoutParams.topMargin = 320;   // Márgen superior

                // Aplica los nuevos parámetros de diseño al ConstraintLayout
                silence.setLayoutParams(layoutParams);
                silence.setVisibility(View.VISIBLE);
                // Verifica si la persona está en silencio
                if (isSilent) {
                    progressBar.incrementProgressBy(1);  // Incrementa la barra de progreso en 1
                    // Actualiza la barra de progreso
                    progressBar.setProgress(silenceCount);
                    message.setVisibility(View.GONE);
                    if (silenceCount == Integer.parseInt(silencio)) {
                        puntosS++;
                        puntos.setText("Puntos: "+ Integer.toString(puntosS));
                        Log.d(TAG, "Puntos CountD"+puntosS);

                    }
                }else{
                    message.setVisibility(View.VISIBLE);
                    message.setText("¡Quédate en silencio para que la barra aumente!");

                }



            }

            @Override
            public void onFinish() {
                // Se ha alcanzado el valor de silencio, reiniciar el temporizador y mostrar nuevas imágenes
                silenceCount = 0;
                silence.setVisibility(View.GONE);
                totalsilence++;
                Log.d(TAG,"totalsilence"+totalsilence);
                conteoEnProgreso = false;
                progressBar.setProgress(0);
                int colorFonema = ContextCompat.getColor(getApplicationContext(), R.color.purple);
                String mensaje = "Pronuncia: " + fonema + " corto una vez";
                Spannable spannable = new SpannableString(mensaje);
                spannable.setSpan(new ForegroundColorSpan(colorFonema), mensaje.indexOf(fonema), mensaje.indexOf(fonema) + fonema.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                message.setText(spannable);
                message.setVisibility(View.VISIBLE);
                if(contadorRepeticiones == Integer.parseInt(repeticiones)&&totalsilence==Integer.parseInt(repeticiones)) {
                    Log.d(TAG, "totals2" + totalsilence);

                    Log.d(TAG, "Bloque else ejecutado");

                    stopSpeechRecognition();
                    mostrarImagenEspecial();
                    ejecutarMostrarResultadoConDelay(3000);
                    sendDataBase(userDocRef, level);
                }

            }
        };
    }
    // Obtén los resultados del reconocimiento de voz

    private void updateSilenceCountTextView(int count) {
        String textoCompleto = "Tiempo en silencio: " + count + "  segundos";
        SpannableString spannableString = new SpannableString(textoCompleto);
        int inicioCount = textoCompleto.indexOf(String.valueOf(count));
        int finCount = inicioCount + String.valueOf(count).length();
        spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.yellow)), inicioCount, finCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        silenceCountTextView.setText(spannableString);


    }
    private void stopVoiceAcquisitionPlayback() {
        isActive = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.setRecognitionListener(null);
        speechRecognizer.destroy();
        stopVoiceAcquisitionPlayback();

    }
    @Override
    protected void onStop() {
        super.onStop();
        isActive = false; // Detener el hilo de audio en onStop

    }
}