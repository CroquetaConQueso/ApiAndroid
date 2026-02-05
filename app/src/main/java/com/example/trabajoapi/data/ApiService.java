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

    // Login de usuario
    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Recuperar Password (Olvidada - Email)
    @POST("api/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    // Cambiar Password (Estando logueado)
    @POST("api/change-password")
    Call<Void> changePassword(
            @Header("Authorization") String token,
            @Body ChangePasswordRequest request
    );

    // --- ZONA FICHAJES ---

    // Fichar (Entrada/Salida)
    @POST("api/fichar")
    Call<FichajeResponse> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );

    // Historial Fichajes (Para saber si estoy dentro o fuera y ver lista)
    @GET("api/mis-fichajes")
    Call<List<FichajeResponse>> obtenerHistorial(@Header("Authorization") String token);

    // Resumen Mensual de Horas (Cálculo de horas extra)
    @GET("api/resumen")
    Call<ResumenResponse> getResumen(
            @Header("Authorization") String token,
            @Query("mes") Integer mes,
            @Query("anio") Integer anio
    );

    // --- ZONA INCIDENCIAS ---

    // Crear Incidencia (Solicitar vacaciones/baja)
    @POST("api/incidencias")
    Call<Void> crearIncidencia(
            @Header("Authorization") String token,
            @Body IncidenciaRequest request
    );

    // Ver Incidencias (Historial de solicitudes)
    @GET("api/incidencias")
    Call<List<IncidenciaResponse>> getMisIncidencias(@Header("Authorization") String token);

    // --- ZONA NOTIFICACIONES ---

    // Guardar Token FCM (Firebase)
    @POST("api/save-fcm-token")
    Call<Void> saveFcmToken(
            @Header("Authorization") String token,
            @Body FcmTokenRequest request
    );

    // --- ZONA ADMINISTRADOR ---

    // Ver lista de todos los empleados
    @GET("api/empleados")
    Call<List<TrabajadorResponse>> getEmpleados(@Header("Authorization") String token);

    // Ver fichajes de un empleado específico
    @GET("api/fichajes-empleado/{id}")
    Call<List<FichajeResponse>> getFichajesEmpleado(
            @Header("Authorization") String token,
            @Path("id") int idEmpleado
    );

    // Obtener configuración de empresa (Lat, Lon, Radio)
    @GET("api/empresa/config")
    Call<EmpresaConfigResponse> getEmpresaConfig(@Header("Authorization") String token);

    // Modificar configuración de empresa
    @POST("api/empresa/config")
    Call<Void> updateEmpresaConfig(
            @Header("Authorization") String token,
            @Body EmpresaConfigResponse config
    );

    @GET("api/recordatorio-fichaje")
    Call<RecordatorioResponse> getRecordatorioFichaje(@Header("Authorization") String token);

}