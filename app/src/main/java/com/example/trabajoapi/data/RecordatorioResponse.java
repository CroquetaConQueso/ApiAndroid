package com.example.trabajoapi.data;
import com.google.gson.annotations.SerializedName;

public class RecordatorioResponse {
    @SerializedName("avisar")
    private boolean avisar;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("mensaje")
    private String mensaje;

    public boolean isAvisar() { return avisar; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
}