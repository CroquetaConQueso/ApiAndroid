package com.example.trabajoapi.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private static final String PREF_NAME = "user_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROL = "rol"; // Nuevo

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public void saveRol(String rol) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ROL, rol);
        editor.apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getRol() {
        return prefs.getString(KEY_ROL, "Trabajador");
    }

    public boolean isAdmin() {
        String rol = getRol();
        return rol != null && (rol.equalsIgnoreCase("Administrador") || rol.equalsIgnoreCase("Admin"));
    }

    public void clearSession() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}