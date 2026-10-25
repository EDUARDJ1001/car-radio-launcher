"""Recolours mapsforge's default render theme into the launcher's dark navy palette.

Usage: python recolor_theme.py default.xml ../app/src/main/assets/crv-dark.xml
(default.xml comes from the mapsforge-themes jar: assets/mapsforge/default.xml)
"""
import re, colorsys, sys
src, dst = sys.argv[1], sys.argv[2]
xml = open(src, encoding="utf-8").read()

def parse(h):
    h = h.lstrip("#")
    return (h[:2] if len(h) == 8 else None), tuple(int(h[-6:][i:i+2], 16) for i in (0, 2, 4))
def fmt(a, c):
    return "#" + (a or "") + "".join(f"{max(0, min(255, round(v))):02X}" for v in c)
def hls(c):
    return colorsys.rgb_to_hls(*(v / 255 for v in c))
def blend(base, c, t):
    return tuple(b * (1 - t) + x * t for b, x in zip(base, c))
def hexc(s):
    return parse(s)[1]

NAVY = hexc("#0E1D4A")

def area(c):
    h, l, s = hls(c)
    if 0.50 <= h <= 0.62 and s > 0.3:        # water
        return hexc("#071330")
    if 0.20 <= h <= 0.45 and s > 0.15:       # parks / forest / grass
        return blend(hexc("#0C2238"), c, 0.10)
    return blend(NAVY, c, 0.10)               # land, buildings, landuse

def line(c):
    h, l, s = hls(c)
    if l >= 0.62 and s >= 0.35 and (h <= 0.17 or h >= 0.95):   # yellow/orange: major roads
        return hexc("#B8954A")
    if l >= 0.80:                                               # white/light: ordinary roads
        return hexc("#3A5AA8")
    if 0.50 <= h <= 0.62 and s > 0.3:                          # rivers / streams
        return hexc("#0A1C45")
    if l < 0.45:                                                # casings, borders
        return hexc("#07102A")
    return blend(hexc("#20366E"), c, 0.12)                       # tracks, paths, misc

def text_fill(c):
    h, l, s = hls(c)
    if s > 0.35:                                                # coloured labels (water names...)
        r, g, b = colorsys.hls_to_rgb(h, 0.72, min(s, 0.6))
        return (r * 255, g * 255, b * 255)
    return hexc("#DCE6FF")

def recolor_tag(m):
    tag, kind = m.group(0), m.group(1)
    def sub(attr, fn):
        nonlocal tag
        def rep(mm):
            a, c = parse(mm.group(2))
            return f'{mm.group(1)}="{fmt(a, fn(c))}"'
        tag = re.sub(rf'\b({attr})="(#[0-9A-Fa-f]{{6,8}})"', rep, tag)
    if kind == "area":
        sub("fill", area); sub("stroke", lambda c: blend(area(c), (255, 255, 255), 0.04))
    elif kind in ("line", "lineSymbol"):
        sub("stroke", line)
    elif kind in ("caption", "pathText"):
        sub("fill", text_fill); sub("stroke", lambda c: hexc("#0A1538"))
    elif kind == "circle":
        sub("stroke", lambda c: hexc("#0A1538"))
    return tag

xml = re.sub(r'<(area|line|lineSymbol|caption|pathText|circle)\b[^>]*>', recolor_tag, xml)
xml = re.sub(r'map-background="#[0-9A-Fa-f]+"', 'map-background="#0E1D4A"', xml)
xml = re.sub(r'map-background-outside="#[0-9A-Fa-f]+"', 'map-background-outside="#081233"', xml)
open(dst, "w", encoding="utf-8").write(xml)
print("ok")
