package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class TrabajadorResponse implements Serializable {

    // Matches Python schema keys exactly
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

    // Use "rol_nombre" as defined in TrabajadorSchema in schemas.py
    @SerializedName("rol_nombre")
    private String rol;

    public TrabajadorResponse() {
    }

    public int getIdTrabajador() { return idTrabajador; }

    public String getNombre() { return nombre != null ? nombre : "Sin Nombre"; }

    public String getApellidos() { return apellidos != null ? apellidos : ""; }

    public String getNif() { return nif != null ? nif : "---"; }

    public String getEmail() { return email; }

    public String getRol() { return rol != null ? rol : "Trabajador"; }

    public String getNombreCompleto() { return getNombre() + " " + getApellidos(); }
}