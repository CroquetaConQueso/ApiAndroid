package com.example.trabajoapi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int PERMISSION_ID = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button btnFichar = findViewById(R.id.btnFichar);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnFichar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndFichar();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionManager.clearSession();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void checkPermissionsAndFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
        }
    }

    private void obtenerUbicacionYFichar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    enviarFichaje(location.getLatitude(), location.getLongitude());
                } else {
                    Toast.makeText(MainActivity.this, "Activa el GPS o abre Google Maps para refrescarlo", Toast.LENGTH_LONG).show();
                }
            }
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
                    Toast.makeText(MainActivity.this, "✅ " + tipo + " REGISTRADA", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FichajeResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYFichar();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}