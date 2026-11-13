package com.dexp.launcher;

import android.app.Application;

/**
 * Минимальный Application-класс.
 * Важно: здесь НЕТ инициализации рекламных SDK, аналитики и трекеров —
 * именно они обычно "съедают" память и скорость на Android TV.
 */
public class LauncherApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Лаунчер должен загружаться мгновенно: никакой фоновой работы при старте.
    }
}
