package com.example.trabajoapi.nfc;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.SystemClock;

public class NfcFichajeController implements NfcAdapter.ReaderCallback {

    public interface Listener {
        void onNfcReady(boolean enabled);
        void onTagValida(String nfcId);
        void onTagInvalida(String motivo, String payloadLeido);
        void onNfcError(String motivo);
    }

    private static final long DEBOUNCE_MS = 2500;

    private final Listener listener;
    private NfcAdapter nfcAdapter;
    private long lastRead = 0;

    public NfcFichajeController(Listener listener) {
        this.listener = listener;
    }

    public void onResume(Activity activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);

        if (nfcAdapter == null) {
            if (listener != null) listener.onNfcReady(false);
            return;
        }

        boolean enabled = nfcAdapter.isEnabled();
        if (listener != null) listener.onNfcReady(enabled);

        if (!enabled) return;

        Bundle opts = new Bundle();
        opts.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

        // Habilitamos lectura de todas las tecnologías comunes para maximizar compatibilidad
        int flags = NfcAdapter.FLAG_READER_NFC_A |
                NfcAdapter.FLAG_READER_NFC_B |
                NfcAdapter.FLAG_READER_NFC_F |
                NfcAdapter.FLAG_READER_NFC_V;

        nfcAdapter.enableReaderMode(activity, this, flags, opts);
    }

    public void onPause(Activity activity) {
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableReaderMode(activity);
            } catch (Exception ignored) { }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        // Evitamos lecturas múltiples muy seguidas (rebote)
        long now = SystemClock.elapsedRealtime();
        if (now - lastRead < DEBOUNCE_MS) return;
        lastRead = now;

        try {
            // 1. Obtenemos el UID (Número de serie único de la tarjeta)
            byte[] idBytes = tag.getId();

            if (idBytes == null || idBytes.length == 0) {
                if (listener != null) listener.onTagInvalida("La tarjeta no tiene UID legible", "");
                return;
            }

            // 2. Lo convertimos a String Hexadecimal (ej: "04A35B...") para enviarlo al backend
            String nfcId = toHexString(idBytes);

            if (listener != null) {
                listener.onTagValida(nfcId);
            }

        } catch (Exception e) {
            if (listener != null) listener.onNfcError("Error leyendo UID: " + e.getMessage());
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}