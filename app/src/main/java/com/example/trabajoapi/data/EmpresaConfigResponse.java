package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class EmpresaConfigResponse implements Serializable {

    // CAMBIO CLAVE: "value" es lo que se envía al servidor (Inglés).
    // "alternate" permite leer lo que llega, venga como venga.

    @SerializedName(value = "latitude", alternate = {"lat", "latitud"})
    private Double latitud;

    @SerializedName(value = "longitude", alternate = {"lon", "lng", "longitud"})
    private Double longitud;

    @SerializedName(value = "radius", alternate = {"radio", "r"})
    private Integer radio;

    public EmpresaConfigResponse() {
    }

    public EmpresaConfigResponse(Double latitud, Double longitud, Integer radio) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.radio = radio;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public Integer getRadio() {
        return radio;
    }

    public void setRadio(Integer radio) {
        this.radio = radio;
    }
}