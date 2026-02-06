package com.example.trabajoapi.work;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.trabajoapi.MainActivity;
import com.example.trabajoapi.R;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;

import java.io.IOException;

import retrofit2.Response;

public class TrabajadorRecordatorio extends Worker {

    private static final String CHANNEL_ID = "canal_recordatorio_fichaje_bg_v1";

    public TrabajadorRecordatorio(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SessionManager sm = new SessionManager(getApplicationContext());
            String auth = sm.getAuthToken();
            if (auth == null) return Result.success();

            Response<RecordatorioResponse> resp =
                    RetrofitClient.getInstance()
                            .getMyApi()
                            .getRecordatorioFichaje("Bearer " + auth)
                            .execute();

            // Backend: 204 = no recordatorio
            if (resp.code() == 204) return Result.success();

            if (!resp.isSuccessful() || resp.body() == null) {
                // 401/403/etc -> no reintentar infinito
                return Result.success();
            }

            RecordatorioResponse r = resp.body();

            // Si tienes campo "avisar" y viene a false, tampoco molestes
            if (r.hasAvisarFlag() && !r.isAvisar()) return Result.success();

            String titulo = safe(r.getTitulo(), "Aviso");
            String mensaje = safe(r.getMensaje(), "");

            if (mensaje.trim().isEmpty()) return Result.success();

            mostrarNotificacion(titulo, mensaje);
            return Result.success();

        } catch (IOException e) {
            // fallo temporal de red
            return Result.retry();
        } catch (Exception e) {
            return Result.success();
        }
    }

    private void mostrarNotificacion(String titulo, String cuerpo) {
        Context ctx = getApplicationContext();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorio de fichaje (BG)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        nm.notify(501, b.build());
    }

    private String safe(String v, String fallback) {
        if (v == null) return fallback;
        String s = v.trim();
        return s.isEmpty() ? fallback : s;
    }
}
