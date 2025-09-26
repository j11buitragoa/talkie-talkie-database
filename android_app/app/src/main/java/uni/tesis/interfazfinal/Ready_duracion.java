package uni.tesis.interfazfinal;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ready_duracion extends AppCompatActivity {
    Button okButton, adminButton;
    String selectLevel;
    Intent goDuracion;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ready_duracion);

        okButton = findViewById(R.id.okButton);
        adminButton = findViewById(R.id.adminButton);
        goDuracion = new Intent(this,duracion.class);

        okButton.setOnClickListener(view -> {
            startActivity(goDuracion);
        });

        adminButton.setOnClickListener(v -> {
            showAdminDuracionDialog();
        });
    }
    private void showAdminDuracionDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.customAlertDialogTalkie);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_admin_habla_duracion,null);
        dialogView.setBackgroundResource(R.drawable.rounded_white_background);

        builder.setView(dialogView);

        Spinner spinnerLevel;
        Button saveButton, cancelButton;
        EditText editTiempoSilencio,editTiempoHabla, editIntentos;
        String[] level = new String[]{
                "Selecciona un nivel",
                "Nivel 1"
        };

        // Muestra el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();

        spinnerLevel = dialogView.findViewById(R.id.spinnerLevel);
        editTiempoSilencio = dialogView.findViewById(R.id.editTiempoSilencio);
        editTiempoHabla = dialogView.findViewById(R.id.editTiempoHabla);
        editIntentos = dialogView.findViewById(R.id.editIntentos);
        saveButton = dialogView.findViewById(R.id.saveButton);
        cancelButton = dialogView.findViewById(R.id.cancelButton);

        final List<String> devicesList = new ArrayList<>(Arrays.asList(level));
        final ArrayAdapter<String> adapterDevice = new ArrayAdapter<String>(this,R.layout.spinner_item,devicesList){
            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
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
                datos.add(editTiempoSilencio.getText().toString());
                datos.add(editTiempoHabla.getText().toString());
                datos.add(editIntentos.getText().toString());
                goDuracion.putStringArrayListExtra(selectLevel, datos);
                Toast.makeText(this, "        " + selectLevel + "\nDatos guardados", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Selecciona un nivel", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.cancel();
        });
    }
}