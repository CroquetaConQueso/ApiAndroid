package com.example.trabajoapi.data;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- ZONA AUTENTICACIÓN ---

    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    @POST("api/change-password")
    Call<Void> changePassword(
            @Header("Authorization") String token,
            @Body ChangePasswordRequest request
    );

    // --- ZONA FICHAJES ---

    // Este endpoint sirve para fichaje normal (GPS).
    // Si tu backend también acepta nfc_data opcional aquí, perfecto; no lo rompemos.
    @POST("api/fichar")
    Call<FichajeResponse> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );

    // NUEVO (recomendado): fichaje NFC en endpoint dedicado (JWT + UID como 2ª verificación)
    @POST("api/fichajes/fichar-nfc")
    Call<FichajeResponse> ficharNfc(
            @Header("Authorization") String token,
            @Body NfcFichajeRequest request
    );

    @GET("api/mis-fichajes")
    Call<List<FichajeResponse>> obtenerHistorial(@Header("Authorization") String token);

    @GET("api/resumen")
    Call<ResumenResponse> getResumen(
            @Header("Authorization") String token,
            @Query("mes") Integer mes,
            @Query("anio") Integer anio
    );

    // --- ZONA INCIDENCIAS ---

    @POST("api/incidencias")
    Call<Void> crearIncidencia(
            @Header("Authorization") String token,
            @Body IncidenciaRequest request
    );

    @GET("api/incidencias")
    Call<List<IncidenciaResponse>> getMisIncidencias(@Header("Authorization") String token);

    // --- ZONA NOTIFICACIONES ---

    @POST("api/save-fcm-token")
    Call<Void> saveFcmToken(
            @Header("Authorization") String token,
            @Body FcmTokenRequest request
    );

    // --- ZONA ADMINISTRADOR ---

    @GET("api/empleados")
    Call<List<TrabajadorResponse>> getEmpleados(@Header("Authorization") String token);

    @GET("api/fichajes-empleado/{id}")
    Call<List<FichajeResponse>> getFichajesEmpleado(
            @Header("Authorization") String token,
            @Path("id") int idEmpleado
    );

    @GET("api/empresa/config")
    Call<EmpresaConfigResponse> getEmpresaConfig(@Header("Authorization") String token);

    @POST("api/empresa/config")
    Call<Void> updateEmpresaConfig(
            @Header("Authorization") String token,
            @Body EmpresaConfigResponse config
    );

    @GET("api/recordatorio-fichaje")
    Call<RecordatorioResponse> getRecordatorioFichaje(@Header("Authorization") String token);
}
