package uni.tesis.interfazfinal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String TAG = "TAG";

    private final String USERS_COLLECTION = "User";
    private final String AGE = "edad";
    private final String NAME = "nombre";
    private final String TYPE_DEV = "tipo de dispositivo";
    private final String ADMIN = "es admin";
    private final String SEXO = "sexo";
    private String selectSexo, selectDevice;

    private Button loginButtonReg, loginButton;
    private EditText username, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        FirebaseApp.initializeApp(this);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        ImageView about=findViewById(R.id.about);

        loginButtonReg = findViewById(R.id.loginButtonReg);
        loginButton = findViewById(R.id.loginButtonLog);

        loginButton.setOnClickListener(view -> {
            loginUser();
        });

        loginButtonReg.setOnClickListener(v -> showRegisterDialog());
        about.setOnClickListener(view -> {
            String urlVideo = "https://youtu.be/e8tVHJpOgEc";

            // Crear un Intent con la acción ACTION_VIEW
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // Establecer la URL del video en el Intent
            intent.setData(Uri.parse(urlVideo));

            // Intentar iniciar la actividad para abrir el video
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // Manejar la excepción si no se encuentra una actividad para manejar la intención
                e.printStackTrace();

                // Puedes mostrar un mensaje de error o proporcionar una alternativa aquí
            }
        });
    }

    private void loginUser(){
        String usuario = username.getText().toString();
        String contraseña = password.getText().toString();
        Log.d(TAG, "Usuario " + usuario);

        if (TextUtils.isEmpty(usuario) || TextUtils.isEmpty(contraseña)) {
            Toast.makeText(Login.this, "Por favor, ingrese su usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(usuario+"@app.com",contraseña).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Log.d(TAG, "user " + mAuth.getCurrentUser().getDisplayName() + "\nID " + mAuth.getCurrentUser().getUid());
                Intent goMenu = new Intent(Login.this, MainActivity.class);
                startActivity(goMenu);
                finish();
            }else {
                // Error en el inicio de sesión
                Toast.makeText(Login.this, "No existe usuario o su contraseña es incorrecta . Regístrese por favor", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showRegisterDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.customAlertDialogTalkie);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.registro,null);
        dialogView.setBackgroundResource(R.drawable.rounded_white_background);

        builder.setView(dialogView);

        EditText usuarioN, contraseñaN, edadN, nameN;
        Switch adminSW;
        Spinner spinnerSexo, spinnerDevice;
        Button buttonCancel, buttonRegister;


        usuarioN = dialogView.findViewById(R.id.usuarioN);
        contraseñaN = dialogView.findViewById(R.id.contraseñaN);
        edadN = dialogView.findViewById(R.id.edadN);
        nameN = dialogView.findViewById(R.id.nameN);
        spinnerSexo = dialogView.findViewById(R.id.spinnerSexo);
        spinnerDevice = dialogView.findViewById(R.id.spinnerDevice);
        adminSW = dialogView.findViewById(R.id.switchAdmin);
        buttonRegister = dialogView.findViewById(R.id.buttonRegister);
        buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        String[] devices = new String[]{
                "Dispositivo",
                "Auriculares",
                "Implante",
                "Ninguno"
        };
        String[] sexo = new String[]{
                "Sexo",
                "Femenino",
                "Masculino"
        };

        final List<String> devicesList = new ArrayList<>(Arrays.asList(devices));
        final List<String> sexoList = new ArrayList<>(Arrays.asList(sexo));

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
        spinnerDevice.setAdapter(adapterDevice);

        final ArrayAdapter<String> adapterSexo = new ArrayAdapter<String>(this,R.layout.spinner_item,sexoList){
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
        adapterSexo.setDropDownViewResource(R.layout.spinner_item);
        spinnerSexo.setAdapter(adapterSexo);

        spinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    //Toast.makeText(Login.this, "Por favor, seleccione una opción", Toast.LENGTH_SHORT).show();
                }else {
                    selectDevice = parent.getItemAtPosition(position).toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerSexo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0){
                    //Toast.makeText(Login.this, "Por favor, seleccione una opción", Toast.LENGTH_SHORT).show();
                }else {
                    selectSexo = parent.getItemAtPosition(position).toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // Muestra el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();

        buttonRegister.setOnClickListener(v -> {

            if (selectSexo == null){
                Toast.makeText(Login.this, "Selecciona un sexo", Toast.LENGTH_SHORT).show();
            }else if (selectDevice == null) {
                Toast.makeText(Login.this, "Selecciona un dispositivo", Toast.LENGTH_SHORT).show();
            }else {
                String usuario = usuarioN.getText().toString().toLowerCase();
                String contraseña = contraseñaN.getText().toString();
                String edad = edadN.getText().toString();
                String nombre = nameN.getText().toString();

                if (TextUtils.isEmpty(usuario) || TextUtils.isEmpty(contraseña) || TextUtils.isEmpty(edad)) {
                    Toast.makeText(Login.this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (contraseña.length() < 6) {
                    Toast.makeText(Login.this, "Ingrese una contraseña válida (al menos 6 caracteres)", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Agrega info del usuario
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put(NAME, nombre);
                userInfo.put(AGE, edad);
                userInfo.put(SEXO,selectSexo);
                userInfo.put(TYPE_DEV, selectDevice);
                userInfo.put(ADMIN,adminSW.isChecked());

                String mail = usuario + "@app.com";

                db.collection(USERS_COLLECTION).document(mail).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()){
                                Toast.makeText(Login.this, "El usuario ya está registrado.Inicie sesión", Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }else {
                                mAuth.createUserWithEmailAndPassword(mail,contraseña).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        db.collection(USERS_COLLECTION).document(mail).set(userInfo);
                                        Toast.makeText(Login.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                        dialog.cancel();
                                    }else {
                                        // Fallo en el registro
                                        Toast.makeText(Login.this, "El usuario ya existe. Inicie sesion o digite otro usuario ", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(e -> {

                        });
            }

        });

        buttonCancel.setOnClickListener(v -> {
            dialog.cancel();
        });

    }
}