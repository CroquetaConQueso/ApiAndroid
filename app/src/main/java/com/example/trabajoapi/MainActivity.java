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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.IncidenciaHelper;
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
    private MaterialButton btnFicharDinamico;
    private TextView tvEstadoActual;
    private boolean estaDentro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializamos el Helper pasando el contexto
        incidenciaHelper = new IncidenciaHelper(this, RetrofitClient.getInstance().getMyApi(), sessionManager);

        btnFicharDinamico = findViewById(R.id.btnFicharDinamico);
        tvEstadoActual = findViewById(R.id.tvEstadoActual);

        Button btnLogout = findViewById(R.id.btnBannerLogout);
        Button btnCambiarClave = findViewById(R.id.btnCambiarClave);
        Button btnIncidencia = findViewById(R.id.btnIncidencia);
        Button btnVerEstado = findViewById(R.id.btnVerEstado);

        // 1. Fichar
        btnFicharDinamico.setOnClickListener(v -> {
            btnFicharDinamico.setEnabled(false);
            btnFicharDinamico.setText("Procesando...");
            Toast.makeText(MainActivity.this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();
            checkPermissionsAndFichar();
        });

        // 2. Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                irALogin();
            });
        }

        // 3. Cambiar Clave
        if (btnCambiarClave != null) {
            btnCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());
        }

        // 4. Solicitar Incidencia (AHORA USAMOS EL HELPER)
        if (btnIncidencia != null) {
            btnIncidencia.setOnClickListener(v -> incidenciaHelper.mostrarDialogoNuevaIncidencia());
        }

        // 5. Ver Historial (AHORA USAMOS EL HELPER)
        if (btnVerEstado != null) {
            btnVerEstado.setOnClickListener(v -> incidenciaHelper.mostrarHistorial());
        }
    }


    private void mostrarDialogoCambioPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NUEVA CONTRASEÑA");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etActual = new EditText(this);
        etActual.setHint("Contraseña Actual");
        etActual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etActual);

        final EditText etNueva = new EditText(this);
        etNueva.setHint("Nueva Contraseña");
        etNueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNueva);

        builder.setView(layout);

        builder.setPositiveButton("GUARDAR", (dialog, which) -> {
            String actual = etActual.getText().toString().trim();
            String nueva = etNueva.getText().toString().trim();

            if (!actual.isEmpty() && !nueva.isEmpty()) {
                cambiarPasswordApi(actual, nueva);
            } else {
                mostrarToastPop("Completa los campos", false);
            }
        });

        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void cambiarPasswordApi(String actual, String nueva) {
        String token = "Bearer " + sessionManager.getAuthToken();
        ChangePasswordRequest request = new ChangePasswordRequest(actual, nueva);

        Call<Void> call = RetrofitClient.getInstance().getMyApi().changePassword(token, request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    mostrarToastPop("¡Contraseña cambiada!", true);
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String mensaje = jsonObject.optString("message");
                        if (mensaje.isEmpty()) mensaje = "Error al cambiar clave";
                        mostrarToastPop(mensaje, false);
                    } catch (Exception e) {
                        mostrarToastPop("Datos inválidos", false);
                    }
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mostrarToastPop("Error de red", false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        consultarEstadoServidor();
    }

    private void consultarEstadoServidor() {
        String token = "Bearer " + sessionManager.getAuthToken();
        Call<List<FichajeResponse>> call = RetrofitClient.getInstance().getMyApi().obtenerHistorial(token);

        call.enqueue(new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(Call<List<FichajeResponse>> call, Response<List<FichajeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FichajeResponse> historial = response.body();
                    if (!historial.isEmpty() && "ENTRADA".equalsIgnoreCase(historial.get(0).getTipo())) {
                        actualizarBoton(true);
                    } else {
                        actualizarBoton(false);
                    }
                } else if (response.code() == 401 || response.code() == 422) {
                    irALogin();
                }
            }
            @Override
            public void onFailure(Call<List<FichajeResponse>> call, Throwable t) {
                mostrarToastPop("Sin conexión: " + t.getMessage(), false);
                actualizarBoton(false);
            }
        });
    }

    private void actualizarBoton(boolean estoyDentro) {
        this.estaDentro = estoyDentro;
        btnFicharDinamico.setEnabled(true);

        if (estoyDentro) {
            btnFicharDinamico.setText("FICHAR\nSALIDA");
            btnFicharDinamico.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_fichaje_salida));
            btnFicharDinamico.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_blue)));
            btnFicharDinamico.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnFicharDinamico.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));

            tvEstadoActual.setText("TRABAJANDO");
            tvEstadoActual.setTextColor(ContextCompat.getColor(this, R.color.pop_blue));
            tvEstadoActual.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_pop_work, 0);
            tvEstadoActual.setCompoundDrawablePadding(16);
        } else {
            btnFicharDinamico.setText("FICHAR\nENTRADA");
            btnFicharDinamico.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_fichaje_entrada));
            btnFicharDinamico.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_green)));
            btnFicharDinamico.setTextColor(ContextCompat.getColor(this, R.color.pop_black));
            btnFicharDinamico.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_black)));

            tvEstadoActual.setText("JORNADA PAUSADA");
            tvEstadoActual.setTextColor(ContextCompat.getColor(this, R.color.pop_black));
            tvEstadoActual.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_pop_home, 0);
            tvEstadoActual.setCompoundDrawablePadding(16);
        }
    }

    private void checkPermissionsAndFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
            btnFicharDinamico.setEnabled(true);
            btnFicharDinamico.setText("REINTENTAR");
        }
    }

    private void obtenerUbicacionYFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            enviarFichaje(location.getLatitude(), location.getLongitude());
                        } else {
                            mostrarToastPop("GPS dormido. Abre Maps.", false);
                            consultarEstadoServidor();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarToastPop("Error GPS: " + e.getMessage(), false);
                    consultarEstadoServidor();
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
                    mostrarToastPop(tipo + " EXITOSA", true);
                    actualizarBoton(tipo.equalsIgnoreCase("ENTRADA"));
                } else if (response.code() == 401 || response.code() == 422) {
                    irALogin();
                } else {
                    mostrarToastPop("Error: Fichaje rechazado", false);
                    consultarEstadoServidor();
                }
            }
            @Override
            public void onFailure(Call<FichajeResponse> call, Throwable t) {
                mostrarToastPop("Error Red: " + t.getMessage(), false);
                consultarEstadoServidor();
            }
        });
    }

    private void mostrarToastPop(String mensaje, boolean esExito) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.layout_toast_pop, null);

        TextView text = layout.findViewById(R.id.toastText);
        text.setText(mensaje);

        ImageView icon = layout.findViewById(R.id.toastIcon);
        if (esExito) {
            icon.setImageResource(R.drawable.ic_pop_success);
        } else {
            icon.setImageResource(R.drawable.ic_pop_error);
        }

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    private void irALogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            mostrarToastPop("Permiso necesario", false);
            btnFicharDinamico.setEnabled(true);
            btnFicharDinamico.setText("NECESITA PERMISO");
        }
    }
}