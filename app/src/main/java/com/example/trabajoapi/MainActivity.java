package com.example.trabajoapi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
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

    // UI Elements del Nuevo XML
    private AppCompatButton btnFicharMain;
    private TextView tvSaldoHoras;
    private boolean estaDentro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ocultar Action Bar por defecto para usar nuestro diseño custom
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        incidenciaHelper = new IncidenciaHelper(this, RetrofitClient.getInstance().getMyApi(), sessionManager);

        // Binding UI con los nuevos IDs del XML rediseñado
        btnFicharMain = findViewById(R.id.btnFicharMain);
        tvSaldoHoras = findViewById(R.id.tvSaldoHoras);

        ImageView btnLogout = findViewById(R.id.btnLogoutIcon);
        TextView linkCambiarClave = findViewById(R.id.linkCambiarClave);

        AppCompatButton btnIncidencia = findViewById(R.id.btnIncidencia);
        AppCompatButton btnHistorial = findViewById(R.id.btnHistorial);
        Button btnVerDetalleHoras = findViewById(R.id.btnVerDetalleHoras);

        // 1. Acción Principal: FICHAR
        btnFicharMain.setOnClickListener(v -> {
            btnFicharMain.setEnabled(false);
            btnFicharMain.setText("...");
            checkPermissionsAndFichar();
        });

        // 2. Resumen Horas (Botón dentro de la tarjeta de saldo)
        if (btnVerDetalleHoras != null) {
            btnVerDetalleHoras.setOnClickListener(v -> mostrarResumenMensual(true));
        }

        // 3. Incidencias (Color AMARILLO - Warning)
        if (btnIncidencia != null) {
            // Personalizamos color si queremos diferenciarlo del historial
            btnIncidencia.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_yellow)));
            btnIncidencia.setOnClickListener(v -> incidenciaHelper.mostrarDialogoNuevaIncidencia());
        }

        // 4. Historial (Color BLANCO/NEUTRO)
        if (btnHistorial != null) {
            btnHistorial.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            btnHistorial.setOnClickListener(v -> incidenciaHelper.mostrarHistorial());
        }

        // 5. Utilidades (Logout y Clave)
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                irALogin();
            });
        }
        if (linkCambiarClave != null) {
            linkCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());
        }

        // Carga inicial de datos al abrir
        cargarDatosDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosDashboard();
    }

    private void cargarDatosDashboard() {
        consultarEstadoFichaje();
        mostrarResumenMensual(false); // false = solo actualizar texto saldo, no mostrar popup
    }

    // --- LÓGICA DE INTERFAZ DINÁMICA ---

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
                // En caso de error, dejamos el estado previo pero habilitamos el botón
                btnFicharDinamicoEnabled(true);
            }
        });
    }

    private void actualizarBotonFichaje(boolean estoyDentro) {
        this.estaDentro = estoyDentro;
        btnFicharMain.setEnabled(true);

        if (estoyDentro) {
            btnFicharMain.setText("FICHAR\nSALIDA");
            // Color ROJO/ROSA para salir (Estilo Pop Danger)
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_pink)));
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            btnFicharMain.setText("FICHAR\nENTRADA");
            // Color VERDE para entrar (Estilo Pop Success)
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_green)));
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }

    private void btnFicharDinamicoEnabled(boolean enabled) {
        if(btnFicharMain != null) btnFicharMain.setEnabled(enabled);
    }

    // --- LÓGICA RESUMEN HORAS (Actualiza tarjeta y Popup) ---

    private void mostrarResumenMensual(boolean mostrarPopup) {
        String token = "Bearer " + sessionManager.getAuthToken();
        Call<ResumenResponse> call = RetrofitClient.getInstance().getMyApi().getResumen(token, null, null);

        call.enqueue(new Callback<ResumenResponse>() {
            @Override
            public void onResponse(Call<ResumenResponse> call, Response<ResumenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ResumenResponse r = response.body();

                    // Actualizar Tarjeta Principal (Dashboard)
                    String signo = r.getSaldo() >= 0 ? "+" : "";
                    tvSaldoHoras.setText(signo + r.getSaldo() + " h");

                    // Cambiar color del texto del saldo según sea positivo o negativo
                    int colorSaldo = r.getSaldo() >= 0 ? R.color.pop_green : R.color.pop_red;
                    //tvSaldoHoras.setTextColor(ContextCompat.getColor(MainActivity.this, colorSaldo)); // Opcional

                    if (mostrarPopup) {
                        mostrarPopupDetalle(r);
                    }
                }
            }
            @Override
            public void onFailure(Call<ResumenResponse> call, Throwable t) { }
        });
    }

    private void mostrarPopupDetalle(ResumenResponse r) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("RESUMEN: " + r.getMes().toUpperCase());

        String colorHex = r.getSaldo() >= 0 ? "#2ed573" : "#ff4757";
        String htmlMessage = "<b>Teóricas:</b> " + r.getTeoricas() + "h<br>" +
                "<b>Trabajadas:</b> " + r.getTrabajadas() + "h<br><br>" +
                "<b>SALDO:</b> <font color='" + colorHex + "'>" + r.getSaldo() + "h</font>";

        builder.setMessage(Html.fromHtml(htmlMessage, Html.FROM_HTML_MODE_LEGACY));
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    // --- LÓGICA DE FICHAR (GPS) ---

    private void checkPermissionsAndFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
            btnFicharMain.setEnabled(true);
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
                            mostrarToastPop("GPS no disponible", false);
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
                } else {
                    mostrarToastPop("Fichaje rechazado (Distancia)", false);
                    consultarEstadoFichaje();
                }
            }
            @Override
            public void onFailure(Call<FichajeResponse> call, Throwable t) {
                mostrarToastPop("Error de conexión", false);
                consultarEstadoFichaje();
            }
        });
    }

    // --- UTILIDADES ---

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
        toast.setDuration(Toast.LENGTH_SHORT);
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

    private void mostrarDialogoCambioPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CAMBIAR CLAVE");

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

        builder.setPositiveButton("GUARDAR", (dialog, which) -> {
            cambiarPasswordApi(etActual.getText().toString().trim(), etNueva.getText().toString().trim());
        });
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
                mostrarToastPop(response.isSuccessful() ? "Clave actualizada" : "Error al cambiar clave", response.isSuccessful());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mostrarToastPop("Error de red", false);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            btnFicharMain.setEnabled(true);
            mostrarToastPop("Permiso GPS requerido", false);
        }
    }
}