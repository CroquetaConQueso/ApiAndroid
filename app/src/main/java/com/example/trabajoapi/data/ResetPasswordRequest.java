package com.example.trabajoapi.data;

public class ResetPasswordRequest {
    private String identificador;

    public ResetPasswordRequest(String identificador) {
        this.identificador = identificador;
    }

    public String getIdentificador() {
        return identificador;
    }
}