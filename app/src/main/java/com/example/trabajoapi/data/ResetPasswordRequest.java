package com.example.trabajoapi.data;

public class ResetPasswordRequest {
    private String identificador;

    // Encapsula el email/identificador para enviarlo al endpoint de recuperación.
    public ResetPasswordRequest(String identificador) {
        this.identificador = identificador;
    }

    public String getIdentificador() {
        return identificador;
    }
}
