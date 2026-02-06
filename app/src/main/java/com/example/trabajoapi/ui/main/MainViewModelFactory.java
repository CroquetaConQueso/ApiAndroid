package com.example.trabajoapi.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.MainRepository;

public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final MainRepository repo;

    public MainViewModelFactory(MainRepository repo) {
        this.repo = repo;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
