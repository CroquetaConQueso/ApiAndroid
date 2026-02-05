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

    // Campos nuevos que YA envía tu backend (Flasky)
    @SerializedName("alerta_fichaje")
    private boolean alertaFichaje;

    @SerializedName("alerta_titulo")
    private String alertaTitulo;

    @SerializedName("alerta_mensaje")
    private String alertaMensaje;

    @SerializedName("trabaja_hoy")
    private boolean trabajaHoy;

    @SerializedName("hora_entrada_hoy")
    private String horaEntradaHoy;

    public String getAccessToken() { return accessToken; }
    public int getIdTrabajador() { return idTrabajador; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public int getIdEmpresa() { return idEmpresa; }

    public boolean isAlertaFichaje() { return alertaFichaje; }
    public String getAlertaTitulo() { return alertaTitulo; }
    public String getAlertaMensaje() { return alertaMensaje; }
    public boolean isTrabajaHoy() { return trabajaHoy; }
    public String getHoraEntradaHoy() { return horaEntradaHoy; }
}
