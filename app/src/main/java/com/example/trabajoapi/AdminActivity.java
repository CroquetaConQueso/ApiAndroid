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

        // 1. Botón Volver (Flecha atrás)
        ImageView btnVolver = findViewById(R.id.btnVolverAdmin);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        // 2. Botón Empleados (Lista -> Historial)
        View btnEmpleados = findViewById(R.id.cardEmpleados);
        if (btnEmpleados != null) {
            btnEmpleados.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminEmpleadosActivity.class))
            );
        }

        // 3. Botón Mapa (Configurar Ubicación)
        View btnMapa = findViewById(R.id.cardMapa);
        if (btnMapa != null) {
            btnMapa.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminMapaActivity.class))
            );
        }

        // 4. NUEVO: Botón Configurar NFC (Torno Digital)
        // Este ID 'btnConfigNfc' lo definimos en el XML activity_admin.xml
        View btnConfigNfc = findViewById(R.id.btnConfigNfc);
        if (btnConfigNfc != null) {
            btnConfigNfc.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminNfcConfigActivity.class))
            );
        }
    }
}