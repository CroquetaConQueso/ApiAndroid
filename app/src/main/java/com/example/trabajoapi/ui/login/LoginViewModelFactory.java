package com.example.trabajoapi.ui.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.AuthRepository;

public class LoginViewModelFactory implements ViewModelProvider.Factory {

    private final AuthRepository repo;

    public LoginViewModelFactory(AuthRepository repo) {
        this.repo = repo;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
