#!/usr/bin/env python3
"""Exporta la vibració de cada so de l'iPhone (`sound-*.ahap`) a ones de vibració d'Android.

Com `export-assets.py`, la font és el repositori de l'iPhone i el que s'escriu no es toca a mà:

    python3 scripts/export-haptics.py ../VIRA-APP-I-WEB

Escriu `core/sound/ViraHapticPatternData.kt`: per a cada so, els trams (ms) i les amplituds
(1–255, 0 és silenci) que `VibrationEffect.createWaveform` vol. Un `.ahap` és Core Haptics:
- `HapticTransient` (un cop) → un tram curt de 14 ms a la seva intensitat;
- `HapticContinuous` (una ona) → la seva durada, a la seva intensitat multiplicada per la corba
  `HapticIntensityControl` que comença al mateix temps, si n'hi ha;
- l'agudesa (`HapticSharpness`) no té equivalent en una ona d'amplitud i es deixa.

Es mostreja a graons de 10 ms i es fusionen els graons iguals seguits.
"""

import argparse
import json
from pathlib import Path

STEP_MS = 10
TRANSIENT_MS = 14
OUT = Path(__file__).resolve().parent.parent / "app/src/main/kotlin/cat/vira/android/core/sound/ViraHapticPatternData.kt"


def param(event, name, default=0.0):
    for p in event.get("EventParameters", []):
        if p["ParameterID"] == name:
            return float(p["ParameterValue"])
    return default


def curve_at(curves, t):
    """Multiplicador de la corba d'intensitat que toca a `t` (segons), interpolant entre punts."""
    for start, points in curves:
        end = start + points[-1][0]
        if start <= t <= end:
            local = t - start
            for (t0, v0), (t1, v1) in zip(points, points[1:]):
                if t0 <= local <= t1:
                    return v0 if t1 == t0 else v0 + (v1 - v0) * (local - t0) / (t1 - t0)
            return points[-1][1]
    return 1.0


def waveform(ahap):
    events, curves = [], []
    for entry in ahap["Pattern"]:
        if "Event" in entry:
            events.append(entry["Event"])
        elif "ParameterCurve" in entry and entry["ParameterCurve"]["ParameterID"] == "HapticIntensityControl":
            c = entry["ParameterCurve"]
            curves.append((float(c["Time"]), [(float(p["Time"]), float(p["ParameterValue"])) for p in c["ParameterCurveControlPoints"]]))

    spans = []  # (start_ms, end_ms, amplitude 0..1 or callable)
    for e in events:
        start = float(e["Time"]) * 1000
        level = param(e, "HapticIntensity", 1.0)
        if e["EventType"] == "HapticTransient":
            spans.append((start, start + TRANSIENT_MS, level, False))
        elif e["EventType"] == "HapticContinuous":
            spans.append((start, start + float(e.get("EventDuration", 0)) * 1000, level, True))
    if not spans:
        return [], []

    total = int(max(end for _, end, _, _ in spans) // STEP_MS) + 1
    samples = []
    for i in range(total):
        t = i * STEP_MS
        value = 0.0
        for start, end, level, continuous in spans:
            if start <= t < end:
                value = max(value, level * (curve_at(curves, t / 1000) if continuous else 1.0))
        samples.append(round(min(max(value, 0.0), 1.0) * 255))

    timings, amplitudes = [], []
    for amp in samples:
        if amplitudes and amplitudes[-1] == amp:
            timings[-1] += STEP_MS
        else:
            timings.append(STEP_MS)
            amplitudes.append(amp)
    while amplitudes and amplitudes[-1] == 0:
        timings.pop()
        amplitudes.pop()
    return timings, amplitudes


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("iphone_repo", type=Path)
    args = parser.parse_args()
    files = sorted((args.iphone_repo / "Vira/Resources").glob("sound-*.ahap"))
    lines = []
    for f in files:
        timings, amplitudes = waveform(json.loads(f.read_text()))
        if not timings:
            continue
        lines.append(
            f'        "{f.stem}" to ViraHapticWave(longArrayOf({", ".join(map(str, timings))}), '
            f'intArrayOf({", ".join(map(str, amplitudes))})),'
        )
    OUT.write_text(
        "package cat.vira.android.core.sound\n\n"
        "// Generat per scripts/export-haptics.py a partir dels .ahap de l'iPhone. No es toca a mà.\n\n"
        "internal object ViraHapticPatternData {\n"
        "    val waves: Map<String, ViraHapticWave> = mapOf(\n"
        + "\n".join(lines)
        + "\n    )\n}\n"
    )
    print(f"{len(lines)} patrons → {OUT}")


if __name__ == "__main__":
    main()
