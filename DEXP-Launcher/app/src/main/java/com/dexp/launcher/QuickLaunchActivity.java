package com.dexp.launcher;

import android.app.Activity;
import android.os.Bundle;

/**
 * Прозрачная активность быстрого запуска: вызывается извне (например, по кнопке пульта),
 * запускает целевое приложение и сразу закрывается. Экономит время на старте «холодных» приложений.
 */
public class QuickLaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pkg = getIntent() != null ? getIntent().getStringExtra("pkg") : null;
        if (pkg != null) {
            android.content.Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(launch);
                } catch (Exception ignored) {
                }
            }
        }
        finish();
    }
}
