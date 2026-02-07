package com.example.trabajoapi.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.LoginRequest;
import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AuthRepository
 * Gestiona solo Login con credenciales y recuperación de contraseña.
 */
public class AuthRepository {

    private final ApiService api;

    public AuthRepository(ApiService api) {
        this.api = api;
    }

    public void login(String nif, String password, Callback<LoginResponse> callback) {
        api.login(new LoginRequest(nif, password)).enqueue(callback);
    }

    public void resetPassword(String email, Callback<Void> callback) {
        api.resetPassword(new ResetPasswordRequest(email)).enqueue(callback);
    }

    // ---------------------------
    // LEGACY (por compatibilidad): LiveData<Result<...>>
    // ---------------------------

    public LiveData<Result<LoginResponse>> login(String nif, String password) {
        MutableLiveData<Result<LoginResponse>> live = new MutableLiveData<>();
        api.login(new LoginRequest(nif, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    live.postValue(Result.success(response.body()));
                } else {
                    live.postValue(Result.error("Credenciales incorrectas"));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                live.postValue(Result.error("Error de red: " + (t != null ? t.getMessage() : "")));
            }
        });
        return live;
    }

    public LiveData<Result<Void>> resetPassword(String email) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        api.resetPassword(new ResetPasswordRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    live.postValue(Result.success(null));
                } else {
                    live.postValue(Result.error("No se pudo enviar el email"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                live.postValue(Result.error("Error de red: " + (t != null ? t.getMessage() : "")));
            }
        });
        return live;
    }

    public static class Result<T> {
        public final T data;
        public final String error;

        private Result(T data, String error) {
            this.data = data;
            this.error = error;
        }

        public static <T> Result<T> success(T data) { return new Result<>(data, null); }
        public static <T> Result<T> error(String error) { return new Result<>(null, error); }

        public boolean isSuccess() { return error == null; }
    }
}