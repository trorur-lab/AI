package com.dexp.launcher;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Утилита "ускорения" ТВ: очистка фоновых процессов и кэшей.
 * Работает локально, без интернета — никакого трафика и телеметрии.
 */
public class Optimizer {

    public static class MemInfo {
        public long totalMb;
        public long availMb;

        public String human() {
            return availMb + " / " + totalMb + " МБ";
        }
    }

    /** Информация о памяти (работает на API 16+ через /proc/meminfo — стабильно на TV-чипах). */
    public static MemInfo getMemInfo(Context ctx) {
        MemInfo m = new MemInfo();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"), 256);
            String line;
            long totalKb = 0, availKb = -1, freeKb = 0, buffersKb = 0, cachedKb = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal:")) totalKb = parseKb(line);
                else if (line.startsWith("MemAvailable:")) availKb = parseKb(line);
                else if (line.startsWith("MemFree:")) freeKb = parseKb(line);
                else if (line.startsWith("Buffers:")) buffersKb = parseKb(line);
                else if (line.startsWith("Cached:")) cachedKb = parseKb(line);
            }
            br.close();
            m.totalMb = totalKb / 1024;
            long avail = availKb >= 0 ? availKb : (freeKb + buffersKb + cachedKb / 2);
            m.availMb = avail / 1024;
        } catch (Exception e) {
            // запасной вариант через ActivityManager
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            m.totalMb = mi.totalMem / 1048576L;
            m.availMb = mi.availMem / 1048576L;
        }
        return m;
    }

    private static long parseKb(String line) {
        String[] parts = line.split("\\s+");
        try {
            return Long.parseLong(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Очистка: убиваем фоновые процессы всех приложений, кроме текущих
     * (лаунчер, системные серверы, активное приложение).
     */
    @SuppressWarnings("deprecation")
    public static int killBackgroundProcesses(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        int killed = 0;
        String self = ctx.getPackageName();
        List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
        if (procs == null) return 0;
        for (ActivityManager.RunningAppProcessInfo p : procs) {
            if (p.processName.equals(self)) continue;
            if ((p.flags & ActivityManager.RunningAppProcessInfo.FLAG_PERSISTENT) != 0) continue;
            if (p.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) continue;
            try {
                am.killBackgroundProcesses(p.processName);
                killed++;
            } catch (Exception ignored) {
            }
        }
        return killed;
    }

    /** Освободить RAM: просим систему выгрузить неиспользуемые кэши (нужен root для TRIM, но gc-сигнал безопасен). */
    public static void trimMemory(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        // единственное доступное без root действие — принудительный сборщик мусора нашего лёгкого процесса
        Runtime.getRuntime().gc();
    }

    /** Открыть системный экран «Использование данных/памяти» — пользователь может удалить кэш вручную. */
    public static void openStorageSettings(Context ctx) {
        try {
            ctx.startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            try {
                ctx.startActivity(new Intent(Settings.ACTION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {
            }
        }
    }

    /** Открыть страницу приложения в Настройках (для удаления/остановки preinstall-мусора). */
    public static void openAppSettings(Context ctx, String packageName) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception ignored) {
        }
    }

    public static String humanSize(Context ctx, long bytes) {
        return Formatter.formatFileSize(ctx, bytes);
    }

    /** Размер кэша самого лаунчера (для отображения — он почти нулевой, в этом и смысл). */
    public static long ownCacheSize(Context ctx) {
        long size = 0;
        File cache = ctx.getCacheDir();
        if (cache != null && cache.exists()) {
            for (File f : cache.listFiles()) size += f.length();
        }
        return size;
    }

    public static boolean isLollipopOrNewer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
