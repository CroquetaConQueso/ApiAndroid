package com.example.trabajoapi.data;

public class LoginResponse {
    private String access_token;
    private String nif;
    private String rol;

    public String getAccessToken() {
        return access_token;
    }

    public String getNif() {
        return nif;
    }

    public String getRol() {
        return rol;
    }
}