package com.example.trabajoapi.data;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/fichajes/fichar")
    Call<FichajeResponse> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );
}
