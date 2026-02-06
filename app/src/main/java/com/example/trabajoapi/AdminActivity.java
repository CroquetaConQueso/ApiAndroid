package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.trabajoapi.data.SessionManager;

public class AdminActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        sessionManager = new SessionManager(this);

        // Botón Volver (Flecha atrás)
        ImageView btnVolver = findViewById(R.id.btnVolverAdmin);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        // Botón Empleados (Lista -> Historial)
        View btnEmpleados = findViewById(R.id.cardEmpleados); // Ojo al ID del XML
        if (btnEmpleados != null) {
            btnEmpleados.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminEmpleadosActivity.class))
            );
        }

        // Botón Mapa
        View btnMapa = findViewById(R.id.cardMapa);
        if (btnMapa != null) {
            btnMapa.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminMapaActivity.class))
            );
        }
    }
}