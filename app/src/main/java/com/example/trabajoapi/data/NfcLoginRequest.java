package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class NfcLoginRequest {

    // El backend puede esperar "uid" o "nfc_uid".
    // Ajusta el nombre a lo que uses en Flask.
    @SerializedName("uid")
    private String uid;

    public NfcLoginRequest(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }
}
