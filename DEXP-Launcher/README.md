# DEXP Launcher — лёгкий лаунчер для телевизора DEXP 24HEY1

Android TV-приложение (лаунчер), которое **ускоряет слабый Android-ТВ** и
**не содержит рекламы, трекеров и телеметрии вообще**.

## Почему ТВ с DEXP 24HEY1 тормозит и как это решает лаунчер

Штатная оболочка DEXP/МТС держится на preinstall-мусоре: сервисы рекомендаций,
рекламные модули (`com.dexp.ads` и аналоги), фоновые OTA/аналитические службы.
Они постоянно едят оперативную память (её в этом ТВ всего ~1 ГБ) и CPU.

Что делает наш лаунчер:

| Проблема | Решение в лаунчере |
|---|---|
| Фоновые процессы жрут RAM | Кнопка **«⚡ Ускорить»** убивает фоновые процессы (`killBackgroundProcesses`) и показывает свободную память до/после |
| Реклама и «рекомендации» в меню | Рекламные/трекер-пакеты **скрыты из списка** (`HIDDEN_PACKAGES` в `AppInfoItem.java`);Долгий тап / кнопка Menu) → «О приложении» позволяет **остановить или удалить** preinstall-мусор через штатные Настройки |
| Трекинг и сбор данных | В манифесте **явно удалены разрешения** `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` (`tools:node="remove"`) — приложение физически не может никуда отправлять данные |
| Тяжёлая оболочка | Только Java + AndroidX (appcompat/recyclerview). Никаких Glide/Picasso/Firebase/AdMob. APK < 1 МБ, мгновенный старт |
| Мусор копится после перезагрузки | `BootReceiver` + `MemoryCleanService` чистят фон после включения ТВ |

## Структура проекта

```
DEXP-Launcher/
├── build.gradle / settings.gradle      # Gradle 8.x, AGP 8.1
└── app/
    ├── build.gradle                    # minSdk 19 (KitKat — как у 24HEY1), targetSdk 29
    └── src/main/
        ├── AndroidManifest.xml         # HOME + LEANBACK_LAUNCHER, разрешения "только база"
        ├── java/com/dexp/launcher/
        │   ├── LauncherApp.java        # Application без SDK-инициализаций
        │   ├── MainActivity.java       # экран HOME: сетка приложений + «Ускорить»
        │   ├── AppInfoItem.java        # запускable-приложения + скрытие рекламных пакетов
        │   ├── AppGridAdapter.java     # быстрая сетка (RecyclerView, без библиотек картинок)
        │   ├── Optimizer.java          # память из /proc/meminfo, killBackgroundProcesses
        │   ├── MemoryCleanService.java # фоновая очистка
        │   ├── BootReceiver.java       # очистка после загрузки ТВ
        │   └── QuickLaunchActivity.java# быстрый запуск по external intent
        └── res/                        # тёмная TV-тема, фокус-подсветка под пульт
```

## Сборка

Требуется JDK 17 и Android SDK (platform 34):

```bash
cd DEXP-Launcher
gradle wrapper --gradle-version 8.5   # один раз, если нет gradlew
./gradlew assembleRelease             # APK: app/build/outputs/apk/release/app-release.apk
```

Подпись релизного APK:

```bash
apksigner sign --ks my.keystore app-release.apk
```

## Установка на DEXP 24HEY1

1. На ТВ: **Настройки → Безопасность → Неизвестные источники → Разрешить**.
2. Поставьте APK любой файловостью с USB-флешки, либо по сети:
   ```bash
   adb connect <IP-адрес-ТВ>
   adb install app-release.apk
   ```
3. Нажмите кнопку **Home** на пульте → выберите **DEXP Launcher** → «Всегда».

## Управление с пульта

- **Стрелки** — навигация по сетке (ячейки подсвечиваются рамкой фокуса)
- **OK** — запустить приложение
- **Menu / долгий тап** — «О приложении» (там можно остановить/удалить preinstall-мусор) или «Удалить»
- **⚡ Ускорить** — экстренная очистка памяти; пункт «Сделать лаунчером по умолчанию» тоже в кнопке Menu

## Приватность

Приложение **не имеет разрешения INTERNET** — проверьте это в установленном APK:

```bash
aapt dump permissions app-release.apk
```

Вы увидите только `KILL_BACKGROUND_PROCESSES`, `CLEAR_APP_CACHE`,
`RECEIVE_BOOT_COMPLETED`, `QUERY_ALL_PACKAGES`, `PACKAGE_USAGE_STATS`.
Никакой рекламы, аналитики и фоновой передачи данных — по определению.
