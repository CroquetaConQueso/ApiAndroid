package com.example.trabajoapi.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<Event<LoginResponse>> loginOkEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private final AuthRepository authRepository;

    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Event<LoginResponse>> getLoginOkEvent() { return loginOkEvent; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Boolean> getLoading() { return loading; }

    public void login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            toastEvent.setValue(new Event<>("ERROR: Faltan datos"));
            return;
        }

        loading.setValue(true);

        authRepository.login(username, password, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    loginOkEvent.setValue(new Event<>(response.body()));
                } else {
                    // Intentar leer el mensaje de error del servidor
                    String errorMsg = "Credenciales incorrectas";
                    try {
                        if (response.errorBody() != null) {
                            // Asumimos que el backend devuelve algo como {"message": "..."}
                            // Si es texto plano, lo cogemos. Si es JSON complejo, aquí simplificamos.
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    toastEvent.setValue(new Event<>("ERROR: " + errorMsg));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loading.setValue(false);
                toastEvent.setValue(new Event<>("ERROR: Fallo de conexión"));
            }
        });
    }

    public void resetPassword(String email) {
        if (email == null || email.isEmpty()) {
            toastEvent.setValue(new Event<>("ERROR: Escribe tu email"));
            return;
        }

        loading.setValue(true);
        authRepository.resetPassword(email, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                loading.setValue(false);
                if (response.isSuccessful()) {
                    toastEvent.setValue(new Event<>("OK: Correo enviado (si existe)"));
                } else {
                    toastEvent.setValue(new Event<>("ERROR: No se pudo enviar"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                loading.setValue(false);
                toastEvent.setValue(new Event<>("ERROR: Fallo de red"));
            }
        });
    }
}