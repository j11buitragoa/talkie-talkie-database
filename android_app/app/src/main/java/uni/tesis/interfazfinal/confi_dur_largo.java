package uni.tesis.interfazfinal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class confi_dur_largo extends AppCompatActivity {
    private Handler handler = new Handler();
    private Button buttonstart;
    private Spinner spinner_voc,listagrab;
    private EditText sil_time,dur_time,veces;
    private long tiempoInicio;
    private String nombreGrabacionSeleccionada;
    private AudioTrack audioTrack;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confi_dur_largo);
        tiempoInicio = System.currentTimeMillis();
        listagrab =  findViewById(R.id.listagrab);
        spinner_voc=findViewById(R.id.spinner_voc);
        buttonstart=findViewById(R.id.buttonstart);
        sil_time=findViewById(R.id.sil_time);
        dur_time=findViewById(R.id.dur_time);
        veces=findViewById(R.id.veces);
        cargarListaGrabaciones();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.vocales_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_voc.setAdapter(adapter);
        spinner_voc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedVocal = (String) adapterView.getSelectedItem();
                Toast.makeText(confi_dur_largo.this, "Vocal seleccionada: " + selectedVocal, Toast.LENGTH_SHORT).show();
                SharedPreferences preferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                String fileName = preferences.getString(selectedVocal, "");
                editor.putString("selectedVocal", selectedVocal);
                editor.putString("file_name", fileName);

                editor.apply();



                if (!TextUtils.isEmpty(fileName)) {
                    // Aquí puedes utilizar el nombre de la grabación asociado a la vocal seleccionada
                    Toast.makeText(confi_dur_largo.this, "Nombre de la grabación asociado a " + selectedVocal + ": " + fileName, Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(confi_dur_largo.this, "No hay grabación asociada a " + selectedVocal, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Toast.makeText(confi_dur_largo.this, "No selecciono ninguna vocal  " , Toast.LENGTH_SHORT).show();

            }
        });

        sil_time.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().trim().isEmpty()) {
                    buttonstart.setEnabled(false);
                } else {
                    try {
                        int valor = Integer.parseInt(s.toString());
                        if (valor >= 1 && valor <= 10) {
                            buttonstart.setEnabled(true);
                        } else {
                            buttonstart.setEnabled(false);
                        }
                    } catch (NumberFormatException e) {
                        buttonstart.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        dur_time.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().trim().isEmpty()) {
                    buttonstart.setEnabled(false);
                } else {
                    try {
                        int valor = Integer.parseInt(s.toString());
                        if (valor >= 1 && valor <= 10) {
                            buttonstart.setEnabled(true);
                        } else {
                            buttonstart.setEnabled(false);
                        }
                    } catch (NumberFormatException e) {
                        buttonstart.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        veces.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().trim().isEmpty()) {
                    buttonstart.setEnabled(false);
                } else {
                    try {
                        int valor = Integer.parseInt(s.toString());
                        if (valor >= 1 && valor <= 10) {
                            buttonstart.setEnabled(true);
                        } else {
                            buttonstart.setEnabled(false);
                        }
                    } catch (NumberFormatException e) {
                        buttonstart.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        buttonstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nombreGrabacionSeleccionada != null && !nombreGrabacionSeleccionada.isEmpty()) {

                    SharedPreferences preferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                    String selectedVocal = preferences.getString("selectedVocal", "LA");
                    String veces_s = veces.getText().toString();
                    String silc_time = sil_time.getText().toString();
                    String dura_time = dur_time.getText().toString();
                    Log.d("Envio", selectedVocal);

                    // Todos los campos están llenos, proceder con la lógica actual
                    Log.d("Envio", selectedVocal);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("selectedVocal", selectedVocal);
                    editor.putString("veces", veces_s);
                    editor.putString("nombreGrabacion", nombreGrabacionSeleccionada);
                    editor.putString("silc_time", silc_time);
                    Log.d("ENVIO", silc_time);
                    editor.putString("dura_time", dura_time);
                    editor.apply();
                    // Verificar si los EditText están llenos
                    if (TextUtils.isEmpty(veces_s) || TextUtils.isEmpty(silc_time) || TextUtils.isEmpty(dura_time)) {
                        // Mostrar un mensaje o tomar alguna acción para indicar que los campos deben llenarse
                        Toast.makeText(confi_dur_largo.this, "Todos los campos deben estar llenos", Toast.LENGTH_SHORT).show();

                    } else {
                        Intent intent = new Intent(confi_dur_largo.this, repro_dur_largo.class);
                        startActivity(intent);
                        finish();
                    }
                }else {
                    // Si no se ha seleccionado una grabación, mostrar un mensaje al usuario
                    Toast.makeText(confi_dur_largo.this, "Por favor, seleccione una grabación", Toast.LENGTH_SHORT).show();
                }

            }
        });

        listagrab.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int i, long l) {
                nombreGrabacionSeleccionada = parentView.getItemAtPosition(i).toString();
                Log.d(TAG, "Nombre de grabación seleccionada: " + nombreGrabacionSeleccionada);
                reproducirGrabacion(nombreGrabacionSeleccionada);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }
    private void cargarListaGrabaciones() {
        SharedPreferences preferences = getSharedPreferences("Grabaciones", Context.MODE_PRIVATE);
        Set<String> grabacionesSet = preferences.getStringSet("grabaciones", new HashSet<>());
        List<String> grabacionesList = new ArrayList<>(grabacionesSet);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, grabacionesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listagrab.setAdapter(adapter);
    }
    private void reproducirGrabacion(String nombreGrabacion) {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        final int bufferSize = AudioTrack.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
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
            int bytesRead;

            while ((bytesRead = dataInputStream.read(buffer)) > 0) {
                audioTrack.write(buffer, 0, bytesRead);
            }

            dataInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();

    }
    @Override
    protected void onStop(){
        super.onStop();
        Log.d("confi_dur_largo", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("confi_dur_largo ", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }

}