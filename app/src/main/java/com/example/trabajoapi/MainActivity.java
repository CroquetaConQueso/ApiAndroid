package com.example.trabajoapi;

import android.Manifest;
import android.app.DatePickerDialog; // Importante para Incidencias
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter; // Importante para el Spinner
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner; // Importante para el Spinner
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Tus clases de datos
import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.IncidenciaRequest; // Nueva
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.Calendar; // Para el calendario
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int PERMISSION_ID = 44;

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

        // Referencias a la UI
        btnFicharDinamico = findViewById(R.id.btnFicharDinamico);
        tvEstadoActual = findViewById(R.id.tvEstadoActual);

        Button btnLogout = findViewById(R.id.btnBannerLogout);
        Button btnCambiarClave = findViewById(R.id.btnCambiarClave);
        Button btnIncidencia = findViewById(R.id.btnIncidencia); // Botón de incidencias

        // 1. Listener FICHAR
        btnFicharDinamico.setOnClickListener(v -> {
            btnFicharDinamico.setEnabled(false);
            btnFicharDinamico.setText("Procesando...");
            Toast.makeText(MainActivity.this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();
            checkPermissionsAndFichar();
        });

        // 2. Listener LOGOUT
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                irALogin();
            });
        }

        // 3. Listener CAMBIAR CONTRASEÑA
        if (btnCambiarClave != null) {
            btnCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());
        }

        // 4. Listener SOLICITAR INCIDENCIA
        if (btnIncidencia != null) {
            btnIncidencia.setOnClickListener(v -> mostrarDialogoIncidencia());
        }
    }

    // ==========================================
    //       LÓGICA: SOLICITAR INCIDENCIA
    // ==========================================

    private void mostrarDialogoIncidencia() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NUEVA INCIDENCIA");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Selector de Tipo - AÑADIDO "OLVIDO"
        final Spinner spinnerTipo = new Spinner(this);
        String[] tipos = {"VACACIONES", "BAJA", "ASUNTOS_PROPIOS", "HORAS_EXTRA", "OLVIDO"}; // <--- AQUÍ
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tipos);
        spinnerTipo.setAdapter(adapter);
        layout.addView(spinnerTipo);

        // Fechas (Inputs de solo lectura que abren calendario)
        final EditText etInicio = new EditText(this);
        etInicio.setHint("Fecha Inicio (Toca aquí)");
        etInicio.setFocusable(false);
        etInicio.setOnClickListener(v -> mostrarCalendario(etInicio));
        layout.addView(etInicio);

        final EditText etFin = new EditText(this);
        etFin.setHint("Fecha Fin (Toca aquí)");
        etFin.setFocusable(false);
        etFin.setOnClickListener(v -> mostrarCalendario(etFin));
        layout.addView(etFin);

        // Comentario
        final EditText etComentario = new EditText(this);
        etComentario.setHint("Motivo (Opcional)");
        layout.addView(etComentario);

        builder.setView(layout);

        builder.setPositiveButton("ENVIAR", (dialog, which) -> {
            String tipo = spinnerTipo.getSelectedItem().toString();
            String inicio = etInicio.getText().toString();
            String fin = etFin.getText().toString();
            String comentario = etComentario.getText().toString();

            if (inicio.isEmpty() || fin.isEmpty()) {
                mostrarToastPop("Fechas obligatorias", false);
            } else {
                enviarIncidenciaApi(tipo, inicio, fin, comentario);
            }
        });

        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void mostrarCalendario(final EditText editText) {
        Calendar cal = Calendar.getInstance();
        int anio = cal.get(Calendar.YEAR);
        int mes = cal.get(Calendar.MONTH);
        int dia = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String fechaFormat = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            editText.setText(fechaFormat);
        }, anio, mes, dia);
        dpd.show();
    }

    private void enviarIncidenciaApi(String tipo, String inicio, String fin, String coment) {
        String token = "Bearer " + sessionManager.getAuthToken();
        IncidenciaRequest request = new IncidenciaRequest(tipo, inicio, fin, coment);

        Call<Void> call = RetrofitClient.getInstance().getMyApi().crearIncidencia(token, request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    mostrarToastPop("¡Solicitud Enviada!", true);
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorBody);

                        // LÓGICA DE DIAGNÓSTICO: MOSTRAR TODO EL JSON DE ERROR
                        String mensaje = "";

                        if (jsonObject.has("errors")) {
                            // Si hay errores de validación, mostramos el contenido completo
                            mensaje = "DETALLE: " + jsonObject.getJSONObject("errors").toString();
                        } else if (jsonObject.has("message")) {
                            mensaje = jsonObject.getString("message");
                        } else {
                            mensaje = "Error " + response.code();
                        }

                        // Mostramos el mensaje largo para poder leerlo
                        mostrarToastPop(mensaje, false);
                        // También lo imprimimos en el Logcat por si es muy largo
                        System.out.println("ERROR API: " + errorBody);

                    } catch (Exception e) {
                        mostrarToastPop("Error Servidor: " + response.code(), false);
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mostrarToastPop("Error Red: " + t.getMessage(), false);
            }
        });
    }


    // ==========================================
    //       LÓGICA: CAMBIAR CONTRASEÑA
    // ==========================================

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
                    // Lectura detallada del error
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String mensaje = jsonObject.optString("message");

                        if (mensaje.isEmpty() && jsonObject.has("errors")) {
                            mensaje = "Revisa los datos (mínimo 6 caracteres)";
                        }

                        if (mensaje.isEmpty()) mensaje = "Error al cambiar clave";

                        mostrarToastPop(mensaje, false);
                    } catch (Exception e) {
                        mostrarToastPop("Contraseña incorrecta o datos inválidos", false);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mostrarToastPop("Error de red", false);
            }
        });
    }


    // ==========================================
    //       LÓGICA: FICHAR Y ESTADO
    // ==========================================

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
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "{}";
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String mensaje = jsonObject.optString("message", "Error desconocido");
                        mostrarToastPop(mensaje, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mostrarToastPop("Error Servidor: " + response.code(), false);
                    }
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