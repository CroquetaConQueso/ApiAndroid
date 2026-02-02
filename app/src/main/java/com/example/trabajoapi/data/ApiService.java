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

    // Fichar (Entrada/Salida)
    @POST("api/fichar")
    Call<FichajeResponse> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );

    // Historial Fichajes (Para saber si estoy dentro o fuera)
    // CORREGIDO: Usamos "mis-fichajes" que es la ruta correcta en tu Python
    @GET("api/mis-fichajes")
    Call<List<FichajeResponse>> obtenerHistorial(@Header("Authorization") String token);

    // Recuperar Password (Olvidada - Email)
    @POST("api/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    // Cambiar Password (Estando logueado)
    @POST("api/change-password")
    Call<Void> changePassword(
            @Header("Authorization") String token,
            @Body ChangePasswordRequest request
    );

    // Crear Incidencia (Solicitar vacaciones)
    @POST("api/incidencias")
    Call<Void> crearIncidencia(
            @Header("Authorization") String token,
            @Body IncidenciaRequest request
    );

    // Ver Incidencias (Historial de solicitudes)
    // NECESARIO: Sin esto, el IncidenciaHelper fallará
    @GET("api/incidencias")
    Call<List<IncidenciaResponse>> getMisIncidencias(@Header("Authorization") String token);
}