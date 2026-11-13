package com.dexp.launcher;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Главный экран DEXP Launcher.
 *  - сетка приложений (стрелки пульта, OK = запуск, Меню/долгий тап = действия);
 *  - панель памяти и кнопка "Ускорить" (убивает фоновые процессы);
 *  - никаких виджетов-рекомендаций, новостных лент и рекламных блоков.
 */
public class MainActivity extends AppCompatActivity implements AppGridAdapter.Listener {

    private RecyclerView grid;
    private TextView memText;
    private AppGridAdapter adapter;
    private List<AppInfoItem> apps;

    private final BroadcastReceiver packageChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadApps(); // мгновенное обновление при установке/удалении
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grid = (RecyclerView) findViewById(R.id.appGrid);
        memText = (TextView) findViewById(R.id.memText);

        findViewById(R.id.btnBoost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boost();
            }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Optimizer.openStorageSettings(MainActivity.this);
            }
        });

        GridLayoutManager lm = new GridLayoutManager(this, spanCount());
        grid.setLayoutManager(lm);
        grid.setNextFocusUpId(R.id.btnBoost); // с пульта: вверх от сетки -> кнопки

        reloadApps();
        updateMemory();
    }

    private int spanCount() {
        // на маленьких TV-панелях (720p) — 5 колонок, на 1080p — 6
        float density = getResources().getDisplayMetrics().density;
        return density <= 1.0f ? 5 : 6;
    }

    private void reloadApps() {
        apps = AppInfoItem.loadApps(this);
        adapter = new AppGridAdapter(apps, getPackageManager(), this);
        grid.setAdapter(adapter);
    }

    private void updateMemory() {
        Optimizer.MemInfo m = Optimizer.getMemInfo(this);
        memText.setText(getString(R.string.memory_label, m.human()));
    }

    /** Кнопка «Ускорить»: снятие фонового мусора + сборщик мусора. */
    private void boost() {
        int killed = Optimizer.killBackgroundProcesses(this);
        Optimizer.trimMemory(this);
        updateMemory();
        Toast.makeText(this, getString(R.string.boost_done, killed), Toast.LENGTH_SHORT).show();
    }

    // ---------- действия с приложением ----------

    @Override
    public void onClick(AppInfoItem item) {
        try {
            startActivity(item.launchIntent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.launch_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onLongClick(final AppInfoItem item) {
        String[] actions = {getString(R.string.act_info), getString(R.string.act_uninstall)};
        new AlertDialog.Builder(this)
                .setTitle(item.label)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        Optimizer.openAppSettings(MainActivity.this, item.packageName);
                    } else {
                        try {
                            startActivity(new Intent(Intent.ACTION_DELETE,
                                    android.net.Uri.parse("package:" + item.packageName)));
                        } catch (Exception ignored) {
                        }
                    }
                })
                .show();
        return true;
    }

    // ---------- пульт ----------

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            showMenu();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void showMenu() {
        String[] items = {getString(R.string.menu_boost), getString(R.string.menu_home_default)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) boost();
                    else askDefaultHome();
                })
                .show();
    }

    /** Сделать лаунчером по умолчанию (системный диалог выбора HOME). */
    private void askDefaultHome() {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(i, getString(R.string.menu_home_default)));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMemory();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addDataScheme("package");
        registerReceiver(packageChanged, f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(packageChanged);
        } catch (Exception ignored) {
        }
    }
}
