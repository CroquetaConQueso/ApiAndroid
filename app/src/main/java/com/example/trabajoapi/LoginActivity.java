package com.example.trabajoapi;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton; // Necesario para el nuevo botón

import com.example.trabajoapi.data.LoginRequest;
import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.ResetPasswordRequest;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etNif, etPassword;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Ocultar la barra superior (ActionBar) para que el diseño amarillo luzca limpio
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sessionManager = new SessionManager(this);
        if (sessionManager.getAuthToken() != null) {
            irAMain();
            return;
        }

        setContentView(R.layout.activity_login);

        // 2. CAMBIO DE IDs: Deben coincidir con el nuevo activity_login.xml
        etNif = findViewById(R.id.etNifLogin);      // Antes era etNif
        etPassword = findViewById(R.id.etPassLogin); // Antes era etPassword

        // El botón ahora es un AppCompatButton en el XML
        View btnLogin = findViewById(R.id.btnLogin);

        TextView tvForgotPassword = findViewById(R.id.tvForgot); // Antes tvForgotPassword

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoRecuperacion();
            }
        });
    }

    private void login() {
        String nif = etNif.getText().toString().trim().toUpperCase(); // Añadido toUpperCase por si acaso
        String password = etPassword.getText().toString().trim();

        if (nif.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest loginRequest = new LoginRequest(nif, password);
        Call<LoginResponse> call = RetrofitClient.getInstance().getMyApi().login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Mantenemos tu método getAccessToken()
                    sessionManager.saveAuthToken(response.body().getAccessToken());
                    Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                    irAMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoRecuperacion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RECUPERAR CLAVE");
        builder.setMessage("Introduce tu Email registrado:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); // Optimizado para email
        input.setHint("usuario@empresa.com");
        builder.setView(input);

        builder.setPositiveButton("ENVIAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String identificador = input.getText().toString().trim();
                if (!identificador.isEmpty()) {
                    solicitarResetApi(identificador);
                }
            }
        });

        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void solicitarResetApi(String identificador) {
        ResetPasswordRequest request = new ResetPasswordRequest(identificador);
        Call<Void> call = RetrofitClient.getInstance().getMyApi().resetPassword(request);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Ajuste visual: Mensaje más claro
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Correo enviado. Revisa tu bandeja.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "No se encontró el usuario.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void irAMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Flags para que no se pueda volver atrás al Login pulsando 'Atrás'
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}