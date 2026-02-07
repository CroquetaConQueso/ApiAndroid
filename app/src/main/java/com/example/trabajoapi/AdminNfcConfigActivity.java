package com.example.trabajoapi;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.AdminRepository;
import com.example.trabajoapi.nfc.NfcFichajeController;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminNfcConfigActivity extends AppCompatActivity implements NfcFichajeController.Listener {

    private NfcFichajeController nfcController;
    private SessionManager sessionManager;
    private AdminRepository repository;

    private TextView tvEstado;
    private TextView tvCodigoLeido;
    private MaterialButton btnGuardar;
    private ImageView imgNfcIcon;

    private String codigoLeidoActual = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_nfc_config); // Creamos este layout abajo

        sessionManager = new SessionManager(this);
        repository = new AdminRepository();
        nfcController = new NfcFichajeController(this);

        tvEstado = findViewById(R.id.tvNfcConfigEstado);
        tvCodigoLeido = findViewById(R.id.tvNfcConfigCodigo);
        btnGuardar = findViewById(R.id.btnNfcConfigGuardar);
        imgNfcIcon = findViewById(R.id.imgNfcConfigIcon);

        btnGuardar.setEnabled(false);
        btnGuardar.setOnClickListener(v -> guardarNfc());

        findViewById(R.id.btnBackNfcConfig).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcController.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcController.onPause(this);
    }

    // --- NFC LISTENERS ---

    @Override
    public void onNfcReady(boolean enabled) {
        if (enabled) {
            tvEstado.setText("Acerca la etiqueta que será el 'NFC PRINCIPAL'...");
        } else {
            tvEstado.setText("NFC Desactivado. Actívalo en ajustes.");
        }
    }

    @Override
    public void onTagValida(String nfcId) {
        runOnUiThread(() -> {
            codigoLeidoActual = nfcId;
            tvCodigoLeido.setText("CÓDIGO: " + nfcId);
            tvEstado.setText("¡Etiqueta leída! Pulsa guardar.");
            btnGuardar.setEnabled(true);
            imgNfcIcon.setImageResource(R.drawable.ic_pop_check); // Asegúrate de tener este icono o usa otro
        });
    }

    @Override
    public void onTagInvalida(String motivo, String payload) {
        runOnUiThread(() -> {
            tvEstado.setText("Error leyendo etiqueta: " + motivo);
            imgNfcIcon.setImageResource(R.drawable.ic_pop_error);
        });
    }

    @Override
    public void onNfcError(String motivo) {
        runOnUiThread(() -> {
            tvEstado.setText("Error NFC: " + motivo);
        });
    }

    // --- GUARDAR EN API ---

    private void guardarNfc() {
        if (codigoLeidoActual == null) return;

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        String token = sessionManager.getAuthToken();
        repository.updateEmpresaNfc("Bearer " + token, codigoLeidoActual, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminNfcConfigActivity.this, "¡NFC Principal Configurado!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("GUARDAR CONFIGURACIÓN");
                    Toast.makeText(AdminNfcConfigActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("GUARDAR CONFIGURACIÓN");
                Toast.makeText(AdminNfcConfigActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}