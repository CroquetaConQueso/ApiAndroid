package com.example.trabajoapi;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.trabajoapi.data.EmpresaConfigResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SessionManager sessionManager;

    // Variables para guardar el estado
    private Marker markerEmpresa;
    private Circle radioCircle;
    private LatLng ubicacionActual;
    private int radioActual = 50; // Metros por defecto

    private TextView tvRadioValor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_mapa);

        sessionManager = new SessionManager(this);
        tvRadioValor = findViewById(R.id.tvRadioValor);

        // Configurar Slider de Radio
        Slider sliderRadio = findViewById(R.id.sliderRadio);
        sliderRadio.addOnChangeListener((slider, value, fromUser) -> {
            radioActual = (int) value;
            actualizarTextoYCirculo();
        });

        // Botón Guardar
        AppCompatButton btnGuardar = findViewById(R.id.btnGuardarMapa);
        btnGuardar.setOnClickListener(v -> guardarCambios());

        // Botón Volver
        findViewById(R.id.btnVolverMapa).setOnClickListener(v -> finish());

        // Cargar el Mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Listener para mover la empresa con CLICK LARGO
        mMap.setOnMapLongClickListener(latLng -> {
            ubicacionActual = latLng;
            dibujarEmpresa();
        });

        // Cargar datos del servidor
        cargarConfiguracionActual();
    }

    private void cargarConfiguracionActual() {
        String token = "Bearer " + sessionManager.getAuthToken();
        RetrofitClient.getInstance().getMyApi().getEmpresaConfig(token).enqueue(new Callback<EmpresaConfigResponse>() {
            @Override
            public void onResponse(Call<EmpresaConfigResponse> call, Response<EmpresaConfigResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EmpresaConfigResponse config = response.body();
                    if (config.getLatitud() != null && config.getLongitud() != null) {
                        ubicacionActual = new LatLng(config.getLatitud(), config.getLongitud());
                        radioActual = config.getRadio();

                        // Actualizar UI
                        Slider slider = findViewById(R.id.sliderRadio);
                        slider.setValue(radioActual);

                        // Dibujar en mapa
                        dibujarEmpresa();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual, 17));
                    }
                }
            }
            @Override
            public void onFailure(Call<EmpresaConfigResponse> call, Throwable t) {
                Toast.makeText(AdminMapaActivity.this, "Error cargando config", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dibujarEmpresa() {
        if (mMap == null || ubicacionActual == null) return;

        // Limpiar mapa previo
        mMap.clear();

        // 1. Poner Marcador
        markerEmpresa = mMap.addMarker(new MarkerOptions()
                .position(ubicacionActual)
                .title("Sede Empresa"));

        // 2. Dibujar Círculo de Radio
        // Color: Relleno semitransparente rosa/rojo, Borde negro fuerte (Estilo Pop)
        int strokeColor = Color.BLACK;
        int fillColor = Color.argb(70, 233, 30, 99); // Rosa semitransparente

        radioCircle = mMap.addCircle(new CircleOptions()
                .center(ubicacionActual)
                .radius(radioActual)
                .strokeWidth(5)
                .strokeColor(strokeColor)
                .fillColor(fillColor));

        actualizarTextoYCirculo();
    }

    private void actualizarTextoYCirculo() {
        tvRadioValor.setText(radioActual + " m");
        if (radioCircle != null) {
            radioCircle.setRadius(radioActual);
        }
    }

    private void guardarCambios() {
        if (ubicacionActual == null) return;

        String token = "Bearer " + sessionManager.getAuthToken();
        EmpresaConfigResponse config = new EmpresaConfigResponse();
        config.setLatitud(ubicacionActual.latitude);
        config.setLongitud(ubicacionActual.longitude);
        config.setRadio(radioActual);

        RetrofitClient.getInstance().getMyApi().updateEmpresaConfig(token, config).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminMapaActivity.this, "¡Ubicación actualizada!", Toast.LENGTH_SHORT).show();
                    finish(); // Volver al menú
                } else {
                    Toast.makeText(AdminMapaActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminMapaActivity.this, "Fallo de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}