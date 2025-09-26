package uni.tesis.interfazfinal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
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

public class repro_staccato extends AppCompatActivity {
    private Button comenzar,reproa,intentalo,admin,volver;
    private TextView fonema, coincide,silencio;
    private AudioTrack audioTrack;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private long tiempoInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repro_staccato);
        tiempoInicio = System.currentTimeMillis();

        Intent intent = getIntent();
        SharedPreferences preferences = getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        final String nombreGrabacion = preferences.getString("nombreGrabacion", "");
        final String fonema1=preferences.getString("fonema", "");
        final String silencio1=preferences.getString("silencio","");

        Log.d("Nombre en Reproduccion AC", "Nombre de grabación seleccionada: " +nombreGrabacion);
        // Inicializar AudioTrack y reproducir la grabación
        comenzar=findViewById(R.id.comenzar);
        reproa=findViewById(R.id.reproa);
        fonema=findViewById(R.id.fonema);
        coincide=findViewById(R.id.coincide);
        silencio=findViewById(R.id.silencio);
        intentalo=findViewById(R.id.intentalo);
        admin=findViewById(R.id.admin);
        volver=findViewById(R.id.volver);

        String textoFonema = "Debes pronunciar: " + fonema1;
        fonema.setText(textoFonema);
        String textosilencio = "Debes mantenerte en silencio: " + silencio1+"segundos entre cada pronunciación";
        silencio.setText(textosilencio);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new MyRecognitionListener());

        admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(repro_staccato.this, confi_dstc.class);
                startActivity(intent);
                finish();
            }
        });
        comenzar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(repro_staccato.this, duracion_staccato.class);
                startActivity(intent);
                finish();
            }
        });
        reproa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reproducirGrabacion(nombreGrabacion);
            }
        });
        intentalo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isListening) {
                    startSpeechRecognition();
                } else {
                    stopSpeechRecognition();
                }
            }
        });
        volver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(repro_staccato.this, Habla_Frame.class);
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

    private void startSpeechRecognition() {
        isListening = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es"); // Cambia a tu idioma preferido


        speechRecognizer.startListening(intent);
    }

    private void stopSpeechRecognition() {
        isListening = false;
        speechRecognizer.stopListening();
    }
    private class MyRecognitionListener implements RecognitionListener {
        SharedPreferences preferences=getSharedPreferences("Preferences_new", Context.MODE_PRIVATE);
        String fonema=preferences.getString("fonema","LA");

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d("SpeechRecognizer", "Ready for speech");

        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("SpeechRecognizer", "Beginning of speech");

        }

        @Override
        public void onRmsChanged(float v) {
            // Verifica si el nivel de sonido es mayor que el umbral establecido
        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onResults(Bundle results) {
            updateResultView(results);
            // Reinicia la escucha para la adquisición continua
            if (isListening) {
                startSpeechRecognition();
            }

        }

        @Override
        public void onPartialResults(Bundle bundle) {
            updateResultView(bundle);

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
            String result = matches.get(0);
            if (result.equalsIgnoreCase(fonema)) {
                coincide.setText("Texto reconocido: " + result);

            } else {
                coincide.setText("No coincidió la pronunciación, intentalo de nuevo");
            }
        }
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();

    }
    @Override
    protected void onStop(){
        super.onStop();
        Log.d("dura_cort", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("dura_cort", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }
}