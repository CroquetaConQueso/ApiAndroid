package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class TrabajadorResponse {

    @SerializedName("id_trabajador")
    private int idTrabajador;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("apellidos")
    private String apellidos;

    @SerializedName("nif")
    private String nif;

    @SerializedName("email")
    private String email;

    @SerializedName("rol")
    private String rol;

    // Constructor vacío
    public TrabajadorResponse() {
    }

    // Getters
    public int getIdTrabajador() {
        return idTrabajador;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getNif() {
        return nif;
    }

    public String getEmail() {
        return email;
    }

    public String getRol() {
        return rol;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}