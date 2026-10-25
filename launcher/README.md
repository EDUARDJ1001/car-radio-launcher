# CR-V Launcher

Launcher propio para el radio Android de la Honda CR-V 2010, basado en `../diseno/referencia-home.webp`.

## Compilar
```bash
./gradlew.bat assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

## Instalar en el radio (por ADB)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
```bash
adb shell cmd notification allow_listener com.eduard.crvlauncher/com.eduard.crvlauncher.data.MediaListenerService
```
```bash
adb shell pm grant com.eduard.crvlauncher android.permission.ACCESS_FINE_LOCATION
```
```bash
adb shell cmd package set-home-activity com.eduard.crvlauncher/.MainActivity
```

## Volver al launcher original
```bash
adb shell pm clear-package-preferred-activities com.eduard.crvlauncher
```
o desinstalar:
```bash
adb uninstall com.eduard.crvlauncher
```

## Personalizar
- `app/src/main/java/com/eduard/crvlauncher/Config.kt`: nombre, coordenadas de respaldo para el clima, y paquetes de las apps del radio (teléfono, radio, CarPlay, mapas).
- `app/src/main/res/drawable-nodpi/crv.png`: foto del auto (PNG con fondo transparente).
