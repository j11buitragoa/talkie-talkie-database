package uni.tesis.interfazfinal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class admin_tono extends AppCompatActivity {
    Points myApp;
    long startTime;

    private Spinner spinnertono;
    private Button enviar;
    private Button salir;
    private String TAG = "TAG";

    private EditText sostenido;
    private EditText veces;
    private EditText tsilencio;
    private final String USERS_COLLECTION = "Usuarios";
    private final String TALK_COLLECTION = "TALK";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private CollectionReference talkCollection;
    private DocumentReference talktonoDocument;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_tono);
        spinnertono = findViewById(R.id.spinnertono);
        String[] items = {"Grave", "Agudo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnertono.setAdapter(adapter);
        enviar=findViewById(R.id.enviar);
        salir=findViewById(R.id.salir);
        sostenido=findViewById(R.id.sostenido);
        veces=findViewById(R.id.veces);
        tsilencio=findViewById(R.id.tsilencio);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        talkCollection = db.collection(USERS_COLLECTION).document(user.getEmail()).collection(TALK_COLLECTION);
        talktonoDocument = talkCollection.document("Tono ");


        spinnertono.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = items[position];
                // Guardar el valor seleccionado en SharedPreferences

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sostenido.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    enviar.setEnabled(false);
                } else {
                    try{
                        int valor=Integer.parseInt(s.toString());
                        if (valor>=1&&valor<=10) {
                            enviar.setEnabled(true);
                        }else {
                            enviar.setEnabled(false);
                        }
                    }catch (NumberFormatException e){
                        enviar.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        veces.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    enviar.setEnabled(false);
                } else {
                    try{
                        int valor=Integer.parseInt(s.toString());
                        if (valor>=1&&valor<=10) {
                            enviar.setEnabled(true);
                        }else {
                            enviar.setEnabled(false);
                        }
                    }catch (NumberFormatException e){
                        enviar.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        tsilencio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    enviar.setEnabled(false);
                } else {
                    try{
                        int valor=Integer.parseInt(s.toString());
                        if (valor>=1&&valor<=10) {
                            enviar.setEnabled(true);
                        }else {
                            enviar.setEnabled(false);
                        }
                    }catch (NumberFormatException e){
                        enviar.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarCampos();
                if (enviar.isEnabled()) {
                    String selectedItem = spinnertono.getSelectedItem().toString();
                    String sostenidoValue = sostenido.getText().toString();
                    String vecesValue = veces.getText().toString();
                    String tsilencioValue = tsilencio.getText().toString();


                    sendDataBase(talktonoDocument);


                    SharedPreferences preferences = getSharedPreferences("tono", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("selectedItem", selectedItem);

                    try {
                        int sostenidoInt = Integer.parseInt(sostenidoValue);
                        editor.putInt("sostenidoValue", sostenidoInt);
                    } catch (NumberFormatException e) {
                    }

                    try {
                        int vecesInt = Integer.parseInt(vecesValue);
                        editor.putInt("vecesValue", vecesInt);
                    } catch (NumberFormatException e) {
                    }

                    try {
                        int tsilencioInt = Integer.parseInt(tsilencioValue);
                        editor.putInt("tsilencioValue", tsilencioInt);
                    } catch (NumberFormatException e) {
                    }
                    editor.apply();
                }

            }
        });
        salir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(admin_tono.this,tono.class);
                startActivity(intent);
            }
        });


    }
    private void sendDataBase(DocumentReference doc){

        Map<String, Object> mapa = new HashMap<>();
        String selectedItem = spinnertono.getSelectedItem().toString();
        String sostenidoValue = sostenido.getText().toString();
        String vecesValue = veces.getText().toString();
        String tsilencioValue = tsilencio.getText().toString();
        mapa.put("Tono " , selectedItem);
        mapa.put("Duración del tono " ,sostenidoValue);
        mapa.put("Repeteciones " ,vecesValue);
        mapa.put("Tiempo de Silencio " ,tsilencioValue);

        doc.set(mapa, SetOptions.merge()).addOnSuccessListener(unused -> Log.d(TAG, "Enviado"));
    }
    void validarCampos() {
        String selectedItem = spinnertono.getSelectedItem().toString();
        String sostenidoValue = sostenido.getText().toString();
        String vecesValue = veces.getText().toString();
        String tsilencioValue = tsilencio.getText().toString();

        boolean camposLlenos = !selectedItem.isEmpty() &&
                !sostenidoValue.trim().isEmpty() &&
                !vecesValue.trim().isEmpty() &&
                !tsilencioValue.trim().isEmpty();

        boolean sostenidoValido = false;
        boolean vecesValido = false;
        boolean tsilencioValido = false;

        if (camposLlenos) {
            try {
                int sostenidoInt = Integer.parseInt(sostenidoValue);
                sostenidoValido = (sostenidoInt >= 1 && sostenidoInt <= 10);

                int vecesInt = Integer.parseInt(vecesValue);
                vecesValido = (vecesInt >= 1 && vecesInt <= 10);

                int tsilencioInt = Integer.parseInt(tsilencioValue);
                tsilencioValido = (tsilencioInt >= 1 && tsilencioInt <= 10);
            } catch (NumberFormatException e) {
            }
        }
        boolean camposValidos = camposLlenos && sostenidoValido && vecesValido && tsilencioValido;
        enviar.setEnabled(camposValidos);
        if (!camposValidos) {
            Toast.makeText(getApplicationContext(), "Por favor, complete todos los campos correctamente", Toast.LENGTH_SHORT).show();
        }
        enviar.setEnabled(camposLlenos && sostenidoValido && vecesValido && tsilencioValido);
    }
}