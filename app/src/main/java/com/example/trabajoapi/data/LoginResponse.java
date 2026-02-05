package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("id_trabajador")
    private Integer idTrabajador;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("rol")
    private String rol;

    @SerializedName("id_empresa")
    private Integer idEmpresa;

    // Opcional (si el backend lo envía)
    @SerializedName("recordatorio")
    private Recordatorio recordatorio;

    public String getAccessToken() {
        return accessToken;
    }

    public Integer getIdTrabajador() {
        return idTrabajador;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public Integer getIdEmpresa() {
        return idEmpresa;
    }

    public Recordatorio getRecordatorio() {
        return recordatorio;
    }

    public static class Recordatorio {

        @SerializedName("avisar")
        private Boolean avisar;

        @SerializedName("titulo")
        private String titulo;

        @SerializedName("mensaje")
        private String mensaje;

        public boolean isAvisar() {
            return avisar != null && avisar;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
