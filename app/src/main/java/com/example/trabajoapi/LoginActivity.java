package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trabajoapi.data.LoginRequest;
import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private EditText etNif, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Si ya hay sesión, ir al Main
        if (sessionManager.getAuthToken() != null) {
            irAMain();
        }

        etNif = findViewById(R.id.etNif);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nif = etNif.getText().toString().toUpperCase().trim();
                String pass = etPassword.getText().toString().trim();

                if (nif.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                realizarLogin(nif, pass);
            }
        });
    }

    private void realizarLogin(String nif, String password) {
        LoginRequest request = new LoginRequest(nif, password);
        Call<LoginResponse> call = RetrofitClient.getInstance().getMyApi().login(request);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveAuthToken(response.body().getAccessToken());
                    Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                    irAMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_ERROR).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}