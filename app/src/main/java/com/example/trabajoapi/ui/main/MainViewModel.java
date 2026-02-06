package com.example.trabajoapi.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.MainRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {

    private final MainRepository repo;

    private final MutableLiveData<Boolean> dentro = new MutableLiveData<>(false);
    private final MutableLiveData<ResumenResponse> resumen = new MutableLiveData<>();
    private final MutableLiveData<List<FichajeResponse>> historial = new MutableLiveData<>();

    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<RecordatorioResponse>> recordatorioEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> logoutEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<List<FichajeResponse>>> historialDialogEvent = new MutableLiveData<>();

    public MainViewModel(MainRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> getDentro() { return dentro; }
    public LiveData<ResumenResponse> getResumen() { return resumen; }
    public LiveData<List<FichajeResponse>> getHistorial() { return historial; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<RecordatorioResponse>> getRecordatorioEvent() { return recordatorioEvent; }
    public LiveData<Event<Boolean>> getLogoutEvent() { return logoutEvent; }
    public LiveData<Event<List<FichajeResponse>>> getHistorialDialogEvent() { return historialDialogEvent; }

    public void cargarDashboard(@NonNull String bearer) {
        consultarEstadoFichaje(bearer);
        obtenerHorasExtra(bearer);
    }

    public void consultarEstadoFichaje(@NonNull String bearer) {
        repo.obtenerHistorial(bearer, new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FichajeResponse>> call, @NonNull Response<List<FichajeResponse>> response) {
                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<FichajeResponse> lista = response.body();
                    historial.postValue(lista);

                    boolean dentroNow = !lista.isEmpty() && "ENTRADA".equalsIgnoreCase(lista.get(0).getTipo());
                    dentro.postValue(dentroNow);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FichajeResponse>> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de conexión"));
            }
        });
    }

    public void obtenerHorasExtra(@NonNull String bearer) {
        repo.getResumen(bearer, null, null, new Callback<ResumenResponse>() {
            @Override
            public void onResponse(@NonNull Call<ResumenResponse> call, @NonNull Response<ResumenResponse> response) {
                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    resumen.postValue(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResumenResponse> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de conexión"));
            }
        });
    }

    public void fichar(@NonNull String bearer, double lat, double lon) {
        FichajeRequest req = new FichajeRequest(lat, lon);

        repo.fichar(bearer, req, new Callback<FichajeResponse>() {
            @Override
            public void onResponse(@NonNull Call<FichajeResponse> call, @NonNull Response<FichajeResponse> response) {
                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    String tipo = response.body().getTipo();
                    toastEvent.postValue(new Event<>(tipo + " REGISTRADA"));

                    boolean dentroNow = "ENTRADA".equalsIgnoreCase(tipo);
                    dentro.postValue(dentroNow);

                    obtenerHorasExtra(bearer);
                    consultarEstadoFichaje(bearer);
                } else {
                    toastEvent.postValue(new Event<>("Fichaje rechazado (Lejos)"));
                    consultarEstadoFichaje(bearer);
                }
            }

            @Override
            public void onFailure(@NonNull Call<FichajeResponse> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red"));
                consultarEstadoFichaje(bearer);
            }
        });
    }

    public void comprobarRecordatorio(@NonNull String bearer) {
        repo.getRecordatorio(bearer, new Callback<RecordatorioResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecordatorioResponse> call, @NonNull Response<RecordatorioResponse> response) {
                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    RecordatorioResponse r = response.body();
                    if (r.isAvisar()) {
                        recordatorioEvent.postValue(new Event<>(r));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecordatorioResponse> call, @NonNull Throwable t) {
            }
        });
    }

    public void pedirHistorialParaDialogo(@NonNull String bearer) {
        repo.obtenerHistorial(bearer, new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FichajeResponse>> call, @NonNull Response<List<FichajeResponse>> response) {
                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<FichajeResponse> lista = response.body();
                    historial.postValue(lista);
                    historialDialogEvent.postValue(new Event<>(lista));
                } else {
                    toastEvent.postValue(new Event<>("Error al cargar historial"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FichajeResponse>> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de conexión"));
            }
        });
    }

    public void cambiarPassword(@NonNull String bearer, @NonNull String actual, @NonNull String nueva) {
        if (actual.trim().isEmpty() || nueva.trim().isEmpty()) return;

        ChangePasswordRequest req = new ChangePasswordRequest(actual, nueva);

        repo.changePassword(bearer, req, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }
                toastEvent.postValue(new Event<>(response.isSuccessful() ? "Clave cambiada" : "Error al cambiar"));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red"));
            }
        });
    }
}
