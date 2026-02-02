package com.example.trabajoapi.data;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.trabajoapi.R; // Asegúrate de que este import sea correcto para tu paquete

import java.util.List;

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

    // --- MÉTODO 1: Mostrar Historial (GET) ---
    public void mostrarHistorial() {
        String token = "Bearer " + sessionManager.getAuthToken();
        Call<List<IncidenciaResponse>> call = apiService.getMisIncidencias(token);

        call.enqueue(new Callback<List<IncidenciaResponse>>() {
            @Override
            public void onResponse(Call<List<IncidenciaResponse>> call, Response<List<IncidenciaResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    construirDialogoHistorial(response.body());
                } else {
                    Toast.makeText(context, "Error al cargar historial", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<IncidenciaResponse>> call, Throwable t) {
                Toast.makeText(context, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
            // Simulación de borde y sombra estilo "Pop"
            card.setPadding(30, 30, 30, 30);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 40);
            card.setLayoutParams(params);

            // Icono y Estado
            String estadoIcono = "⏳ PENDIENTE";
            int colorFondo = Color.parseColor("#FEF9C3"); // Amarillo suave

            if ("APROBADA".equalsIgnoreCase(inc.getEstado())) {
                estadoIcono = "✅ APROBADA";
                colorFondo = Color.parseColor("#DCFCE7"); // Verde suave
            } else if ("RECHAZADA".equalsIgnoreCase(inc.getEstado())) {
                estadoIcono = "❌ RECHAZADA";
                colorFondo = Color.parseColor("#FEE2E2"); // Rojo suave
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

            // Separador negro simple
            View line = new View(context);
            line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4));
            line.setBackgroundColor(Color.BLACK);
            layout.addView(line);
            // Espacio extra
            View spacer = new View(context);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(1, 40));
            layout.addView(spacer);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("CERRAR", null);
        builder.show();
    }
}