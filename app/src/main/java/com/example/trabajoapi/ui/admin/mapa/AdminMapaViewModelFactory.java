package com.example.trabajoapi.ui.admin.mapa;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.AdminRepository;

public class AdminMapaViewModelFactory implements ViewModelProvider.Factory {

    private final AdminRepository repo;

    public AdminMapaViewModelFactory(AdminRepository repo) {
        this.repo = repo;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AdminMapaViewModel.class)) {
            return (T) new AdminMapaViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
