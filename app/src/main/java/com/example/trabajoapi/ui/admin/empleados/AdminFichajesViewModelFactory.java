package com.example.trabajoapi.ui.admin.empleados;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.trabajoapi.data.repository.AdminRepository;

public class AdminFichajesViewModelFactory implements ViewModelProvider.Factory {
    private final AdminRepository repo;

    public AdminFichajesViewModelFactory(AdminRepository repo) {
        this.repo = repo;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AdminFichajesViewModel.class)) {
            return (T) new AdminFichajesViewModel(repo);
        }
        throw new IllegalArgumentException("Clase ViewModel desconocida");
    }
}