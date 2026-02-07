package com.example.trabajoapi.data.repository;

import androidx.annotation.NonNull;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.NfcFichajeRequest; // IMPORTANTE
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class MainRepository {

    private final ApiService api;

    public MainRepository() {
        this.api = RetrofitClient.getInstance().getMyApi();
    }

    public void obtenerHistorial(String token, Callback<List<FichajeResponse>> callback) {
        api.obtenerHistorial(token).enqueue(callback);
    }

    public void getResumen(String token, Integer mes, Integer anio, Callback<ResumenResponse> callback) {
        api.getResumen(token, mes, anio).enqueue(callback);
    }

    public void getRecordatorio(String token, Callback<RecordatorioResponse> callback) {
        api.getRecordatorioFichaje(token).enqueue(callback);
    }

    // Fichaje Manual (GPS normal)
    public void fichar(String token, FichajeRequest request, Callback<FichajeResponse> callback) {
        api.fichar(token, request).enqueue(callback);
    }

    // NUEVO: Fichaje por NFC (Endpoint dedicado)
    public void ficharPorNfc(String token, double lat, double lon, String nfcData, Callback<FichajeResponse> callback) {
        NfcFichajeRequest request = new NfcFichajeRequest(nfcData, lat, lon);
        api.ficharNfc(token, request).enqueue(callback);
    }

    public void changePassword(String token, ChangePasswordRequest request, Callback<Void> callback) {
        api.changePassword(token, request).enqueue(callback);
    }
}