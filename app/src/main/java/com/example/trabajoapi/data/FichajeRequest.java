package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class FichajeRequest {
    private Double latitud;
    private Double longitud;

    @SerializedName("nfc_data")
    private String nfcData;

    // Constructor para fichaje manual (sin NFC)
    public FichajeRequest(Double latitud, Double longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.nfcData = null;
    }

    // Constructor para fichaje por NFC
    public FichajeRequest(Double latitud, Double longitud, String nfcData) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.nfcData = nfcData;
    }
}