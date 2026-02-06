package com.example.trabajoapi.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.repository.AuthRepository;

public class LoginViewModel extends ViewModel {

    private final AuthRepository repo;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<LoginResponse> success = new MutableLiveData<>(null);

    public LoginViewModel(AuthRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<LoginResponse> getSuccess() { return success; }

    public void login(String nif, String password) {
        loading.setValue(true);
        error.setValue(null);

        repo.login(nif, password).observeForever(result -> {
            loading.postValue(false);

            if (result != null && result.isSuccess()) {
                success.postValue(result.data);
            } else {
                success.postValue(null);
                error.postValue(result != null ? result.error : "Error inesperado");
            }
        });
    }
}
