#!/usr/bin/env bash
# Read-only: collects how Jancar's FM radio / BT music / audio-source switching
# are exposed, so the launcher can drive them without opening their screens.
# Changes NOTHING on the radio (only dumpsys / pm / pull).
# Usage (Git Bash):  bash C:/CAR-RADIO/scripts/investigar_fuentes.sh [ip:puerto]
set -uo pipefail
export MSYS_NO_PATHCONV=1  # keep Git Bash from rewriting /system/... into Windows paths

ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
D="${1:-10.29.133.31:5555}"
OUT="C:/CAR-RADIO/diagnostico/fuentes"
PKGS="com.jancar.radio com.jancar.music com.jancar.services com.jancar.btservice com.jancar.bluetooth com.txz.radio com.android.bluetooth"

"$ADB" connect "$D" >/dev/null
"$ADB" -s "$D" get-state >/dev/null || { echo "Radio no conectado en $D"; exit 1; }
mkdir -p "$OUT/apks"

for p in $PKGS; do
  echo "-> $p"
  # Activities, services, receivers, providers with their intent filters and exported flags.
  "$ADB" -s "$D" shell dumpsys package "$p" > "$OUT/pkg_$p.txt" 2>&1
  for apk in $("$ADB" -s "$D" shell pm path "$p" | tr -d '\r' | sed 's/^package://'); do
    "$ADB" -s "$D" pull "$apk" "$OUT/apks/${p}__$(basename "$apk")" >/dev/null 2>&1 && echo "   apk: $(basename "$apk")"
  done
done

"$ADB" -s "$D" shell dumpsys media_session > "$OUT/media_session.txt" 2>&1
"$ADB" -s "$D" shell dumpsys audio > "$OUT/audio.txt" 2>&1
"$ADB" -s "$D" shell dumpsys activity services com.jancar > "$OUT/servicios_jancar.txt" 2>&1
"$ADB" -s "$D" shell 'getprop | grep -iE "source|radio|fm|tuner|jancar|atc\.audio"' > "$OUT/props_fuentes.txt"

echo "Listo: $OUT"
