package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.trabajoapi.data.SessionManager;

public class AdminActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        sessionManager = new SessionManager(this);

        ImageView btnVolver = findViewById(R.id.btnVolverAdmin);
        if (btnVolver != null) btnVolver.setOnClickListener(v -> finish());

        AppCompatButton btnEmpleados = findViewById(R.id.btnAdminEmpleados);
        AppCompatButton btnMapa = findViewById(R.id.btnAdminMapa);

        btnEmpleados.setOnClickListener(v -> startActivity(new Intent(this, AdminEmpleadosActivity.class)));
        btnMapa.setOnClickListener(v -> startActivity(new Intent(this, AdminMapaActivity.class)));
    }
}
