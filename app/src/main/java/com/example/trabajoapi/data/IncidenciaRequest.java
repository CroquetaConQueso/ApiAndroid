package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class IncidenciaRequest {
    @SerializedName("tipo")
    private String tipo;

    @SerializedName("fecha_inicio")
    private String fechaInicio; // Formato YYYY-MM-DD

    @SerializedName("fecha_fin")
    private String fechaFin;    // Formato YYYY-MM-DD

    @SerializedName("comentario_trabajador")
    private String comentario;

    public IncidenciaRequest(String tipo, String fechaInicio, String fechaFin, String comentario) {
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.comentario = comentario;
    }
}