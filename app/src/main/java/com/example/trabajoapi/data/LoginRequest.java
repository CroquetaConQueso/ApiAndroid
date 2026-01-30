package com.example.trabajoapi.data;

public class LoginRequest {
    private String nif;
    private String password;

    public LoginRequest(String nif, String password) {
        this.nif = nif;
        this.password = password;
    }
}