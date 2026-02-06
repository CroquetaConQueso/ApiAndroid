package com.example.trabajoapi.data.repository;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.EmpresaConfigResponse;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.TrabajadorResponse;

import java.util.List;

import retrofit2.Call;

public class AdminRepository {

    private final ApiService api;

    public AdminRepository() {
        this.api = RetrofitClient.getInstance().getMyApi();
    }

    public Call<List<TrabajadorResponse>> getEmpleados(String bearerToken) {
        return api.getEmpleados(bearerToken);
    }

    public Call<List<FichajeResponse>> getFichajesDeEmpleado(String token, int idEmpleado) {
        return RetrofitClient.getInstance().getMyApi().getFichajesEmpleado(token, idEmpleado);
    }
    public Call<List<FichajeResponse>> getFichajesEmpleado(String bearerToken, int idEmpleado) {
        return api.getFichajesEmpleado(bearerToken, idEmpleado);
    }

    public Call<EmpresaConfigResponse> getEmpresaConfig(String bearerToken) {
        return api.getEmpresaConfig(bearerToken);
    }

    public Call<Void> updateEmpresaConfig(String bearerToken, EmpresaConfigResponse config) {
        return api.updateEmpresaConfig(bearerToken, config);
    }
}
