package com.dexp.launcher;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * Фоновая очистка. Запускается после загрузки ТВ (BootReceiver) —
 * выгружает фоновые процессы preinstall-мусора, чтобы интерфейс не тормозил.
 */
public class MemoryCleanService extends IntentService {

    public MemoryCleanService() {
        super("dexp-memory-clean");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Optimizer.killBackgroundProcesses(getApplicationContext());
        Optimizer.trimMemory(getApplicationContext());
    }

    public static void enqueue(Context ctx) {
        try {
            ctx.startService(new Intent(ctx, MemoryCleanService.class));
        } catch (Exception ignored) {
        }
    }
}
