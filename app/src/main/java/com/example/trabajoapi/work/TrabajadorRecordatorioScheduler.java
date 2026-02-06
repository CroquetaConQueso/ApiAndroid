package com.example.trabajoapi.work;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class TrabajadorRecordatorioScheduler {

    private TrabajadorRecordatorioScheduler() {}

    private static final String UNIQUE_WORK_NAME = "trabajador_recordatorio_bg_v1";

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(TrabajadorRecordatorio.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .addTag(UNIQUE_WORK_NAME)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // si cambias algo, se actualiza
                req
        );
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
    }
}
