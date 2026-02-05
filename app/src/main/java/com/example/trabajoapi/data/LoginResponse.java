package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("id_trabajador")
    private int idTrabajador;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("rol")
    private String rol;

    @SerializedName("id_empresa")
    private int idEmpresa;

    @SerializedName("recordatorio")
    private Recordatorio recordatorio;

    public String getAccessToken() { return accessToken; }
    public int getIdTrabajador() { return idTrabajador; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public int getIdEmpresa() { return idEmpresa; }
    public Recordatorio getRecordatorio() { return recordatorio; }

    public static class Recordatorio {
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
}
