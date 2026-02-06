package com.example.trabajoapi.data.repository;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.IncidenciaRequest;
import com.example.trabajoapi.data.IncidenciaResponse;
import com.example.trabajoapi.data.RetrofitClient;

import java.util.List;

import retrofit2.Call;

public class IncidenciaRepository {

    private final ApiService api;

    public IncidenciaRepository() {
        this.api = RetrofitClient.getInstance().getMyApi();
    }

    public Call<Void> crearIncidencia(String bearer, IncidenciaRequest request) {
        return api.crearIncidencia(bearer, request);
    }

    public Call<List<IncidenciaResponse>> getMisIncidencias(String bearer) {
        return api.getMisIncidencias(bearer);
    }
}
