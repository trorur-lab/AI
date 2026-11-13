package com.dexp.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * После загрузки телевизора запускаем лёгкую очистку памяти.
 * Никакого сетевого трафика — только локальные операции.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MemoryCleanService.enqueue(context);
        }
        // PACKAGE_ADDED / PACKAGE_REMOVED обрабатывает живой MainActivity, тут ничего не делаем
    }
}
