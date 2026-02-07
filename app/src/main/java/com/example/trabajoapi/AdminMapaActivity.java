package com.example.trabajoapi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.AdminRepository;
import com.example.trabajoapi.ui.admin.mapa.AdminMapaViewModel;
import com.example.trabajoapi.ui.admin.mapa.AdminMapaViewModelFactory;
import com.google.android.material.slider.Slider;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.List;

public class AdminMapaActivity extends AppCompatActivity {

    private MapView map;
    private SessionManager sessionManager;
    private AdminMapaViewModel vm;

    private GeoPoint ubicacionActual;
    private int radioActual = 50;

    private TextView tvRadioValor;
    private Slider sliderRadio;

    private Marker markerEmpresa;
    private Polygon circuloRadio;

    // Permite elegir en el mapa la ubicación de la empresa y ajustar el radio de fichaje.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa la configuración de OSMdroid y fija el user agent de la app.
        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_admin_mapa);

        sessionManager = new SessionManager(this);

        // Conecta el VM para cargar y guardar la configuración de empresa.
        vm = new ViewModelProvider(
                this,
                new AdminMapaViewModelFactory(new AdminRepository())
        ).get(AdminMapaViewModel.class);

        tvRadioValor = findViewById(R.id.tvRadioValor);

        // Prepara el mapa con zoom y controles táctiles.
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(18.0);

        // Permite tocar el mapa para mover la ubicación seleccionada.
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { moverEmpresa(p); return true; }
            @Override public boolean longPressHelper(GeoPoint p) { moverEmpresa(p); return true; }
        };
        map.getOverlays().add(new MapEventsOverlay(mReceive));

        // Ajusta el radio y refresca los elementos dibujados.
        sliderRadio = findViewById(R.id.sliderRadio);
        sliderRadio.addOnChangeListener((slider, value, fromUser) -> {
            radioActual = (int) value;
            actualizarVisuales();
        });

        // Guarda la configuración actual si ya hay una ubicación seleccionada.
        findViewById(R.id.btnGuardarMapa).setOnClickListener(v -> {
            if (ubicacionActual == null) {
                mostrarToastPop("Selecciona una ubicación primero", false);
                return;
            }
            String token = sessionManager.getAuthToken();
            if (token == null) { irALogin(); return; }

            vm.guardarConfiguracion("Bearer " + token,
                    ubicacionActual.getLatitude(),
                    ubicacionActual.getLongitude(),
                    radioActual
            );
        });

        // Vuelve a la pantalla anterior sin modificar nada.
        findViewById(R.id.btnVolverMapa).setOnClickListener(v -> finish());

        // Engancha eventos del VM para reflejar configuración, mensajes y navegación.
        observarVM();

        String token = sessionManager.getAuthToken();
        if (token == null) { irALogin(); return; }

        // Carga la configuración existente para rellenar mapa y slider.
        vm.cargarConfiguracion("Bearer " + token);
    }

    // Reacciona a la configuración cargada, mensajes del VM y eventos de guardado.
    private void observarVM() {
        vm.getConfig().observe(this, cfg -> {
            if (cfg == null) return;

            // Si ya había ubicación, centra el mapa y pinta marcador/círculo.
            if (cfg.getLatitud() != null && cfg.getLongitud() != null) {
                ubicacionActual = new GeoPoint(cfg.getLatitud(), cfg.getLongitud());
                radioActual = cfg.getRadio();

                sliderRadio.setValue(radioActual);
                map.getController().setCenter(ubicacionActual);
                actualizarVisuales();
            } else {
                // Si no hay datos previos, deja una vista inicial y pide seleccionar punto.
                mostrarToastPop("Sin ubicación previa. Toca el mapa.", false);
                map.getController().setCenter(new GeoPoint(40.416775, -3.703790));
            }
        });

        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg != null) mostrarToastPop(msg, !msg.toUpperCase().contains("ERROR"));
        });

        vm.getGoLoginEvent().observe(this, e -> {
            if (e == null) return;
            Boolean go = e.getContentIfNotHandled();
            if (go != null && go) irALogin();
        });

        vm.getSavedEvent().observe(this, e -> {
            if (e == null) return;
            Boolean ok = e.getContentIfNotHandled();
            if (ok != null && ok) finish();
        });
    }

    // Cambia el punto seleccionado y repinta el marcador y el radio.
    private void moverEmpresa(GeoPoint nuevoPunto) {
        ubicacionActual = nuevoPunto;
        actualizarVisuales();
    }

    // Dibuja marcador y círculo del radio actual, y actualiza el texto del slider.
    private void actualizarVisuales() {
        if (map == null || ubicacionActual == null) return;

        if (markerEmpresa != null) map.getOverlays().remove(markerEmpresa);
        if (circuloRadio != null) map.getOverlays().remove(circuloRadio);

        markerEmpresa = new Marker(map);
        markerEmpresa.setPosition(ubicacionActual);
        markerEmpresa.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        markerEmpresa.setTitle("Sede Empresa");
        map.getOverlays().add(markerEmpresa);

        List<GeoPoint> puntosCirculo = Polygon.pointsAsCircle(ubicacionActual, radioActual);
        circuloRadio = new Polygon();
        circuloRadio.setPoints(puntosCirculo);
        circuloRadio.getOutlinePaint().setColor(Color.BLACK);
        circuloRadio.getOutlinePaint().setStrokeWidth(5f);
        circuloRadio.getFillPaint().setColor(Color.argb(70, 233, 30, 99));
        map.getOverlays().add(0, circuloRadio);

        tvRadioValor.setText(radioActual + " m");
        map.invalidate();
    }

    // Muestra el toast personalizado y, si falla el layout, usa el toast estándar.
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

    // Limpia la sesión y vuelve al login cuando el token ya no es válido.
    private void irALogin() {
        sessionManager.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Mantiene el ciclo de vida del mapa sincronizado con la Activity.
    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
