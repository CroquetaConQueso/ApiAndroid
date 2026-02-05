package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class FichajeResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("tipo")
    private String tipo;

    @SerializedName("fecha_hora")
    private String fechaHora;

    @SerializedName("latitud")
    private double latitud;

    @SerializedName("longitud")
    private double longitud;

    // Constructor vacío
    public FichajeResponse() {
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }


    public String getFechaHora() {
        return fechaHora;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }
}
