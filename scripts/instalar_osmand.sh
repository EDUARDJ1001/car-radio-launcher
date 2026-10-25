#!/usr/bin/env bash
# Copies the OsmAnd Honduras map to the radio and checks OsmAnd is installed.
# Prerequisite: install OsmAnd from the radio's Play Store (it picks the 32-bit
# build automatically) and open it once so it creates its data folder.
# Usage (Git Bash):  bash C:/CAR-RADIO/scripts/instalar_osmand.sh [ip:puerto]
set -euo pipefail
export MSYS_NO_PATHCONV=1  # keep Git Bash from rewriting /sdcard/... into Windows paths

ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
D="${1:-10.29.133.31:5555}"
MAP="C:/CAR-RADIO/mapas/osmand/Honduras_centralamerica_2.obf"

"$ADB" connect "$D" >/dev/null
"$ADB" -s "$D" get-state >/dev/null || { echo "Radio no conectado en $D"; exit 1; }
[ -f "$MAP" ] || { echo "Falta $MAP"; exit 1; }

PKG=""
for p in net.osmand net.osmand.plus; do
  if "$ADB" -s "$D" shell pm list packages "$p" | grep -qx "package:$p"; then PKG="$p"; break; fi
done
[ -n "$PKG" ] || { echo "OsmAnd no está instalado en el radio. Instálalo desde Play Store y ábrelo una vez."; exit 1; }
echo "OsmAnd encontrado: $PKG ($("$ADB" -s "$D" shell dumpsys package "$PKG" | grep -m1 versionName | tr -d ' '))"

DEST="/sdcard/Android/data/$PKG/files"
"$ADB" -s "$D" shell mkdir -p "$DEST"
"$ADB" -s "$D" push "$MAP" "$DEST/"
"$ADB" -s "$D" shell ls -l "$DEST/" | grep -i honduras
echo "Listo. Abre OsmAnd: el mapa de Honduras aparece en Menú > Mapas y recursos > Instalados."
