package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.AuthRepository;
import com.example.trabajoapi.ui.login.LoginViewModel;
import com.example.trabajoapi.ui.login.LoginViewModelFactory;

public class LoginActivity extends AppCompatActivity {

    private EditText etNif, etPass;
    private AppCompatButton btnLogin;
    private TextView tvForgot;

    private SessionManager sessionManager;
    private LoginViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // OJO: estos IDs deben existir en tu activity_login.xml
        etNif = findViewById(R.id.etNifLogin);
        etPass = findViewById(R.id.etPassLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);

        AuthRepository repo = new AuthRepository(RetrofitClient.getInstance().getMyApi());
        vm = new ViewModelProvider(this, new LoginViewModelFactory(repo)).get(LoginViewModel.class);

        observarVM();

        btnLogin.setOnClickListener(v -> {
            // 1. Primero obtenemos los textos y quitamos espacios
            String nif = etNif.getText() != null ? etNif.getText().toString().trim() : "";
            String pass = etPass.getText() != null ? etPass.getText().toString().trim() : "";

            // 2. Validamos ANTES de bloquear el botón
            if (nif.isEmpty() || pass.isEmpty()) {
                mostrarToastPop("Por favor, introduce usuario y contraseña", false);
                return;
            }

            // 3. Si todo está bien, ahora sí bloqueamos y llamamos
            btnLogin.setEnabled(false);
            btnLogin.setText("...");
            vm.login(nif, pass);
        });

        if (tvForgot != null) {
            tvForgot.setOnClickListener(v -> mostrarDialogoResetPassword());
        }
    }

    private void observarVM() {
        vm.getLoading().observe(this, isLoading -> {
            // Esta lógica reactiva el botón cuando el servidor termina (sea éxito o error)
            boolean loading = isLoading != null && isLoading;
            btnLogin.setEnabled(!loading);
            if (!loading) btnLogin.setText("ENTRAR");
        });

        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String raw = e.getContentIfNotHandled();
            if (raw == null) return;

            boolean ok = raw.startsWith("OK:");
            String msg = raw;

            if (raw.startsWith("OK:") || raw.startsWith("ERROR:")) {
                msg = raw.substring(raw.indexOf(":") + 1).trim();
            }

            mostrarToastPop(msg, ok);
        });

        vm.getLoginOkEvent().observe(this, e -> {
            if (e == null) return;
            LoginResponse r = e.getContentIfNotHandled();
            if (r == null) return;

            sessionManager.saveSession(r);

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            if (r.getRecordatorio() != null && r.getRecordatorio().isAvisar()) {
                intent.putExtra("AVISO_TITULO", r.getRecordatorio().getTitulo());
                intent.putExtra("AVISO_MENSAJE", r.getRecordatorio().getMensaje());
            }

            startActivity(intent);
            finish();
        });
    }

    private void mostrarDialogoResetPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RECUPERAR CONTRASEÑA");

        EditText input = new EditText(this);
        input.setHint("Email");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(60, 40, 60, 40);
        builder.setView(input);

        builder.setPositiveButton("ENVIAR", (dialog, which) -> {
            String email = input.getText() != null ? input.getText().toString() : "";
            vm.resetPassword(email);
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void mostrarToastPop(String mensaje, boolean esExito) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.layout_toast_pop, null);

            TextView text = layout.findViewById(R.id.toastText);
            text.setText(mensaje);

            ImageView icon = layout.findViewById(R.id.toastIcon);
            icon.setImageResource(esExito ? R.drawable.ic_pop_success : R.drawable.ic_pop_error);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        }
    }
}