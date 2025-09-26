package uni.tesis.interfazfinal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class grabaciones extends AppCompatActivity {
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread playingThread;
    private boolean isPlaying = false;
    private boolean isRecording = false;
    private Thread recordingThread;
    private long tiempoInicio;

    private Button grabA,grabE,grabI,grabO,grabU,grabar,guardar,eliminar,repro,listo, nuevo,backButton;
    private static String fileName = null;
    private String currentButtonLetter;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grabaciones);
        tiempoInicio = System.currentTimeMillis();

        grabA = findViewById(R.id.grabA);
        grabE = findViewById(R.id.grabE);
        grabI = findViewById(R.id.grabI);
        grabO = findViewById(R.id.grabO);
        grabU=  findViewById(R.id.grabU);
        grabar = findViewById(R.id.grabar);
        guardar = findViewById(R.id.guardar);
        eliminar = findViewById(R.id.eliminar);
        repro = findViewById(R.id.repro);
        listo= findViewById(R.id.listo);
        nuevo = findViewById(R.id.nuevo);
        backButton = findViewById(R.id.backButton);

        grabA.setOnClickListener(v -> {
            setCurrentButtonLetter("a");
            grabar.setVisibility(View.VISIBLE);
            guardar.setVisibility(View.VISIBLE);
            eliminar.setVisibility(View.VISIBLE);
            repro.setVisibility(View.VISIBLE);
            listo.setVisibility(View.VISIBLE);

        });
        grabE.setOnClickListener(v -> {
            setCurrentButtonLetter("e");
            grabar.setVisibility(View.VISIBLE);
            guardar.setVisibility(View.VISIBLE);
            eliminar.setVisibility(View.VISIBLE);
            repro.setVisibility(View.VISIBLE);
            listo.setVisibility(View.VISIBLE);


        });
        grabI.setOnClickListener(v -> {
            setCurrentButtonLetter("i");
            grabar.setVisibility(View.VISIBLE);
            guardar.setVisibility(View.VISIBLE);
            eliminar.setVisibility(View.VISIBLE);
            repro.setVisibility(View.VISIBLE);
            listo.setVisibility(View.VISIBLE);

        });

        grabO.setOnClickListener(v -> {
            setCurrentButtonLetter("o");
            grabar.setVisibility(View.VISIBLE);
            guardar.setVisibility(View.VISIBLE);
            eliminar.setVisibility(View.VISIBLE);
            repro.setVisibility(View.VISIBLE);
            listo.setVisibility(View.VISIBLE);

        });
        grabU.setOnClickListener(v -> {
            setCurrentButtonLetter("u");
            grabar.setVisibility(View.VISIBLE);
            guardar.setVisibility(View.VISIBLE);
            eliminar.setVisibility(View.VISIBLE);
            repro.setVisibility(View.VISIBLE);
            listo.setVisibility(View.VISIBLE);
        });

        listo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("Presiona otro boton para grabar");
                listo.setVisibility(View.GONE);
                grabar.setVisibility(View.GONE);
                guardar.setVisibility(View.GONE);
                eliminar.setVisibility(View.GONE);
                repro.setVisibility(View.GONE);

            }
        });
        grabar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });
        guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
                guardarNombreGrabacion(fileName);
                Log.d(TAG, "Nombre del archivo después de detener la grabación: " + fileName);
            }
        });
        repro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPlaying) {
                    startPlaying();
                } else {
                    stopPlaying();
                }
            }
        });
        eliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteRecording();
            }
        });
        nuevo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog();

            }
        });
        backButton.setOnClickListener(v -> {
            Intent goMenu = new Intent(this, MainActivity.class);
            startActivity(goMenu);
            finish();
        });

    }
    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opciones de grabación");
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog, null);
        builder.setView(dialogView);


        final EditText editText = dialogView.findViewById(R.id.editText);
        final Button buttonStartRecording = dialogView.findViewById(R.id.buttonStartRecording);
        final Button buttonStopRecording = dialogView.findViewById(R.id.buttonStopRecording);
        final Button buttonrepro = dialogView.findViewById(R.id.buttonrepro);
        final Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        final Button buttonReady=dialogView.findViewById(R.id.buttonReady);

        final AlertDialog alertDialog = builder.create();

        buttonStartRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentButtonLetter = editText.getText().toString();
                startRecording();
            }
        });

        buttonStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        buttonrepro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    startPlaying();
                } else {
                    stopPlaying();
                }
            }
        });
        buttonReady.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                guardarNombreGrabacion(editText.getText().toString());
                alertDialog.dismiss();

            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteRecording();
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }
    private void setCurrentButtonLetter(String letter) {
        currentButtonLetter = letter;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void startRecording() {
        Log.d(TAG, "Grabando ");
        showToast("Iniciando grabación");
        if (currentButtonLetter != null) {
            fileName = getExternalCacheDir().getAbsolutePath() + "/grabacion_" + currentButtonLetter + ".pcm";
        } else {
            showToast("Ningún botón seleccionado");
            return;  // Si no hay botón seleccionado, no inicies la grabación
        }

        int bufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

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
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        final short[] audioBuffer = new short[bufferSize];

        audioRecord.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    private void stopRecording() {
        Log.d(TAG, "Grbaciòn detenida  ");
        showToast("Grabación detenida");

        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;

        try {
            recordingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void writeAudioDataToFile() {
        byte[] data = new byte[AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)];

        DataOutputStream os = null;

        try {
            os = new DataOutputStream(new FileOutputStream(fileName));

            while (isRecording) {
                int read = audioRecord.read(data, 0, data.length);
                if (AudioRecord.ERROR_INVALID_OPERATION != read && os != null) {
                    os.write(data, 0, read);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void startPlaying() {
        Log.d(TAG, "Reproduciendo ");
        showToast("Iniciando reproducción");

        int bufferSize = AudioTrack.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        final byte[] audioData = new byte[bufferSize];

        audioTrack.play();
        isPlaying = true;

        playingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DataInputStream is = new DataInputStream(new FileInputStream(fileName));
                    while (isPlaying) {
                        int read = is.read(audioData, 0, audioData.length);
                        if (read > 0) {
                            audioTrack.write(audioData, 0, read);
                        } else {
                            break;
                        }
                    }
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "AudioPlayer Thread");

        playingThread.start();
    }

    private void stopPlaying() {
        Log.d(TAG, "Reproducción detenida ");
        showToast("Reproducción detenida");

        isPlaying = false;
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;

        try {
            playingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void deleteRecording() {
        Log.d(TAG, "Eliminando grabación anterior ");
        if (currentButtonLetter != null) {
            fileName = getExternalCacheDir().getAbsolutePath() + "/grabacion_" + currentButtonLetter + ".pcm";
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
                showToast("Grabación eliminada");
                fileName = null;
            } else {
                showToast("No hay grabación para eliminar");
            }
            fileName = null;
            currentButtonLetter = null;
        } else {
            showToast("Ningún botón seleccionado");
        }
    }
    private void guardarNombreGrabacion(String nombreGrabacion) {
        if (currentButtonLetter != null) {
            String fileName = getExternalCacheDir().getAbsolutePath() + "/grabacion_" + currentButtonLetter + ".pcm";
            SharedPreferences preferences = getSharedPreferences("Grabaciones", Context.MODE_PRIVATE);
            Set<String> grabacionesSet = preferences.getStringSet("grabaciones", new HashSet<>());
            grabacionesSet.add(fileName);
            Log.d(TAG, "guardarNombregrab1: " + fileName);
            preferences.edit().putStringSet("grabaciones", grabacionesSet).apply();

            // Aquí guardamos el nombre de la grabación asociado al botón en las preferencias
            SharedPreferences.Editor editor = getSharedPreferences("MyPreferencesGrab", Context.MODE_PRIVATE).edit();
            editor.putString(currentButtonLetter, fileName);
            Log.d(TAG, "guardarNombregrab: " + fileName);
            editor.apply();
        }
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();

    }
    @Override
    protected void onStop(){
        super.onStop();
        Log.d("grab", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("grab", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }

}