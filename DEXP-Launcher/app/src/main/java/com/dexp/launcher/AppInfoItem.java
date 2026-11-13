package com.dexp.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Модель приложения в лаунчере + получение списка установленных приложений.
 * Показываем только то, что реально имеет экран запуска (LEANBACK_LAUNCHER / LAUNCHER).
 */
public class AppInfoItem {

    public final String label;
    public final String packageName;
    public final Intent launchIntent;
    public final boolean isSystem;

    private Drawable icon;

    public AppInfoItem(String label, String packageName, Intent launchIntent, boolean isSystem) {
        this.label = label;
        this.packageName = packageName;
        this.launchIntent = launchIntent;
        this.isSystem = isSystem;
    }

    public Drawable getIcon(PackageManager pm) {
        if (icon == null) {
            try {
                icon = pm.getActivityIcon(launchIntent);
            } catch (Exception e) {
                icon = null;
            }
        }
        return icon;
    }

    /** Пакеты-кандидаты на скрытие: системные сервисы DEXP/Amlogic без реальной пользы для пользователя. */
    private static final Set<String> HIDDEN_PACKAGES = new HashSet<>();
    static {
        // типичные preinstall'ы DEXP / MTK / Amlogic TV: реклама, OTA-трекер, "рекомендации"
        HIDDEN_PACKAGES.add("com.dexp.ads");
        HIDDEN_PACKAGES.add("com.android.tv.settingsrecommendations");
        HIDDEN_PACKAGES.add("com.mediatek.datatransfer");
        HIDDEN_PACKAGES.add("com.amlogic.smart_home");
        HIDDEN_PACKAGES.add("com.android.certinstaller");
        HIDDEN_PACKAGES.add("com.android.tag");
    }

    /**
     * Возвращает список запускаемых приложений, отсортированный:
     * сначала пользовательские, затем системные, по алфавиту.
     */
    public static List<AppInfoItem> loadApps(final Context ctx) {
        PackageManager pm = ctx.getPackageManager();

        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent leanback = new Intent(Intent.ACTION_MAIN, null);
            leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            resolved = pm.queryIntentActivities(leanback, 0);
            // добавляем обычные LAUNCHER-приложения (на старых прошивках не всё дублирует leanback)
            for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
                boolean found = false;
                for (ResolveInfo lb : resolved) {
                    if (lb.activityInfo.packageName.equals(ri.activityInfo.packageName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) resolved.add(ri);
            }
        } else {
            resolved = pm.queryIntentActivities(main, 0);
        }

        String self = ctx.getPackageName();
        List<AppInfoItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ResolveInfo ri : resolved) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(self)) continue;               // не показываем сам лаунчер
            if (HIDDEN_PACKAGES.contains(pkg)) continue;  // скрываем рекламные модули
            if (!seen.add(pkg)) continue;                 // один пакет = одна иконка

            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                CharSequence ls = ri.loadLabel(pm);
                String label = (ls != null && ls.length() > 0) ? ls.toString() : pkg;

                Intent launch = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClassName(pkg, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                items.add(new AppInfoItem(label, pkg, launch, system));
            } catch (Exception ignored) {
                // приложение могло быть удалено прямо во время обхода списка
            }
        }

        Collections.sort(items, new Comparator<AppInfoItem>() {
            @Override
            public int compare(AppInfoItem a, AppInfoItem b) {
                if (a.isSystem != b.isSystem) return a.isSystem ? 1 : -1;
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return items;
    }
}
