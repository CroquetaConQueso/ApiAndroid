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
        ImageView btnVolver = findViewById(R.id.btnVolverLista);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Volver
        btnVolver.setOnClickListener(v -> finish());

        // Cargar datos
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
                    EmpleadoAdapter adapter = new EmpleadoAdapter(empleados);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(AdminEmpleadosActivity.this, "Error al cargar plantilla", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TrabajadorResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminEmpleadosActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}