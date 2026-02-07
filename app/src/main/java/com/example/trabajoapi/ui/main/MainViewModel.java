package com.example.trabajoapi.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.NfcFichajeRequest; // Asegúrate de tener este import
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.MainRepository;

import org.json.JSONObject;

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

    // --- GETTERS ---
    public LiveData<Boolean> getDentro() { return dentro; }
    public LiveData<ResumenResponse> getResumen() { return resumen; }
    public LiveData<List<FichajeResponse>> getHistorial() { return historial; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<RecordatorioResponse>> getRecordatorioEvent() { return recordatorioEvent; }
    public LiveData<Event<Boolean>> getLogoutEvent() { return logoutEvent; }
    public LiveData<Event<List<FichajeResponse>>> getHistorialDialogEvent() { return historialDialogEvent; }

    // --- INICIALIZACIÓN ---
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
                toastEvent.postValue(new Event<>("📡 Sin conexión al servidor"));
            }
        });
    }

    public void obtenerHorasExtra(@NonNull String bearer) {
        repo.getResumen(bearer, null, null, new Callback<ResumenResponse>() {
            @Override
            public void onResponse(@NonNull Call<ResumenResponse> call, @NonNull Response<ResumenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resumen.postValue(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<ResumenResponse> call, @NonNull Throwable t) { }
        });
    }

    // --- FICHAJES ---

    // Fichaje Manual (Botón pantalla)
    public void fichar(@NonNull String bearer, double lat, double lon, String ignorarNfc) {
        FichajeRequest req = new FichajeRequest(lat, lon, null);
        repo.fichar(bearer, req, new Callback<FichajeResponse>() {
            @Override
            public void onResponse(@NonNull Call<FichajeResponse> call, @NonNull Response<FichajeResponse> response) {
                manejarRespuestaFichaje(response, bearer, "Manual");
            }
            @Override
            public void onFailure(@NonNull Call<FichajeResponse> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red: Revisa tu conexión"));
            }
        });
    }

    // Fichaje NFC (Tarjeta)
    public void realizarFichajeNfc(@NonNull String bearer, double lat, double lon, String nfcId) {
        repo.ficharPorNfc(bearer, lat, lon, nfcId, new Callback<FichajeResponse>() {
            @Override
            public void onResponse(@NonNull Call<FichajeResponse> call, @NonNull Response<FichajeResponse> response) {
                manejarRespuestaFichaje(response, bearer, "NFC");
            }
            @Override
            public void onFailure(@NonNull Call<FichajeResponse> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error conexión NFC"));
            }
        });
    }

    // --- GESTIÓN DE RESPUESTAS (Aquí está la magia) ---

    private void manejarRespuestaFichaje(Response<FichajeResponse> response, String bearer, String origen) {
        // 1. Caso NO AUTORIZADO (Token caducado)
        if (response.code() == 401) {
            toastEvent.postValue(new Event<>("Sesión caducada. Entra de nuevo."));
            logoutEvent.postValue(new Event<>(true));
            return;
        }

        // 2. Caso ÉXITO (200 OK)
        if (response.isSuccessful() && response.body() != null) {
            String tipo = response.body().getTipo();
            String mensajeExito = "ENTRADA".equalsIgnoreCase(tipo) ? "¡Bienvenido! Has entrado." : "¡Hasta luego! Has salido.";

            toastEvent.postValue(new Event<>(mensajeExito));

            boolean dentroNow = "ENTRADA".equalsIgnoreCase(tipo);
            dentro.postValue(dentroNow);
            obtenerHorasExtra(bearer);
            consultarEstadoFichaje(bearer);
        }
        // 3. Caso ERROR (403, 400, 500...)
        else {
            String mensajeAmigable = analizarErrorServer(response);
            toastEvent.postValue(new Event<>(mensajeAmigable));
        }
    }

    /**
     * Esta función traduce el JSON feo del servidor a un mensaje bonito para humanos.
     */
    private String analizarErrorServer(Response<?> response) {
        try {
            // 1. Obtener el JSON crudo del error
            String errorJson = response.errorBody() != null ? response.errorBody().string() : "";

            // 2. Intentar sacar el campo "message" o "status"
            String mensajeOriginal = "";
            try {
                JSONObject json = new JSONObject(errorJson);
                if (json.has("message")) mensajeOriginal = json.getString("message");
                else if (json.has("status")) mensajeOriginal = json.getString("status");
            } catch (Exception e) {
                // Si no es JSON, usamos el texto tal cual
                mensajeOriginal = errorJson;
            }

            // 3. DICCIONARIO DE TRADUCCIÓN (Palabras clave)
            String m = mensajeOriginal.toLowerCase();

            if (m.contains("lejos")) {
                // Intentamos sacar los metros si vienen en el mensaje
                String extra = "";
                if(mensajeOriginal.contains("(")) {
                    extra = " " + mensajeOriginal.substring(mensajeOriginal.indexOf("("));
                }
                return "📍 Estás demasiado lejos de la oficina" + extra;
            }

            if (m.contains("restringido") || m.contains("escanear el nfc")) {
                return "🔒 Acceso Bloqueado: Debes fichar en el torno de entrada.";
            }

            if (m.contains("nfc incorrecto") || m.contains("no válido")) {
                return "❌ Tarjeta desconocida. Usa tu credencial de empresa.";
            }

            if (m.contains("gps")) {
                return "🛰️ Activa el GPS de alta precisión para fichar.";
            }

            // Errores genéricos por código HTTP
            if (response.code() == 403) return "Acceso denegado.";
            if (response.code() == 404) return "Error: Endpoint no encontrado.";
            if (response.code() >= 500) return "Error del servidor. Inténtalo en 5 min.";

            // Si no sabemos qué es, mostramos el mensaje original limpio (o un genérico)
            return !mensajeOriginal.isEmpty() ? mensajeOriginal : "Error desconocido al fichar.";

        } catch (Exception e) {
            return "Error procesando la respuesta.";
        }
    }

    // --- OTROS MÉTODOS ---

    public void comprobarRecordatorio(@NonNull String bearer) {
        repo.getRecordatorio(bearer, new Callback<RecordatorioResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecordatorioResponse> call, @NonNull Response<RecordatorioResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RecordatorioResponse r = response.body();
                    if (r.isAvisar()) recordatorioEvent.postValue(new Event<>(r));
                }
            }
            @Override
            public void onFailure(@NonNull Call<RecordatorioResponse> call, @NonNull Throwable t) { }
        });
    }

    public void pedirHistorialParaDialogo(@NonNull String bearer) {
        repo.obtenerHistorial(bearer, new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FichajeResponse>> call, @NonNull Response<List<FichajeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historialDialogEvent.postValue(new Event<>(response.body()));
                } else {
                    toastEvent.postValue(new Event<>("No se pudo cargar historial"));
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<FichajeResponse>> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red"));
            }
        });
    }

    public void cambiarPassword(@NonNull String bearer, @NonNull String actual, @NonNull String nueva) {
        ChangePasswordRequest req = new ChangePasswordRequest(actual, nueva);
        repo.changePassword(bearer, req, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) toastEvent.postValue(new Event<>("Contraseña actualizada"));
                else toastEvent.postValue(new Event<>("Error al cambiar contraseña"));
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red"));
            }
        });
    }
}