package com.example.trabajoapi.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences prefs;

    private static final String PREF_NAME = "user_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROL = "rol";

    // (Opcional pero útil)
    private static final String KEY_ID_TRABAJADOR = "id_trabajador";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_ID_EMPRESA = "id_empresa";

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void saveRol(String rol) {
        prefs.edit().putString(KEY_ROL, rol).apply();
    }

    public void saveIdTrabajador(int id) {
        prefs.edit().putInt(KEY_ID_TRABAJADOR, id).apply();
    }

    public void saveNombre(String nombre) {
        prefs.edit().putString(KEY_NOMBRE, nombre).apply();
    }

    public void saveIdEmpresa(int idEmpresa) {
        prefs.edit().putInt(KEY_ID_EMPRESA, idEmpresa).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    // Alias para compatibilidad con tu LoginActivity viejo
    public String getToken() {
        return getAuthToken();
    }

    public String getBearerToken() {
        String t = getAuthToken();
        return t == null ? null : "Bearer " + t;
    }

    public String getRol() {
        return prefs.getString(KEY_ROL, "Trabajador");
    }

    public int getIdTrabajador() {
        return prefs.getInt(KEY_ID_TRABAJADOR, -1);
    }

    public String getNombre() {
        return prefs.getString(KEY_NOMBRE, null);
    }

    public int getIdEmpresa() {
        return prefs.getInt(KEY_ID_EMPRESA, -1);
    }

    // Alias para compatibilidad
    public void saveSession(String token, String rol) {
        saveAuthToken(token);
        saveRol(rol);
    }


    public void saveSession(LoginResponse r) {
        if (r == null) return;
        if (r.getAccessToken() != null) saveAuthToken(r.getAccessToken());
        if (r.getRol() != null) saveRol(r.getRol());
        saveIdTrabajador(r.getIdTrabajador());
        saveNombre(r.getNombre());
        saveIdEmpresa(r.getIdEmpresa());
    }

    public boolean isAdmin() {
        String rol = getRol();
        if (rol == null) return false;
        String r = rol.toUpperCase();
        return r.contains("ADMIN") || r.contains("GERENTE") || r.contains("JEFE");
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
