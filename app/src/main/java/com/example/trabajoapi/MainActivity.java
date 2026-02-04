package com.example.trabajoapi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.IncidenciaHelper;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int PERMISSION_ID = 44;
    private IncidenciaHelper incidenciaHelper;

    // Elementos de UI
    private MaterialButton btnFicharMain;
    private TextView tvHorasExtraValor;
    private TextView tvEstadoHoras;
    private boolean estaDentro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        incidenciaHelper = new IncidenciaHelper(this, RetrofitClient.getInstance().getMyApi(), sessionManager);

        // Binding
        btnFicharMain = findViewById(R.id.btnFicharMain);
        tvHorasExtraValor = findViewById(R.id.tvHorasExtraValor);
        tvEstadoHoras = findViewById(R.id.tvEstadoHoras);

        ImageView btnLogout = findViewById(R.id.btnLogoutIcon);
        AppCompatButton btnIncidencia = findViewById(R.id.btnIncidencia);
        AppCompatButton btnHistorial = findViewById(R.id.btnHistorial);
        AppCompatButton btnCambiarClave = findViewById(R.id.btnCambiarClave);

        // 1. FICHAR
        btnFicharMain.setOnClickListener(v -> {
            btnFicharMain.setEnabled(false);
            btnFicharMain.setText("...");
            checkPermissionsAndFichar();
        });

        // 2. INCIDENCIAS (Helper)
        if (btnIncidencia != null) btnIncidencia.setOnClickListener(v -> incidenciaHelper.mostrarDialogoNuevaIncidencia());

        // 3. HISTORIAL (Helper)
        if (btnHistorial != null) btnHistorial.setOnClickListener(v -> incidenciaHelper.mostrarHistorial());

        // 4. CAMBIAR CLAVE
        if (btnCambiarClave != null) btnCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());

        // 5. LOGOUT
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                irALogin();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDashboard();
    }

    private void cargarDashboard() {
        consultarEstadoFichaje();
        obtenerCalculoHorasExtra();
    }

    // --- LÓGICA DE HORAS EXTRA (Requisito PDF) ---
    private void obtenerCalculoHorasExtra() {
        String token = "Bearer " + sessionManager.getAuthToken();
        // El backend calcula: Horas Trabajadas - Horas Teóricas
        Call<ResumenResponse> call = RetrofitClient.getInstance().getMyApi().getResumen(token, null, null);

        call.enqueue(new Callback<ResumenResponse>() {
            @Override
            public void onResponse(Call<ResumenResponse> call, Response<ResumenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ResumenResponse r = response.body();
                    double saldo = r.getSaldo();

                    if (saldo >= 0) {
                        tvHorasExtraValor.setText("+" + saldo + " h");
                        tvHorasExtraValor.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.pop_green)); // Verde
                        tvEstadoHoras.setText("TIENES HORAS EXTRA ACUMULADAS");
                    } else {
                        tvHorasExtraValor.setText(saldo + " h");
                        tvHorasExtraValor.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.pop_red)); // Rojo
                        tvEstadoHoras.setText("DEBES HORAS A LA EMPRESA");
                    }
                }
            }
            @Override
            public void onFailure(Call<ResumenResponse> call, Throwable t) {
                tvHorasExtraValor.setText("--");
                tvEstadoHoras.setText("Error de conexión");
            }
        });
    }

    // --- LÓGICA DE FICHAJE ---
    private void consultarEstadoFichaje() {
        String token = "Bearer " + sessionManager.getAuthToken();
        Call<List<FichajeResponse>> call = RetrofitClient.getInstance().getMyApi().obtenerHistorial(token);

        call.enqueue(new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(Call<List<FichajeResponse>> call, Response<List<FichajeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FichajeResponse> historial = response.body();
                    boolean dentro = !historial.isEmpty() && "ENTRADA".equalsIgnoreCase(historial.get(0).getTipo());
                    actualizarBotonFichaje(dentro);
                } else if (response.code() == 401) {
                    irALogin();
                }
            }
            @Override
            public void onFailure(Call<List<FichajeResponse>> call, Throwable t) {
                btnFicharMain.setEnabled(true);
            }
        });
    }

    private void actualizarBotonFichaje(boolean estoyDentro) {
        this.estaDentro = estoyDentro;
        btnFicharMain.setEnabled(true);

        if (estoyDentro) {
            btnFicharMain.setText("FICHAR\nSALIDA");
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_pink))); // Rojo/Rosa Salida
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            btnFicharMain.setText("FICHAR\nENTRADA");
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_green))); // Verde Entrada
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }

    // --- PERMISOS Y GPS ---
    private void checkPermissionsAndFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
            btnFicharMain.setEnabled(true);
        }
    }

    private void obtenerUbicacionYFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            enviarFichaje(location.getLatitude(), location.getLongitude());
                        } else {
                            mostrarToastPop("Activa el GPS", false);
                            consultarEstadoFichaje();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarToastPop("Error GPS", false);
                    consultarEstadoFichaje();
                });
    }

    private void enviarFichaje(double lat, double lon) {
        String token = "Bearer " + sessionManager.getAuthToken();
        FichajeRequest request = new FichajeRequest(lat, lon);
        Call<FichajeResponse> call = RetrofitClient.getInstance().getMyApi().fichar(token, request);

        call.enqueue(new Callback<FichajeResponse>() {
            @Override
            public void onResponse(Call<FichajeResponse> call, Response<FichajeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String tipo = response.body().getTipo();
                    mostrarToastPop(tipo + " REGISTRADA", true);
                    actualizarBotonFichaje(tipo.equalsIgnoreCase("ENTRADA"));
                    obtenerCalculoHorasExtra(); // Recalcular saldo al fichar
                } else {
                    mostrarToastPop("Fichaje rechazado (Lejos)", false);
                    consultarEstadoFichaje();
                }
            }
            @Override
            public void onFailure(Call<FichajeResponse> call, Throwable t) {
                mostrarToastPop("Error de red", false);
                consultarEstadoFichaje();
            }
        });
    }

    // --- UTILIDADES ---
    private void mostrarDialogoCambioPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CAMBIAR CONTRASEÑA");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etActual = new EditText(this);
        etActual.setHint("Actual");
        etActual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etActual);

        final EditText etNueva = new EditText(this);
        etNueva.setHint("Nueva");
        etNueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNueva);

        builder.setView(layout);
        builder.setPositiveButton("GUARDAR", (dialog, which) -> cambiarPasswordApi(etActual.getText().toString(), etNueva.getText().toString()));
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void cambiarPasswordApi(String actual, String nueva) {
        if(actual.isEmpty() || nueva.isEmpty()) return;
        String token = "Bearer " + sessionManager.getAuthToken();
        ChangePasswordRequest request = new ChangePasswordRequest(actual, nueva);
        RetrofitClient.getInstance().getMyApi().changePassword(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                mostrarToastPop(response.isSuccessful() ? "Clave cambiada" : "Error al cambiar", response.isSuccessful());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void mostrarToastPop(String mensaje, boolean esExito) {
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
    }

    private void irALogin() {
        sessionManager.clearSession();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            btnFicharMain.setEnabled(true);
        }
    }
}