package com.example.trabajoapi.data;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    // Login
    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Fichar
    @POST("api/fichar")
    Call<FichajeResponse> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );

    // Historial
    @GET("api/mis-fichajes")
    Call<List<FichajeResponse>> obtenerHistorial(@Header("Authorization") String token);

    // Recuperar Password (Olvidada)
    @POST("api/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    // --- ESTE ES EL QUE TE FALTA Y DABA EL ERROR ---
    // Cambiar Password (Estando logueado)
    @POST("api/change-password")
    Call<Void> changePassword(
            @Header("Authorization") String token,
            @Body ChangePasswordRequest request
    );

    @POST("api/incidencias")
    Call<Void> crearIncidencia(
            @Header("Authorization") String token,
            @Body IncidenciaRequest request
    );
}