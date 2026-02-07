package com.example.trabajoapi.ui.incidencia;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.IncidenciaRequest;
import com.example.trabajoapi.data.IncidenciaResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.IncidenciaRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidenciaViewModel extends ViewModel {

    private final IncidenciaRepository repo;

    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<List<IncidenciaResponse>>> historialEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> logoutEvent = new MutableLiveData<>();

    private Call<Void> crearCall;
    private Call<List<IncidenciaResponse>> histCall;

    public IncidenciaViewModel(IncidenciaRepository repo) {
        this.repo = repo;
    }

    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<List<IncidenciaResponse>>> getHistorialEvent() { return historialEvent; }
    public LiveData<Event<Boolean>> getLogoutEvent() { return logoutEvent; }

    public void crearIncidencia(String bearer, String tipo, String inicio, String fin, String comentario) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        if (crearCall != null) crearCall.cancel();
        crearCall = repo.crearIncidencia(bearer, new IncidenciaRequest(tipo, inicio, fin, comentario));

        crearCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    toastEvent.postValue(new Event<>("¡Solicitud Enviada!"));
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (call.isCanceled()) return;
                toastEvent.postValue(new Event<>("Error Red: " + (t.getMessage() != null ? t.getMessage() : "")));
            }
        });
    }

    public void cargarHistorial(String bearer) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        if (histCall != null) histCall.cancel();
        histCall = repo.getMisIncidencias(bearer);

        histCall.enqueue(new Callback<List<IncidenciaResponse>>() {
            @Override
            public void onResponse(Call<List<IncidenciaResponse>> call, Response<List<IncidenciaResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historialEvent.postValue(new Event<>(response.body()));
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<List<IncidenciaResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                toastEvent.postValue(new Event<>("Error Red: " + (t.getMessage() != null ? t.getMessage() : "")));
            }
        });
    }

    private void handleError(Response<?> response) {
        if (response.code() == 401) {
            toastEvent.postValue(new Event<>("Sesión caducada"));
            logoutEvent.postValue(new Event<>(true));
            return;
        }

        String msg = "Error " + response.code();
        try {
            if (response.errorBody() != null) {
                // Leemos el mensaje real del servidor (ej: "fecha_inicio invalid format")
                msg = "Server: " + response.errorBody().string();
            }
        } catch (Exception e) { }
        toastEvent.postValue(new Event<>(msg));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (crearCall != null) crearCall.cancel();
        if (histCall != null) histCall.cancel();
    }
}