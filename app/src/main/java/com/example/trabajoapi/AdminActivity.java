package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton; // <--- CAMBIO IMPORTANTE

import com.example.trabajoapi.data.EmpresaConfigResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        sessionManager = new SessionManager(this);

        // 1. BOTÓN VOLVER (Flecha atrás)
        ImageView btnVolver = findViewById(R.id.btnVolverAdmin);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        // 2. BUSCAR LOS BOTONES DEL MENÚ
        AppCompatButton btnEmpleados = findViewById(R.id.btnAdminEmpleados);
        AppCompatButton btnMapa = findViewById(R.id.btnAdminMapa);

        // 3. ACCIÓN: IR A LISTA DE EMPLEADOS
        btnEmpleados.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminEmpleadosActivity.class);
            startActivity(intent);
        });

        // 4. ACCIÓN: IR A CONFIGURACIÓN MAPA
        btnMapa.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminMapaActivity.class);
            startActivity(intent);
        });
    }
    private void mostrarDialogoRadio() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modificar Radio GPS (Metros)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String radioStr = input.getText().toString();
            if (!radioStr.isEmpty()) {
                actualizarRadio(Integer.parseInt(radioStr));
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void actualizarRadio(int nuevoRadio) {
        String token = "Bearer " + sessionManager.getAuthToken();
        EmpresaConfigResponse config = new EmpresaConfigResponse();
        config.setRadio(nuevoRadio);
        // Nota: En un caso real, deberías obtener primero la lat/lon actuales para no borrarlas
        // config.setLatitud(latActual);
        // config.setLongitud(lonActual);

        RetrofitClient.getInstance().getMyApi().updateEmpresaConfig(token, config)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminActivity.this, "Radio actualizado a " + nuevoRadio + "m", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AdminActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(AdminActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}