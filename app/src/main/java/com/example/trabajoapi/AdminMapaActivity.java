package com.example.trabajoapi;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager; // Necesario para OSM
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trabajoapi.data.EmpresaConfigResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.google.android.material.slider.Slider;

// --- IMPORTS DE OSM (Igual que en tu proyecto anterior) ---
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMapaActivity extends AppCompatActivity {

    private MapView map;
    private SessionManager sessionManager;

    // Variables de estado
    private GeoPoint ubicacionActual;
    private int radioActual = 50;

    private TextView tvRadioValor;
    private Marker markerEmpresa;
    private Polygon circuloRadio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. CONFIGURACIÓN OSM (Importante: antes de setContentView)
        // Esto estaba en tu MainActivity antigua y es obligatorio para que cargue el mapa
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_admin_mapa);

        sessionManager = new SessionManager(this);
        tvRadioValor = findViewById(R.id.tvRadioValor);

        // 2. INICIALIZAR MAPA
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK); // Estilo estándar
        map.setMultiTouchControls(true); // Zoom con dedos
        map.getController().setZoom(18.0);

        // 3. DETECTAR CLICS (Para mover la empresa)
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                moverEmpresa(p);
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                moverEmpresa(p);
                return true;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mReceive));

        // 4. CONTROLES UI
        Slider sliderRadio = findViewById(R.id.sliderRadio);
        sliderRadio.addOnChangeListener((slider, value, fromUser) -> {
            radioActual = (int) value;
            actualizarVisuales();
        });

        findViewById(R.id.btnGuardarMapa).setOnClickListener(v -> guardarCambiosApi());
        findViewById(R.id.btnVolverMapa).setOnClickListener(v -> finish());

        // 5. CARGAR DATOS DEL SERVIDOR
        cargarConfiguracionActual();
    }

    private void moverEmpresa(GeoPoint nuevoPunto) {
        ubicacionActual = nuevoPunto;
        actualizarVisuales();
    }

    private void cargarConfiguracionActual() {
        String token = "Bearer " + sessionManager.getAuthToken();
        RetrofitClient.getInstance().getMyApi().getEmpresaConfig(token).enqueue(new Callback<EmpresaConfigResponse>() {
            @Override
            public void onResponse(Call<EmpresaConfigResponse> call, Response<EmpresaConfigResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EmpresaConfigResponse config = response.body();

                    // Si la empresa ya tiene lat/lon guardada, la usamos
                    if (config.getLatitud() != null && config.getLongitud() != null) {
                        ubicacionActual = new GeoPoint(config.getLatitud(), config.getLongitud());
                        radioActual = config.getRadio();

                        // Actualizamos UI
                        ((Slider) findViewById(R.id.sliderRadio)).setValue(radioActual);
                        map.getController().setCenter(ubicacionActual);
                        actualizarVisuales();
                    } else {
                        // Si no tiene ubicación, centramos en un punto por defecto (ej: Madrid) o pedimos GPS
                        // Por simplicidad, ponemos un punto neutro si es null
                        Toast.makeText(AdminMapaActivity.this, "Sin ubicación previa. Toca el mapa.", Toast.LENGTH_LONG).show();
                        map.getController().setCenter(new GeoPoint(40.416775, -3.703790));
                    }
                }
            }
            @Override
            public void onFailure(Call<EmpresaConfigResponse> call, Throwable t) {
                Toast.makeText(AdminMapaActivity.this, "Error cargando datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarVisuales() {
        if (map == null || ubicacionActual == null) return;

        // Limpiamos marcadores y círculos anteriores
        if (markerEmpresa != null) map.getOverlays().remove(markerEmpresa);
        if (circuloRadio != null) map.getOverlays().remove(circuloRadio);

        // 1. DIBUJAR MARCADOR (Estilo de tu proyecto anterior)
        markerEmpresa = new Marker(map);
        markerEmpresa.setPosition(ubicacionActual);
        markerEmpresa.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        markerEmpresa.setTitle("Sede Empresa");
        map.getOverlays().add(markerEmpresa);

        // 2. DIBUJAR CÍRCULO (Radio)
        // OSM no tiene "Circle" nativo, así que dibujamos un polígono redondo
        List<GeoPoint> puntosCirculo = Polygon.pointsAsCircle(ubicacionActual, radioActual);
        circuloRadio = new Polygon();
        circuloRadio.setPoints(puntosCirculo);

        // Estilo Pop: Borde negro, relleno semitransparente
        circuloRadio.getOutlinePaint().setColor(Color.BLACK);
        circuloRadio.getOutlinePaint().setStrokeWidth(5f);
        circuloRadio.getFillPaint().setColor(Color.argb(70, 233, 30, 99)); // Rosa Pop

        map.getOverlays().add(0, circuloRadio); // Añadir al fondo (índice 0)

        // Actualizar texto
        tvRadioValor.setText(radioActual + " m");

        map.invalidate(); // Refrescar
    }

    private void guardarCambiosApi() {
        if (ubicacionActual == null) {
            Toast.makeText(this, "Selecciona una ubicación primero", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + sessionManager.getAuthToken();
        EmpresaConfigResponse config = new EmpresaConfigResponse();
        config.setLatitud(ubicacionActual.getLatitude());
        config.setLongitud(ubicacionActual.getLongitude());
        config.setRadio(radioActual);

        RetrofitClient.getInstance().getMyApi().updateEmpresaConfig(token, config).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminMapaActivity.this, "¡Configuración Guardada!", Toast.LENGTH_SHORT).show();
                    finish(); // Salir al menú
                } else {
                    Toast.makeText(AdminMapaActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminMapaActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Ciclo de vida del mapa (Importante para no consumir batería)
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