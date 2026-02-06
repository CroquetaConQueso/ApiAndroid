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
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 112; // Notificaciones (Android 13+)
    private static final int PERMISSION_ID = 44;            // GPS

    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;
    private IncidenciaHelper incidenciaHelper;

    private MaterialButton btnFicharMain;
    private TextView tvHorasExtraValor;
    private TextView tvEstadoHoras;
    private boolean estaDentro = false;

    // Aviso pendiente (si llega desde Login)
    private String avisoTituloPendiente = null;
    private String avisoMensajePendiente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        incidenciaHelper = new IncidenciaHelper(this, RetrofitClient.getInstance().getMyApi(), sessionManager);

        btnFicharMain = findViewById(R.id.btnFicharMain);
        tvHorasExtraValor = findViewById(R.id.tvHorasExtraValor);
        tvEstadoHoras = findViewById(R.id.tvEstadoHoras);

        ImageView btnLogout = findViewById(R.id.btnLogoutIcon);
        AppCompatButton btnIncidencia = findViewById(R.id.btnIncidencia);
        AppCompatButton btnHistorial = findViewById(R.id.btnHistorial);
        AppCompatButton btnMisFichajes = findViewById(R.id.btnMisFichajes);
        AppCompatButton btnCambiarClave = findViewById(R.id.btnCambiarClave);
        AppCompatButton btnAdminPanel = findViewById(R.id.btnAdminPanel);

        btnFicharMain.setOnClickListener(v -> {
            btnFicharMain.setEnabled(false);
            btnFicharMain.setText("...");
            checkPermissionsAndFichar();
        });

        if (btnIncidencia != null) btnIncidencia.setOnClickListener(v -> incidenciaHelper.mostrarDialogoNuevaIncidencia());
        if (btnHistorial != null) btnHistorial.setOnClickListener(v -> incidenciaHelper.mostrarHistorial());
        if (btnMisFichajes != null) btnMisFichajes.setOnClickListener(v -> mostrarHistorialFichajes());
        if (btnCambiarClave != null) btnCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                irALogin();
            });
        }

        if (btnAdminPanel != null) {
            if (sessionManager.isAdmin()) {
                btnAdminPanel.setVisibility(View.VISIBLE);
                btnAdminPanel.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AdminActivity.class)));
            } else {
                btnAdminPanel.setVisibility(View.GONE);
            }
        }

        // 1) Capturamos aviso que venga del Login (si existe)
        prepararAvisoLoginSiExiste();

        // 2) Pedimos permiso notificaciones (Android 13+)
        pedirPermisosNotificaciones();

        // 3) Si ya hay permiso, mostramos el aviso pendiente
        intentarMostrarAvisoPendiente();

        enviarTokenFCM();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDashboard();
        comprobarRecordatorioFichaje();
    }

    private void comprobarRecordatorioFichaje() {
        String token = sessionManager.getAuthToken();
        if (token == null) return;

        RetrofitClient.getInstance().getMyApi()
                .getRecordatorioFichaje("Bearer " + token)
                .enqueue(new Callback<RecordatorioResponse>() {
                    @Override
                    public void onResponse(Call<RecordatorioResponse> call, Response<RecordatorioResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            RecordatorioResponse aviso = response.body();
                            if (aviso.isAvisar()) {
                                if (tienePermisoNotificaciones()) {
                                    mostrarNotificacionLocal(aviso.getTitulo(), aviso.getMensaje());
                                } else {
                                    avisoTituloPendiente = aviso.getTitulo();
                                    avisoMensajePendiente = aviso.getMensaje();
                                    pedirPermisosNotificaciones();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<RecordatorioResponse> call, Throwable t) {
                        // Silencioso
                    }
                });
    }

    private boolean tienePermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void pedirPermisosNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!tienePermisoNotificaciones()) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    private void prepararAvisoLoginSiExiste() {
        Intent i = getIntent();
        if (i == null) return;

        String titulo = i.getStringExtra("AVISO_TITULO");
        String mensaje = i.getStringExtra("AVISO_MENSAJE");

        if (titulo != null && mensaje != null) {
            avisoTituloPendiente = titulo;
            avisoMensajePendiente = mensaje;

            i.removeExtra("AVISO_TITULO");
            i.removeExtra("AVISO_MENSAJE");
            setIntent(i);
        }
    }

    private void intentarMostrarAvisoPendiente() {
        if (avisoTituloPendiente == null || avisoMensajePendiente == null) return;

        if (tienePermisoNotificaciones()) {
            mostrarNotificacionLocal(avisoTituloPendiente, avisoMensajePendiente);
            avisoTituloPendiente = null;
            avisoMensajePendiente = null;
        }
    }

    private void mostrarNotificacionLocal(String titulo, String cuerpo) {
        String channelId = "canal_fichajes_local_v1";
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "Avisos de Fichaje (Local)",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(titulo != null ? titulo : "Aviso")
                        .setContentText(cuerpo != null ? cuerpo : "")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(101, builder.build());
    }

    private void mostrarHistorialFichajes() {
        String token = "Bearer " + sessionManager.getAuthToken();
        Call<List<FichajeResponse>> call = RetrofitClient.getInstance().getMyApi().obtenerHistorial(token);

        call.enqueue(new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(Call<List<FichajeResponse>> call, Response<List<FichajeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FichajeResponse> lista = response.body();

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("MIS ÚLTIMOS FICHAJES");

                    if (lista.isEmpty()) {
                        builder.setMessage("No tienes registros de fichaje aún.");
                    } else {
                        String[] items = new String[lista.size()];
                        for (int i = 0; i < lista.size(); i++) {
                            FichajeResponse f = lista.get(i);

                            String rawFecha = f.getFechaHora();
                            String fechaLimpia = "Sin fecha";
                            if (rawFecha != null) {
                                fechaLimpia = rawFecha.replace("T", " ");
                                if (fechaLimpia.length() > 16) fechaLimpia = fechaLimpia.substring(0, 16);
                            }

                            items[i] = (f.getTipo() != null ? f.getTipo() : "REGISTRO") + "\n" + fechaLimpia;
                        }
                        builder.setItems(items, null);
                    }

                    builder.setPositiveButton("CERRAR", null);
                    builder.show();
                } else {
                    mostrarToastPop("Error al cargar historial", false);
                }
            }

            @Override
            public void onFailure(Call<List<FichajeResponse>> call, Throwable t) {
                mostrarToastPop("Error de conexión", false);
            }
        });
    }

    private void cargarDashboard() {
        consultarEstadoFichaje();
        obtenerCalculoHorasExtra();
    }

    private void obtenerCalculoHorasExtra() {
        String token = "Bearer " + sessionManager.getAuthToken();
        Call<ResumenResponse> call = RetrofitClient.getInstance().getMyApi().getResumen(token, null, null);

        call.enqueue(new Callback<ResumenResponse>() {
            @Override
            public void onResponse(Call<ResumenResponse> call, Response<ResumenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ResumenResponse r = response.body();
                    double saldo = r.getSaldo();

                    if (saldo >= 0) {
                        tvHorasExtraValor.setText("+" + saldo + " h");
                        tvHorasExtraValor.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.pop_green));
                        tvEstadoHoras.setText("TIENES HORAS EXTRA ACUMULADAS");
                    } else {
                        tvHorasExtraValor.setText(saldo + " h");
                        tvHorasExtraValor.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.pop_red));
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
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_pink)));
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            btnFicharMain.setText("FICHAR\nENTRADA");
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_green)));
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }

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
                    obtenerCalculoHorasExtra();
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
        builder.setPositiveButton("GUARDAR", (dialog, which) ->
                cambiarPasswordApi(etActual.getText().toString(), etNueva.getText().toString())
        );
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void cambiarPasswordApi(String actual, String nueva) {
        if (actual.isEmpty() || nueva.isEmpty()) return;

        String token = "Bearer " + sessionManager.getAuthToken();
        ChangePasswordRequest request = new ChangePasswordRequest(actual, nueva);

        RetrofitClient.getInstance().getMyApi().changePassword(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                mostrarToastPop(response.isSuccessful() ? "Clave cambiada" : "Error al cambiar", response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) { }
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

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYFichar();
            } else {
                btnFicharMain.setEnabled(true);
            }
            return;
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                intentarMostrarAvisoPendiente();
            }
        }
    }

    private void enviarTokenFCM() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        System.out.println("Fetching FCM registration token failed");
                        return;
                    }

                    String tokenFCM = task.getResult();
                    System.out.println("Token FCM: " + tokenFCM);

                    String authToken = "Bearer " + sessionManager.getAuthToken();
                    com.example.trabajoapi.data.FcmTokenRequest request =
                            new com.example.trabajoapi.data.FcmTokenRequest(tokenFCM);

                    RetrofitClient.getInstance().getMyApi().saveFcmToken(authToken, request)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        System.out.println("Token FCM guardado en servidor correctamente.");
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    System.out.println("Error enviando token FCM: " + t.getMessage());
                                }
                            });
                });
    }
}
