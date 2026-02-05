package com.example.trabajoapi;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.EmpresaConfigResponse;
import com.google.android.material.button.MaterialButton;
import android.widget.EditText;
import android.text.InputType;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin); // Necesitarás crear este layout

        sessionManager = new SessionManager(this);

        MaterialButton btnEmpleados = findViewById(R.id.btnAdminEmpleados);
        MaterialButton btnMapa = findViewById(R.id.btnAdminMapa);

        // 1. Ver Empleados
        btnEmpleados.setOnClickListener(v -> {
            // Aquí abrirías una Activity con un RecyclerView de empleados
            Toast.makeText(this, "Funcionalidad: Lista de Empleados", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, ListaEmpleadosActivity.class);
            // startActivity(intent);
        });

        // 2. Configuración GPS (Radio)
        btnMapa.setOnClickListener(v -> mostrarDialogoRadio());
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
        // Ejemplo simplificado. Deberías obtener primero la lat/lon actuales
        // y enviar el objeto completo.
        String token = "Bearer " + sessionManager.getAuthToken();
        EmpresaConfigResponse config = new EmpresaConfigResponse();
        config.setRadio(nuevoRadio);
        // config.setLatitud(...); // Necesario mantener la actual

        RetrofitClient.getInstance().getMyApi().updateEmpresaConfig(token, config)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if(response.isSuccessful()) Toast.makeText(AdminActivity.this, "Radio actualizado", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
    }
}