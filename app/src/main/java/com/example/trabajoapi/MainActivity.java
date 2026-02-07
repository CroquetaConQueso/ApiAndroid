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
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.IncidenciaHelper;
import com.example.trabajoapi.data.IncidenciaResponse;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.IncidenciaRepository;
import com.example.trabajoapi.data.repository.MainRepository;
import com.example.trabajoapi.ui.incidencia.IncidenciaViewModel;
import com.example.trabajoapi.ui.incidencia.IncidenciaViewModelFactory;
import com.example.trabajoapi.ui.main.MainViewModel;
import com.example.trabajoapi.ui.main.MainViewModelFactory;
import com.example.trabajoapi.work.TrabajadorRecordatorio;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 112;
    private static final int PERMISSION_ID = 44;

    private static final String WORK_UNIQUE_NAME = "recordatorio_fichaje_bg";

    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;

    private MaterialButton btnFicharMain;
    private TextView tvHorasExtraValor;
    private TextView tvEstadoHoras;

    private String avisoTituloPendiente = null;
    private String avisoMensajePendiente = null;

    private MainViewModel vm;

    // Incidencias (MVVM)
    private IncidenciaHelper incidenciaHelper;
    private IncidenciaViewModel ivm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ViewModels
        vm = new ViewModelProvider(
                this,
                new MainViewModelFactory(new MainRepository())
        ).get(MainViewModel.class);

        ivm = new ViewModelProvider(
                this,
                new IncidenciaViewModelFactory(new IncidenciaRepository())
        ).get(IncidenciaViewModel.class);

        // Helper UI-only (OJO: tu helper solo acepta Context)
        incidenciaHelper = new IncidenciaHelper(this);

        // UI refs
        btnFicharMain = findViewById(R.id.btnFicharMain);
        tvHorasExtraValor = findViewById(R.id.tvHorasExtraValor);
        tvEstadoHoras = findViewById(R.id.tvEstadoHoras);

        ImageView btnLogout = findViewById(R.id.btnLogoutIcon);
        AppCompatButton btnIncidencia = findViewById(R.id.btnIncidencia);
        AppCompatButton btnHistorial = findViewById(R.id.btnHistorial);
        AppCompatButton btnMisFichajes = findViewById(R.id.btnMisFichajes);
        AppCompatButton btnCambiarClave = findViewById(R.id.btnCambiarClave);
        AppCompatButton btnAdminPanel = findViewById(R.id.btnAdminPanel);

        // Fichar
        btnFicharMain.setOnClickListener(v -> {
            btnFicharMain.setEnabled(false);
            btnFicharMain.setText("...");
            checkPermissionsAndFichar();
        });

        // Incidencia: ahora requiere listener
        if (btnIncidencia != null) {
            btnIncidencia.setOnClickListener(v -> {
                incidenciaHelper.mostrarDialogoNuevaIncidencia((tipo, inicio, fin, comentario) -> {
                    String token = sessionManager.getAuthToken();
                    if (token == null) { irALogin(); return; }
                    ivm.crearIncidencia("Bearer " + token, tipo, inicio, fin, comentario);
                });
            });
        }

        // Historial incidencias: lo carga el VM y lo pinta el helper
        if (btnHistorial != null) {
            btnHistorial.setOnClickListener(v -> {
                String token = sessionManager.getAuthToken();
                if (token == null) { irALogin(); return; }
                ivm.cargarHistorial("Bearer " + token);
            });
        }

        // Mis fichajes
        if (btnMisFichajes != null) {
            btnMisFichajes.setOnClickListener(v -> {
                String token = sessionManager.getAuthToken();
                if (token == null) { irALogin(); return; }
                vm.pedirHistorialParaDialogo("Bearer " + token);
            });
        }

        // Cambiar clave
        if (btnCambiarClave != null) {
            btnCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                cancelRecordatorioWorker();
                sessionManager.clearSession();
                irALogin();
            });
        }

        // Admin panel
        if (btnAdminPanel != null) {
            if (sessionManager.isAdmin()) {
                btnAdminPanel.setVisibility(View.VISIBLE);
                btnAdminPanel.setOnClickListener(v ->
                        startActivity(new Intent(MainActivity.this, AdminActivity.class))
                );
            } else {
                btnAdminPanel.setVisibility(View.GONE);
            }
        }

        prepararAvisoLoginSiExiste();
        pedirPermisosNotificaciones();
        intentarMostrarAvisoPendiente();

        // Si hay sesión, programamos worker BG
        if (sessionManager.getAuthToken() != null) {
            scheduleRecordatorioWorker();
        }

        observarVM();
        observarIncidenciasVM();

        enviarTokenFCM();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String token = sessionManager.getAuthToken();
        if (token == null) {
            irALogin();
            return;
        }

        // Re-asegurar worker BG en caso de reinstalación/limpieza
        scheduleRecordatorioWorker();

        String bearer = "Bearer " + token;
        vm.cargarDashboard(bearer);

        // Esto es lo que quieres: recordatorio cada vez que abres la app
        vm.comprobarRecordatorio(bearer);
    }

    private void observarVM() {
        vm.getDentro().observe(this, this::actualizarBotonFichaje);

        vm.getResumen().observe(this, r -> {
            if (r == null) return;

            double saldo = r.getSaldo();
            if (saldo >= 0) {
                tvHorasExtraValor.setText("+" + saldo + " h");
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.pop_green));
                tvEstadoHoras.setText("TIENES HORAS EXTRA ACUMULADAS");
            } else {
                tvHorasExtraValor.setText(saldo + " h");
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.pop_red));
                tvEstadoHoras.setText("DEBES HORAS A LA EMPRESA");
            }
        });

        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg != null) {
                boolean ok = msg.contains("REGISTRADA") || msg.contains("Clave cambiada");
                mostrarToastPop(msg, ok);
            }
        });

        vm.getRecordatorioEvent().observe(this, e -> {
            if (e == null) return;
            RecordatorioResponse r = e.getContentIfNotHandled();
            if (r == null) return;

            if (tienePermisoNotificaciones()) {
                mostrarNotificacionLocal(r.getTitulo(), r.getMensaje());
            } else {
                avisoTituloPendiente = r.getTitulo();
                avisoMensajePendiente = r.getMensaje();
                pedirPermisosNotificaciones();
            }
        });

        vm.getHistorialDialogEvent().observe(this, e -> {
            if (e == null) return;
            List<FichajeResponse> lista = e.getContentIfNotHandled();
            if (lista == null) return;
            mostrarDialogoHistorialFichajes(lista);
        });

        vm.getLogoutEvent().observe(this, e -> {
            if (e == null) return;
            Boolean must = e.getContentIfNotHandled();
            if (must != null && must) irALogin();
        });
    }

    private void observarIncidenciasVM() {
        ivm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg != null) {
                boolean ok = msg.toUpperCase().contains("SOLICITUD") || msg.toUpperCase().contains("ENVIAD");
                incidenciaHelper.mostrarToastPop(msg, ok);
            }
        });

        ivm.getHistorialEvent().observe(this, e -> {
            if (e == null) return;
            List<IncidenciaResponse> lista = e.getContentIfNotHandled();
            if (lista != null) incidenciaHelper.mostrarDialogoHistorial(lista);
        });

        ivm.getLogoutEvent().observe(this, e -> {
            if (e == null) return;
            Boolean must = e.getContentIfNotHandled();
            if (must != null && must) irALogin();
        });
    }

    private void mostrarDialogoHistorialFichajes(List<FichajeResponse> lista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("MIS ÚLTIMOS FICHAJES");

        if (lista == null || lista.isEmpty()) {
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
    }

    private void actualizarBotonFichaje(boolean estoyDentro) {
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ID
            );
            btnFicharMain.setEnabled(true);
        }
    }

    private void obtenerUbicacionYFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        String token = sessionManager.getAuthToken();
                        if (token == null) { irALogin(); return; }

                        if (location != null) {
                            vm.fichar("Bearer " + token, location.getLatitude(), location.getLongitude());
                        } else {
                            mostrarToastPop("Activa el GPS", false);
                            vm.consultarEstadoFichaje("Bearer " + token);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarToastPop("Error GPS", false);
                    String token = sessionManager.getAuthToken();
                    if (token != null) vm.consultarEstadoFichaje("Bearer " + token);
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
        etNueva.setHint("Nueva (min 6 caracteres)");
        etNueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNueva);

        builder.setView(layout);

        builder.setPositiveButton("GUARDAR", (dialog, which) -> {
            String actual = etActual.getText().toString().trim();
            String nueva = etNueva.getText().toString().trim();

            if (actual.isEmpty() || nueva.isEmpty()) {
                mostrarToastPop("Completa ambos campos", false);
                return;
            }

            // VALIDACIÓN CLAVE: Evita el 422 del servidor
            if (nueva.length() < 6) {
                mostrarToastPop("La contraseña es muy corta (mínimo 6)", false);
                return;
            }

            String token = sessionManager.getAuthToken();
            if (token != null) vm.cambiarPassword("Bearer " + token, actual, nueva);
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
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
        cancelRecordatorioWorker();
        sessionManager.clearSession();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
                    if (!task.isSuccessful()) return;

                    String tokenFCM = task.getResult();
                    String authToken = sessionManager.getAuthToken();
                    if (authToken == null) return;

                    String bearer = "Bearer " + authToken;

                    com.example.trabajoapi.data.FcmTokenRequest request =
                            new com.example.trabajoapi.data.FcmTokenRequest(tokenFCM);

                    RetrofitClient.getInstance().getMyApi().saveFcmToken(bearer, request)
                            .enqueue(new Callback<Void>() {
                                @Override public void onResponse(Call<Void> call, Response<Void> response) { }
                                @Override public void onFailure(Call<Void> call, Throwable t) { }
                            });
                });
    }

    // ---------- WORKMANAGER BG RECORDATORIO ----------

    private void scheduleRecordatorioWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                TrabajadorRecordatorio.class,
                15, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WORK_UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );
    }

    private void cancelRecordatorioWorker() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_UNIQUE_NAME);
    }
}
