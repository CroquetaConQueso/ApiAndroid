package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class TrabajadorResponse implements Serializable {

    // Aceptamos cualquier variante común para el ID
    @SerializedName(value = "id", alternate = {"id_trabajador", "user_id", "userId"})
    private int idTrabajador;

    // Aceptamos name/nombre/first_name
    @SerializedName(value = "name", alternate = {"nombre", "first_name", "firstname"})
    private String nombre;

    // Aceptamos surname/apellidos/last_name
    @SerializedName(value = "surname", alternate = {"apellidos", "last_name", "lastname"})
    private String apellidos;

    @SerializedName(value = "nif", alternate = {"dni", "document"})
    private String nif;

    @SerializedName(value = "email", alternate = {"mail", "correo"})
    private String email;

    // Aceptamos role/rol/type
    @SerializedName(value = "role", alternate = {"rol", "tipo", "type"})
    private String rol;

    public TrabajadorResponse() {
    }

    // Getters seguros (evitan nulos)
    public int getIdTrabajador() {
        return idTrabajador;
    }

    public String getNombre() {
        return nombre != null ? nombre : "Sin Nombre";
    }

    public String getApellidos() {
        return apellidos != null ? apellidos : "";
    }

    public String getNif() {
        return nif != null ? nif : "---";
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public String getRol() {
        return rol != null ? rol : "Trabajador";
    }

    public String getNombreCompleto() {
        return getNombre() + " " + getApellidos();
    }
}