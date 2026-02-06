package com.example.trabajoapi.data.repository;

import androidx.annotation.NonNull;

import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.RetrofitClient;

import java.util.List;

import retrofit2.Call; // Importante: Algunos ViewModels usan Call directo
import retrofit2.Callback;

public class MainRepository {

    // --- Métodos Void (Callback directo) ---

    public void obtenerHistorial(@NonNull String bearer, @NonNull Callback<List<FichajeResponse>> cb) {
        RetrofitClient.getInstance().getMyApi().obtenerHistorial(bearer).enqueue(cb);
    }

    public void getResumen(@NonNull String bearer, Integer mes, Integer anio, @NonNull Callback<ResumenResponse> cb) {
        RetrofitClient.getInstance().getMyApi().getResumen(bearer, mes, anio).enqueue(cb);
    }

    public void fichar(@NonNull String bearer, @NonNull FichajeRequest req, @NonNull Callback<FichajeResponse> cb) {
        RetrofitClient.getInstance().getMyApi().fichar(bearer, req).enqueue(cb);
    }

    public void getRecordatorio(@NonNull String bearer, @NonNull Callback<RecordatorioResponse> cb) {
        RetrofitClient.getInstance().getMyApi().getRecordatorioFichaje(bearer).enqueue(cb);
    }

    public void changePassword(@NonNull String bearer, @NonNull ChangePasswordRequest req, @NonNull Callback<Void> cb) {
        RetrofitClient.getInstance().getMyApi().changePassword(bearer, req).enqueue(cb);
    }

    public Call<List<FichajeResponse>> obtenerHistorialCall(String token) {
        return RetrofitClient.getInstance().getMyApi().obtenerHistorial(token);
    }
}