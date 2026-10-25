# Read-only diagnostic of the head unit. Changes NOTHING on the radio.
# Usage:  powershell -ExecutionPolicy Bypass -File C:\CAR-RADIO\scripts\diagnostico.ps1
$ErrorActionPreference = "Continue"
$out = "C:\CAR-RADIO\diagnostico"
New-Item -ItemType Directory -Force $out | Out-Null

function Run($name, $cmd) {
    Write-Host "-> $name"
    $result = & adb shell $cmd 2>&1 | Out-String
    $result | Out-File -Encoding utf8 "$out\$name.txt"
}

$devices = adb devices | Select-String "\tdevice$"
if (-not $devices) { Write-Host "No hay radio conectado (adb devices vacío o 'unauthorized')."; exit 1 }

# General / hardware
Run "props"            "getprop"
Run "pantalla"         "wm size; wm density; dumpsys display | grep -iE 'mBaseDisplayInfo|PhysicalDisplayInfo' | head -5"
Run "cpu_memoria"      "cat /proc/cpuinfo | grep -iE 'hardware|model name|processor' | sort -u; cat /proc/meminfo | head -3"
Run "almacenamiento"   "df -h /data /sdcard 2>/dev/null"

# Apps (to identify CAN bus / camera / BT / radio / CarPlay packages)
Run "paquetes"         "pm list packages -f"
Run "paquetes_sistema" "pm list packages -s"
Run "launcher_actual"  "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
Run "app_en_pantalla"  "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'"

# 1) Bluetooth / AVRCP
Run "bt_props"         "getprop | grep -iE 'avrcp|bluetooth|bt\.|a2dp'"
Run "bt_dumpsys"       "dumpsys bluetooth_manager"
Run "media_sessions"   "dumpsys media_session"

# 2) CAN bus / outside temperature
Run "canbus_apps"      "pm list packages | grep -iE 'can|bus|car|syu|fyt|ts|xbr|raise|hiworld|simple'"
Run "seriales"         "ls -l /dev/ttyS* /dev/ttyMT* /dev/ttyUSB* 2>/dev/null"

# 3) GPS / maps
Run "gps"              "dumpsys location | head -120"

Write-Host "`nListo. Resultados en $out"
