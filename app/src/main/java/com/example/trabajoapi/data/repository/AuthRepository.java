package com.example.trabajoapi.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.LoginRequest;
import com.example.trabajoapi.data.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService api;

    public AuthRepository(ApiService api) {
        this.api = api;
    }

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

    public static class Result<T> {
        public final T data;
        public final String error;

        private Result(T data, String error) {
            this.data = data;
            this.error = error;
        }

        public static <T> Result<T> success(T data) { return new Result<>(data, null); }
        public static <T> Result<T> error(String error) { return new Result<>(null, error); }

        public boolean isSuccess() { return data != null; }
    }
}
