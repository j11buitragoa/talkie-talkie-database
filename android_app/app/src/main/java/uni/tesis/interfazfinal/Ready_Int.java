package uni.tesis.interfazfinal;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ready_Int extends AppCompatActivity {
    Button okButton, adminButton;
    String selectLevel;

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    RingDrawer ringDrawer;
    Intent goIntensidad;
    private int ringWidth, ringSize;
    private long tiempoInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ready_int);
        tiempoInicio = System.currentTimeMillis();

        okButton = findViewById(R.id.okButton);
        adminButton = findViewById(R.id.adminButton);
        goIntensidad = new Intent(this,Habla_Intensidad.class);

        okButton.setOnClickListener(view -> {
            startActivity(goIntensidad);
            finish();
        });

        adminButton.setOnClickListener(v -> {
            showAdminIntDialog();
        });
    }

    private void showAdminIntDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.customAlertDialogTalkie);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_admin_habla_intensidad,null);
        dialogView.setBackgroundResource(R.drawable.rounded_white_background);

        builder.setView(dialogView);

        Spinner spinnerLevel;
        Button saveButton, cancelButton;
        EditText editTiempoHabla;
        SeekBar seekBarWidth, seekBarSize;

        String[] level = new String[]{
                "Selecciona un nivel",
                "Nivel 1"
        };

        // Muestra el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();

        spinnerLevel = dialogView.findViewById(R.id.spinnerLevel);
        editTiempoHabla = dialogView.findViewById(R.id.editTiempoHabla);
        seekBarWidth = dialogView.findViewById(R.id.seekBarWidth);
        seekBarSize = dialogView.findViewById(R.id.seekBarSize);
        saveButton = dialogView.findViewById(R.id.saveButton);
        cancelButton = dialogView.findViewById(R.id.cancelButton);

        surfaceView = dialogView.findViewById(R.id.surfaceView);
        surfaceView.setZOrderMediaOverlay(true);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        ringDrawer = new RingDrawer(surfaceHolder);

        final List<String> devicesList = new ArrayList<>(Arrays.asList(level));
        final ArrayAdapter<String> adapterDevice = new ArrayAdapter<String>(this,R.layout.spinner_item,devicesList){
            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                }
                else
                {
                    return true;
                }
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        adapterDevice.setDropDownViewResource(R.layout.spinner_item);
        spinnerLevel.setAdapter(adapterDevice);

        spinnerLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    //Toast.makeText(Login.this, "Por favor, seleccione una opción", Toast.LENGTH_SHORT).show();
                }else {
                    selectLevel = parent.getItemAtPosition(position).toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        saveButton.setOnClickListener(v -> {
            if (selectLevel != null){
                ArrayList<String> datos = new ArrayList<>();
                datos.add(editTiempoHabla.getText().toString());
                datos.add(String.valueOf(ringSize));
                datos.add(String.valueOf(ringWidth));
                goIntensidad.putStringArrayListExtra(selectLevel, datos);
                Toast.makeText(this, "        " + selectLevel + "\nDatos guardados", Toast.LENGTH_SHORT).show();
                //startActivity(goIntensidad);
            }else {
                Toast.makeText(this, "Selecciona un nivel", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.cancel();
        });

        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ringSize = progress;
                ringDrawer.drawRingWithSize(ringSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekBarWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ringWidth = progress;
                ringDrawer.drawRingWithWidth(ringWidth);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private class RingDrawer {
        private SurfaceHolder surfaceHolder;
        private int ringSize = 0;
        private int ringWidth = 50;

        RingDrawer(SurfaceHolder holder) {
            surfaceHolder = holder;
        }

        void drawRingWithSize(int size){
            ringSize = size;
            draw(ringSize,ringWidth);
        }

        void drawRingWithWidth(int width){
            ringWidth = width;
            draw(ringSize,ringWidth);
        }

        void draw(int ringSize, int ringWidth) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.WHITE);
                int centerX = canvas.getWidth() / 2;
                int centerY = canvas.getHeight() / 2;

                // Dibujar anillo
                if (ringSize > 0){
                    Paint ringPaint = new Paint();
                    ringPaint.setColor(Color.argb(100,189,236,182));
                    ringPaint.setStyle(Paint.Style.STROKE);
                    ringPaint.setStrokeWidth(ringWidth);
                    canvas.drawCircle(centerX, centerY, ringSize / 2, ringPaint);
                }

                surfaceHolder.unlockCanvasAndPost(canvas);
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
        Log.d("Habal_int_adm", "onDestroy - Llamado");
        long tiempoSesionActual = System.currentTimeMillis() - tiempoInicio;
        TimeT.guardarTiempoAcumulado(this, tiempoSesionActual);
        Log.d("Habal_int_adm", "onDestroy - Tiempo acumulado: " + tiempoSesionActual);
    }
}