package com.example.trabajoapi.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.AuthRepository;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final AuthRepository repo;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<LoginResponse>> loginOkEvent = new MutableLiveData<>();

    public LoginViewModel(AuthRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<LoginResponse>> getLoginOkEvent() { return loginOkEvent; }

    public void login(String nif, String password) {
        String n = nif != null ? nif.trim() : "";
        String p = password != null ? password.trim() : "";

        if (n.isEmpty() || p.isEmpty()) {
            toastEvent.setValue(new Event<>("ERROR: Rellena NIF y contraseña"));
            return;
        }

        loading.setValue(true);

        repo.login(n, p, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                loading.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();

                    // Tu modelo se llama getAccessToken(), no getToken()
                    String token = body.getAccessToken();
                    if (token == null || token.trim().isEmpty()) {
                        toastEvent.postValue(new Event<>("ERROR: Respuesta inválida (sin access_token)"));
                        return;
                    }

                    loginOkEvent.postValue(new Event<>(body));
                    return;
                }

                toastEvent.postValue(new Event<>(
                        parseErrorMessage(response.errorBody(), "ERROR: Credenciales incorrectas")
                ));
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loading.postValue(false);
                toastEvent.postValue(new Event<>("ERROR: Error de red: " + (t != null ? t.getMessage() : "")));
            }
        });
    }

    public void resetPassword(String email) {
        String e = email != null ? email.trim() : "";
        if (e.isEmpty() || !e.contains("@")) {
            toastEvent.setValue(new Event<>("ERROR: Email inválido"));
            return;
        }

        loading.setValue(true);

        repo.resetPassword(e, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                loading.postValue(false);

                if (response.isSuccessful()) {
                    toastEvent.postValue(new Event<>("OK: Si el correo existe, se enviará un email de recuperación."));
                    return;
                }

                toastEvent.postValue(new Event<>(
                        parseErrorMessage(response.errorBody(), "ERROR: No se pudo enviar el email")
                ));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                loading.postValue(false);
                toastEvent.postValue(new Event<>("ERROR: Error de red: " + (t != null ? t.getMessage() : "")));
            }
        });
    }

    private String parseErrorMessage(ResponseBody errorBody, String fallback) {
        if (errorBody == null) return fallback;
        try {
            String raw = errorBody.string();
            if (raw == null || raw.trim().isEmpty()) return fallback;

            try {
                JSONObject obj = new JSONObject(raw);
                if (obj.has("error")) return normalize(obj.optString("error", fallback), fallback);
                if (obj.has("message")) return normalize(obj.optString("message", fallback), fallback);
            } catch (Exception ignored) { }

            return normalize(raw, fallback);
        } catch (IOException e) {
            return fallback;
        }
    }

    private String normalize(String msg, String fallback) {
        if (msg == null) return fallback;
        String m = msg.trim();
        if (m.isEmpty()) return fallback;
        if (m.startsWith("OK:") || m.startsWith("ERROR:")) return m;
        return "ERROR: " + m;
    }
}
