package com.example.trabajoapi.data;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.trabajoapi.R;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidenciaHelper {

    private final Context context;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    public IncidenciaHelper(Context context, ApiService apiService, SessionManager sessionManager) {
        this.context = context;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    public void mostrarDialogoNuevaIncidencia() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("NUEVA INCIDENCIA");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Selector de Tipo
        final Spinner spinnerTipo = new Spinner(context);
        String[] tipos = {"VACACIONES", "BAJA", "ASUNTOS_PROPIOS", "HORAS_EXTRA", "OLVIDO"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, tipos);
        spinnerTipo.setAdapter(adapter);
        layout.addView(spinnerTipo);

        // Fechas
        final EditText etInicio = new EditText(context);
        etInicio.setHint("Fecha Inicio (Toca aquí)");
        etInicio.setFocusable(false);
        etInicio.setOnClickListener(v -> mostrarCalendario(etInicio));
        layout.addView(etInicio);

        final EditText etFin = new EditText(context);
        etFin.setHint("Fecha Fin (Toca aquí)");
        etFin.setFocusable(false);
        etFin.setOnClickListener(v -> mostrarCalendario(etFin));
        layout.addView(etFin);


        final EditText etComentario = new EditText(context);
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

        DatePickerDialog dpd = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            String fechaFormat = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            editText.setText(fechaFormat);
        }, anio, mes, dia);
        dpd.show();
    }

    private void enviarIncidenciaApi(String tipo, String inicio, String fin, String coment) {
        String token = "Bearer " + sessionManager.getAuthToken();
        IncidenciaRequest request = new IncidenciaRequest(tipo, inicio, fin, coment);

        Call<Void> call = apiService.crearIncidencia(token, request);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    mostrarToastPop("¡Solicitud Enviada!", true);
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String mensaje = "";

                        if (jsonObject.has("errors")) {
                            mensaje = "DETALLE: " + jsonObject.getJSONObject("errors").toString();
                        } else if (jsonObject.has("message")) {
                            mensaje = jsonObject.getString("message");
                        } else {
                            mensaje = "Error " + response.code();
                        }
                        mostrarToastPop(mensaje, false);

                    } catch (Exception e) {
                        mostrarToastPop("Error Servidor: " + response.code(), false);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mostrarToastPop("Error Red: " + t.getMessage(), false);
            }
        });
    }


    public void mostrarHistorial() {
        String token = "Bearer " + sessionManager.getAuthToken();
        Call<List<IncidenciaResponse>> call = apiService.getMisIncidencias(token);

        call.enqueue(new Callback<List<IncidenciaResponse>>() {
            @Override
            public void onResponse(Call<List<IncidenciaResponse>> call, Response<List<IncidenciaResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    construirDialogoHistorial(response.body());
                } else {
                    mostrarToastPop("Error al cargar historial", false);
                }
            }

            @Override
            public void onFailure(Call<List<IncidenciaResponse>> call, Throwable t) {
                mostrarToastPop("Error de red: " + t.getMessage(), false);
            }
        });
    }

    private void construirDialogoHistorial(List<IncidenciaResponse> lista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("MIS SOLICITUDES");

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        if (lista.isEmpty()) {
            TextView tv = new TextView(context);
            tv.setText("No tienes solicitudes recientes.");
            layout.addView(tv);
        }

        for (IncidenciaResponse inc : lista) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(30, 30, 30, 30);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 40);
            card.setLayoutParams(params);

            String estadoIcono = "⏳ PENDIENTE";
            int colorFondo = Color.parseColor("#FEF9C3");

            if ("APROBADA".equalsIgnoreCase(inc.getEstado())) {
                estadoIcono = "✅ APROBADA";
                colorFondo = Color.parseColor("#DCFCE7");
            } else if ("RECHAZADA".equalsIgnoreCase(inc.getEstado())) {
                estadoIcono = "❌ RECHAZADA";
                colorFondo = Color.parseColor("#FEE2E2");
            }

            TextView tvHeader = new TextView(context);
            tvHeader.setText(inc.getTipo() + "\n" + estadoIcono);
            tvHeader.setTextSize(16);
            tvHeader.setPadding(20, 20, 20, 20);
            tvHeader.setBackgroundColor(colorFondo);
            tvHeader.setTextColor(Color.BLACK);
            card.addView(tvHeader);

            TextView tvFechas = new TextView(context);
            tvFechas.setText("📅 " + inc.getFechaInicio() + " -> " + inc.getFechaFin());
            tvFechas.setTextColor(Color.DKGRAY);
            tvFechas.setPadding(10, 20, 10, 0);
            card.addView(tvFechas);

            if (inc.getComentarioAdmin() != null && !inc.getComentarioAdmin().isEmpty()) {
                TextView tvAdmin = new TextView(context);
                tvAdmin.setText("💬 Admin: " + inc.getComentarioAdmin());
                tvAdmin.setPadding(10, 10, 10, 10);
                tvAdmin.setTextColor(Color.BLACK);
                card.addView(tvAdmin);
            }

            layout.addView(card);

            View line = new View(context);
            line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4));
            line.setBackgroundColor(Color.BLACK);
            layout.addView(line);

            View spacer = new View(context);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(1, 40));
            layout.addView(spacer);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("CERRAR", null);
        builder.show();
    }

    private void mostrarToastPop(String mensaje, boolean esExito) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.layout_toast_pop, null);

            TextView text = layout.findViewById(R.id.toastText);
            text.setText(mensaje);

            ImageView icon = layout.findViewById(R.id.toastIcon);
            if (esExito) {
                icon.setImageResource(R.drawable.ic_pop_success);
            } else {
                icon.setImageResource(R.drawable.ic_pop_error);
            }

            Toast toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            // Fallback por si acaso falla el inflado
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();
        }
    }
}