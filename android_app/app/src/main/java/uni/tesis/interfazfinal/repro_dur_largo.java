package uni.tesis.interfazfinal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class repro_dur_largo extends AppCompatActivity {
    private Button comenzar,reproducir,intentalo,admin,volver;
    private TextView result,silencio,pronunciar,tvResult,timetext;
    private long elapsedTimeSinceVocalDetection;

    private AudioTrack audioTrack;
    private StringBuilder currentVowel = new StringBuilder();
    private HashMap<String, Long> vocalDurations = new HashMap<>();
    private static final float RMS_THRESHOLD = 8f;
    private long startTimeSpeaking;
    private long startTimeVocalDetection = 0;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private long startTimeSelectedVocal;
    private boolean isVowelMatch = false;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private Map<String, Long> startTimeVowelMap = new HashMap<>();
    private boolean isSpeaking = false;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean isActive = false;
    private long tiempoInicio;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repro_dur_largo);
        tiempoInicio = System.currentTimeMillis();

        SharedPreferences preferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        String selectedVocal = preferences.getString("selectedVocal", "a");
        String silencio1 = preferences.getString("silc_time", "5");
        String duracion1 = preferences.getString("dura_time", "2");
        String nombreGrabacion = preferences.getString("nombreGrabacion", "");



        result = findViewById(R.id.result);
        silencio = findViewById(R.id.silencio);
        pronunciar = findViewById(R.id.pronunciar);
        comenzar = findViewById(R.id.comenzar);
        reproducir = findViewById(R.id.reproducir);
        intentalo = findViewById(R.id.intentalo);
        tvResult = findViewById(R.id.tvResult);
        timetext = findViewById(R.id.timetext);
        admin=findViewById(R.id.admin);
        volver=findViewById(R.id.volver);


        inicializarReconocedorVoz();
        // Verificar si los valores no están configurados y asignar valores predeterminados
        if (selectedVocal.isEmpty()) {
            selectedVocal = "a"; // Valor predeterminado para la vocal
        }

        if (silencio1.isEmpty()) {
            silencio1 = "5"; // Valor predeterminado para el tiempo de silencio
        }

        if (duracion1.isEmpty()) {
            duracion1 = "2"; // Valor predeterminado para la duración
        }
        switch (selectedVocal.toLowerCase()) {
            case "a":
                String textoV = "Debes pronunciar : " + " La "+ "\nDurante:  "+ duracion1+" segundos";
                pronunciar.setText(textoV);
                break;
            case "e":
                String textoVo = "Debes pronunciar : " + " Me "+ "\nDurante: "+ duracion1+" segundos";
                pronunciar.setText(textoVo);
                break;
            case "i":
                String textoVoc = "Debes pronunciar : " + " Mi "+ "\nDurante: "+ duracion1+" segundos";
                pronunciar.setText(textoVoc);
                break;
            case "o":
                String textoVoca = "Debes pronunciar : " + " No "+ "\nDurante: "+ duracion1+" segundos";
                pronunciar.setText(textoVoca);
                break;
            case "u":
                String textoVocal = "Debes pronunciar : " + " Su "+ "Durante: "+ duracion1+" segundos";
                pronunciar.setText(textoVocal);
                break;
        }

        String textosilencio = "Debes mantenerte en silencio:  " + silencio1+" segundos entre \n cada pronunciación";
        silencio.setText(textosilencio);

        admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(repro_dur_largo.this, confi_dur_largo.class);
                startActivity(intent);
                finish();
            }
        });
        comenzar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                final String nombreGrabacion = preferences.getString("nombreGrabacion", "");
                String selectedVocal = preferences.getString("selectedVocal", "a");
                String silencio1 = preferences.getString("silc_time", "5");
                String duracion1 = preferences.getString("dura_time", "2");

                // Verificar si los valores son los predeterminados y actualizar si es necesario
                if ( selectedVocal.isEmpty() | silencio1.isEmpty() || duracion1.isEmpty()) {
                    // Asignar valores predeterminados
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("selectedVocal", "a");
                    editor.putString("silc_time", "5");
                    editor.putString("dura_time", "2");
                    editor.apply();

                }
                Intent intent = new Intent(repro_dur_largo.this, dura_largo.class);
                startActivity(intent);
                finish();
            }
        });
        reproducir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reproducirGrabacion(nombreGrabacion);
            }
        });
        intentalo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVoiceRecognition();
            }
        });
        volver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(repro_dur_largo.this, Habla_Frame.class);
                startActivity(intent);
            }
        });
    }
    private void reproducirGrabacion(String nombreGrabacion) {
        final int bufferSize = AudioTrack.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);


        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        try {
            // Simplificamos la construcción de la ruta del archivo
            File file = new File(nombreGrabacion);

            if (!file.exists()) {
                Log.e(TAG, "El archivo no existe: " + file.getAbsolutePath());
                return;
            }

            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            audioTrack.play();

            byte[] buffer = new byte[bufferSize];
            short[] stereoBuffer = new short[bufferSize / 2]; // Convertimos a shorts
            int bytesRead;

            while ((bytesRead = dataInputStream.read(buffer)) > 0) {
                ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(stereoBuffer);
                short[] modifiedBuffer = modifyBufferForStereo(stereoBuffer, bytesRead / 2);
                audioTrack.write(modifiedBuffer, 0, modifiedBuffer.length);
            }

            dataInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private short[] modifyBufferForStereo(short[] buffer, int bytesRead) {
        // Nuevo tamaño del búfer teniendo en cuenta dos canales (izquierdo y derecho)
        int modifiedBufferSize = bytesRead * 2;
        short[] modifiedBuffer = new short[modifiedBufferSize];

        for (int i = 0, j = 0; i < bytesRead; i++) {
            // Canal izquierdo
            modifiedBuffer[j++] = buffer[i];
            // Canal derecho (silencioso)
            modifiedBuffer[j++] = 0;
        }

        return modifiedBuffer;
    }

    private void verificarPermisos() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            Log.d("O", "NO entro");

        } else {
            Log.d("O", "entro");
            inicializarReconocedorVoz();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                inicializarReconocedorVoz();
            } else {
                Log.d("O", "Permiso denegado");
            }
        }
    }
    private void toggleVoiceRecognition() {
        if (!isListening) {
            result.setVisibility(View.VISIBLE);
            startVoiceRecognition();
            isActive = true;
        } else {
            stopVoiceRecognition();
        }
    }
    private void startVoiceRecognition() {
        runOnUiThread(() -> {

            startTimeSelectedVocal = System.currentTimeMillis();
            startTimeVocalDetection = System.currentTimeMillis();
            if (speechRecognizer == null) {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                } catch (Exception e) {
                    Log.e("SpeechRecognizer", "Error al crear SpeechRecognizer: " + e.getMessage());
                    return;
                }
                inicializarReconocedorVoz();
            }

            Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            try {
                speechRecognizer.startListening(recognizerIntent);
                isListening = true;
                Log.d("SpeechRecognizer", "Start listening");
            } catch (Exception e) {
                Log.e("SpeechRecognizer", "Error al iniciar el reconocimiento: " + e.getMessage());
            }
        });
    }

    private void inicializarReconocedorVoz() {
        SharedPreferences preferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        String selectedVocal = preferences.getString("selectedVocal", "LA");
        Log.d("SharedPreferencesRecibo", "Vocal seleccionada desde SharedPreferences: " + selectedVocal);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.d("SpeechRecognizer", "Ready for speech");
                startTimeSpeaking = System.currentTimeMillis();
                startTimeVowelMap.clear();
                isListening = true;
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Beginning of speech");
                // Calcula el tiempo transcurrido desde el inicio hasta aquí
                long elapsedTime = System.currentTimeMillis() - startTimeSpeaking;
                Log.d("SpeechRecognizer", "Tiempo hasta comenzar a hablar: " + elapsedTime + " ms");
                verificarPermisos();

            }

            @Override
            public void onRmsChanged(float v) {
                if (v > RMS_THRESHOLD) {

                    if (!currentVowel.toString().isEmpty()) {
                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - startTimeSpeaking;
                        vocalDurations.put(currentVowel.toString(), vocalDurations.getOrDefault(currentVowel.toString(), 0L) + elapsedTime);
                        actualizarDuracionVocales();

                    }
                    currentVowel = new StringBuilder();
                    startTimeVowelMap.clear();
                } else {
                    if (!currentVowel.toString().isEmpty()) {
                        actualizarDuracionVocales();
                    }

                }
                // Actualiza el progreso de la barra aquí cuando cambia el RMS
                if (isSpeaking && isVowelMatch) {
                    long currentTime = System.currentTimeMillis();
                    elapsedTimeSinceVocalDetection = currentTime - startTimeVocalDetection;
                    if (currentVowel.toString().toLowerCase().equals(selectedVocal.toLowerCase())) {
                        long tiempoTranscurrido = currentTime - startTimeSpeaking;
                        actualizarTiempoPronunciacion(tiempoTranscurrido);
                    }
                }
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.d("SpeechRecognizer", "Buffer received: " + Arrays.toString(buffer));

            }

            @Override
            public void onEndOfSpeech() {
                Log.d("SpeechRecognizer", "End of speech");
                isListening = false;
                isSpeaking = false;
                //startVoiceRecognition();
            }

            @Override
            public void onError(int error) {
                Log.e("SpeechRecognizer", "Error during recognition: " + error);
                onSpeechEnd();
                if (error == SpeechRecognizer.ERROR_SERVER) {
                    startVoiceRecognition();
                } else if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    startVoiceRecognition();
                } else {
                    Log.e("SpeechRecognizer", "Error durante el reconocimiento: " + error);
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        verificarPermisos();
                    }
                }

            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String result = matches.get(0);
                    Log.d("SpeechRecognizer", "Recognition result: " + result);
                    //startVoiceRecognition();
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String results = matches.get(0);
                    Log.d("SpeechRecognizer", "Partial result: " + results);
                    String vowels = getVowels(results);
                    updateTvResult(vowels);
                    if (!vowels.isEmpty()) {
                        currentVowel = new StringBuilder(vowels);
                        Log.d("SpeechRecognizer", "currentVowel: " + currentVowel.toString());
                        Log.d("SpeechRecognizer", "selectedVocal: " + selectedVocal);
                        if (currentVowel.toString().toLowerCase().equals(selectedVocal.toLowerCase())) {
                            long currentTime = System.currentTimeMillis();
                            long elapsedTimeSinceVocalDetection = currentTime - startTimeVocalDetection;

                            startTimeVocalDetection = currentTime;
                            Log.d("SpeechRecognizer", "La vocal coincide: " + selectedVocal);

                            if (!isSpeaking) {
                                // Inicia el tiempo de voz solo si no está hablando actualmente
                                startTimeSpeaking = currentTime;
                                isSpeaking = true;
                                //start...
                            }
                            isVowelMatch = true;
                            startTimeVocalDetection = System.currentTimeMillis();
                            startTimeSpeaking = startTimeVocalDetection;
                            startTimeVowelMap.put(currentVowel.toString(), startTimeSpeaking);
                        } else {
                            isSpeaking = false;
                            isVowelMatch = false;
                            Log.d("SpeechRecognizer", "La vocal no coincide. Vocal seleccionada: " + selectedVocal);
                        }
                    }
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }


    private void actualizarTiempoPronunciacion(long tiempoTranscurrido) {
        int segundos = (int) (tiempoTranscurrido / 1000);
        if (isSpeaking) {
            uiHandler.post(() -> {
                if (result != null) {

                    result.setText("Tiempo de pronunciación:  " + segundos + "  segundos"+"\n de la vocal  "+ currentVowel);

                } else {
                    Log.e("TiempoPronunciacionError", "tiempoPronunciacion es nulo");
                }
            });
        }
    }


    private void updateTvResult(String text) {
        uiHandler.post(() -> {
            if (tvResult != null) {
                //tvResult.setText(text);
            } else {
                Log.e("TvResultError", "tvResult es nulo");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void onSpeechEnd() {
        Log.d("SpeechRecognizer", "End of speech");
        if (!currentVowel.toString().isEmpty()) {
            long elapsedTime = System.currentTimeMillis() - startTimeSpeaking;
            vocalDurations.put(currentVowel.toString(), vocalDurations.getOrDefault(currentVowel.toString(), 0L) + elapsedTime);
        }
        final String durationText = getDurationText();  // Almacenar el resultado en una variable
        uiHandler.post(() -> {
            if (timetext != null) {
                //timetext.setText("Total: " + durationText + " ms");
            } else {
                Log.e("DuracionError", "timetext es nulo");
            }
        });
        Log.d("Duracion Total ", getDurationText());
        // Restablecer variables
        currentVowel = new StringBuilder();
        isSpeaking = false;
        isVowelMatch = false;
        startTimeSpeaking = 0;
    }

    @SuppressLint("SetTextI18n")
    private void actualizarDuracionVocales() {
        long totalDuration = 0;
        for (Map.Entry<String, Long> entry : startTimeVowelMap.entrySet()) {
            String vowel = entry.getKey();
            long startTime = entry.getValue();
            long duration = System.currentTimeMillis() - startTime;
            totalDuration += duration;
            vocalDurations.put(vowel, duration);
        }
        startTimeVowelMap.clear();
        //vocalDurations.clear();
    }
    private String getDurationText() {
        StringBuilder resultText = new StringBuilder();
        for (Map.Entry<String, Long> entry : vocalDurations.entrySet()) {
            resultText.append(entry.getKey())
                    .append("duración total : ")
                    .append(entry.getValue())
                    .append(" ms\n");
        }
        return resultText.toString();
    }
    private String getVowels(String input) {
        return input.replaceAll("[^aeiouAEIOU]", "");
    }

    private void stopVoiceRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
    @Override
    protected void onStop(){
        super.onStop();
        Log.d("dur_largo", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("dur_largo", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }

}

