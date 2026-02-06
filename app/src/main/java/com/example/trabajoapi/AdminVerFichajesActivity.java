package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.AdminRepository;
import com.example.trabajoapi.ui.admin.empleados.AdminFichajesViewModel;
import com.example.trabajoapi.ui.admin.empleados.AdminFichajesViewModelFactory;

public class AdminVerFichajesActivity extends AppCompatActivity {

    private AdminFichajesViewModel viewModel;
    private SessionManager sessionManager;
    private FichajeAdapter adapter;

    // UI Elements
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvNombreEmpleado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CAMBIO: Usamos el nuevo layout específico
        setContentView(R.layout.activity_admin_ver_fichajes);

        // 1. Recibir datos
        int idEmpleado = getIntent().getIntExtra("ID_EMPLEADO", -1);
        String nombre = getIntent().getStringExtra("NOMBRE_EMPLEADO");

        if (idEmpleado == -1) {
            finish();
            return;
        }

        // 2. Vincular Vistas
        sessionManager = new SessionManager(this);
        recyclerView = findViewById(R.id.recyclerFichajesAdmin);
        progressBar = findViewById(R.id.progressFichajesAdmin);
        tvEmpty = findViewById(R.id.tvVacioFichajes);
        tvNombreEmpleado = findViewById(R.id.tvNombreEmpleadoHistorial);
        ImageView btnVolver = findViewById(R.id.btnVolverHistorial);

        // Configurar cabecera y botón volver
        if (nombre != null) tvNombreEmpleado.setText(nombre);
        btnVolver.setOnClickListener(v -> finish());

        // Configurar Lista
        adapter = new FichajeAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 3. ViewModel
        AdminFichajesViewModelFactory factory = new AdminFichajesViewModelFactory(new AdminRepository());
        viewModel = new ViewModelProvider(this, factory).get(AdminFichajesViewModel.class);

        // 4. Observar
        observarViewModel();

        // 5. Cargar
        viewModel.cargarHistorial("Bearer " + sessionManager.getAuthToken(), idEmpleado);
    }

    private void observarViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                // Ocultamos la lista mientras carga para que se vea limpio
                if(isLoading) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getFichajes().observe(this, lista -> {
            // Cuando termina de cargar (loading false), mostramos lista o mensaje vacío
            if (lista != null && !lista.isEmpty()) {
                recyclerView.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
                adapter.setLista(lista);
            } else {
                recyclerView.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getToastEvent().observe(this, event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getGoLoginEvent().observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                sessionManager.clearSession();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });
    }
}