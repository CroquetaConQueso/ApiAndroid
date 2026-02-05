package com.example.trabajoapi;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.TrabajadorResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminEmpleadosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_empleados);

        sessionManager = new SessionManager(this);

        recyclerView = findViewById(R.id.recyclerEmpleados);
        progressBar = findViewById(R.id.progressEmpleados);

        // Botón Volver
        ImageView btnVolver = findViewById(R.id.btnVolverLista);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        cargarEmpleados();
    }

    private void cargarEmpleados() {
        String token = "Bearer " + sessionManager.getAuthToken();

        RetrofitClient.getInstance().getMyApi().getEmpleados(token).enqueue(new Callback<List<TrabajadorResponse>>() {
            @Override
            public void onResponse(Call<List<TrabajadorResponse>> call, Response<List<TrabajadorResponse>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<TrabajadorResponse> empleados = response.body();

                    if (empleados.isEmpty()) {
                        Toast.makeText(AdminEmpleadosActivity.this, "La lista está vacía", Toast.LENGTH_SHORT).show();
                    } else {
                        // Usamos el adaptador con listener
                        EmpleadoAdapter adapter = new EmpleadoAdapter(empleados, empleado -> {
                            // Aquí programaremos ver el detalle/fichajes
                            Toast.makeText(AdminEmpleadosActivity.this, "Seleccionado: " + empleado.getNombre(), Toast.LENGTH_SHORT).show();
                        });
                        recyclerView.setAdapter(adapter);
                    }
                } else {
                    // Si falla aquí, el servidor devuelve error (400, 500...)
                    Toast.makeText(AdminEmpleadosActivity.this, "Error del servidor: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<TrabajadorResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminEmpleadosActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}