#!/usr/bin/env python3
"""Exporta els vídeos de l'iPhone (`Vira/Resources/*.mov`) a MP4 H.264 per a Android.

    python3 -m venv .venv && .venv/bin/pip install av numpy
    .venv/bin/python scripts/export-videos.py ../VIRA-APP-I-WEB

Alguns clips de l'iPhone porten **transparència** en HEVC amb canal alfa, el format d'Apple.
Android no el reprodueix, i H.264 no té canal alfa. Per això aquests surten com a **alfa
empaquetat**: el mateix fotograma dues vegades dins del vídeo, el color a una meitat i la
transparència (en grisos) a l'altra. El component de vídeo d'Android les ajunta en dibuixar-lo.
Les meitats van una sota l'altra o una al costat de l'altra, el que deixi el fotograma més petit,
perquè un mòbil de gamma mitjana descodifica fins a 1080p i prou.

La resta de clips no tenen transparència i surten com a MP4 normal, amb el so si en tenen.

Els vídeos van a `res/raw/` (`viri-emotion-tired` → `viri_emotion_tired.mp4`) i
`core/resources/ViraVideos.kt` en diu, per a cadascun, si porta alfa i com, la mida i la
cadència. Calen PyAV i numpy, perquè el decodificador de l'alfa d'HEVC és d'FFmpeg 8 i
l'`ffmpeg` dels sistemes acostuma a ser més vell.
"""

import argparse
import re
import sys
from fractions import Fraction
from pathlib import Path

import av
import numpy as np

KOTLIN = Path("app/src/main/kotlin/cat/vira/android/core/resources/ViraVideos.kt")
CRF = "20"


def resource_name(name: str) -> str:
    name = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()
    return re.sub(r"[^a-z0-9]+", "_", name).strip("_")


def enum_name(name: str) -> str:
    return "".join(part.capitalize() for part in name.split("_"))


def has_alpha(path: Path) -> bool:
    with av.open(str(path)) as container:
        frame = next(container.decode(container.streams.video[0]))
        return frame.format.name.startswith("yuva")


def layout_for(width: int, height: int) -> str:
    """`stacked` (color a dalt, alfa a sota) o `sideBySide` (color a l'esquerra)."""
    return "stacked" if max(width, height * 2) <= max(width * 2, height) else "sideBySide"


def convert(source: Path, target: Path) -> dict:
    alpha = has_alpha(source)
    with av.open(str(source)) as inp, av.open(str(target), "w", format="mp4",
                                             options={"movflags": "+faststart"}) as out:
        video_in = inp.streams.video[0]
        width, height = video_in.codec_context.width, video_in.codec_context.height
        rate = video_in.average_rate or Fraction(24)
        layout = layout_for(width, height) if alpha else None
        out_w = width * (2 if layout == "sideBySide" else 1)
        out_h = height * (2 if layout == "stacked" else 1)
        video_out = out.add_stream("libx264", rate=rate)
        video_out.width, video_out.height = out_w, out_h
        video_out.pix_fmt = "yuv420p"
        video_out.options = {"crf": CRF, "preset": "slow", "profile": "high", "tune": "animation"}
        audio_in = inp.streams.audio[0] if inp.streams.audio else None
        audio_out = out.add_stream_from_template(audio_in) if audio_in else None

        frames = 0
        streams = [video_in] + ([audio_in] if audio_in else [])
        for packet in inp.demux(*streams):
            if packet.stream is audio_in:
                if packet.dts is not None:
                    packet.stream = audio_out
                    out.mux(packet)
                continue
            for frame in packet.decode():
                if alpha:
                    rgba = frame.to_ndarray(format="rgba")
                    rgb, mask = rgba[..., :3], np.repeat(rgba[..., 3:4], 3, axis=2)
                    packed = np.concatenate([rgb, mask], axis=0 if layout == "stacked" else 1)
                    frame = av.VideoFrame.from_ndarray(np.ascontiguousarray(packed), format="rgb24")
                frame = frame.reformat(width=out_w, height=out_h, format="yuv420p")
                frame.pts = frames
                frame.time_base = 1 / rate
                out.mux(video_out.encode(frame))
                frames += 1
        out.mux(video_out.encode())
    return {"layout": layout, "width": width, "height": height, "rate": rate, "frames": frames}


def write_kotlin(entries: list[tuple[str, dict]]) -> str:
    lines = [
        "package cat.vira.android.core.resources",
        "",
        "import androidx.annotation.RawRes",
        "import cat.vira.android.R",
        "",
        "// Generat per scripts/export-videos.py. No es toca a mà.",
        "",
        "/// Com va empaquetada la transparència d'un vídeo: H.264 no té canal alfa, i els clips que",
        "/// a l'iPhone en porten surten amb el color i l'alfa (en grisos) a dues meitats del fotograma.",
        "enum class ViraVideoAlpha {",
        "    /// Sense transparència: el fotograma sencer és la imatge.",
        "    None,",
        "    /// Color a la meitat de dalt, alfa a la de sota.",
        "    Stacked,",
        "    /// Color a la meitat de l'esquerra, alfa a la de la dreta.",
        "    SideBySide,",
        "}",
        "",
        "/// Els vídeos de l'iPhone (`Vira/Resources/*.mov`), amb la mida de la imatge (no del fitxer).",
        "enum class ViraVideo(",
        "    @RawRes val resource: Int,",
        "    val alpha: ViraVideoAlpha,",
        "    val width: Int,",
        "    val height: Int,",
        "    val framesPerSecond: Double,",
        "    val frameCount: Int,",
        ") {",
    ]
    for name, info in entries:
        alpha = {"stacked": "Stacked", "sideBySide": "SideBySide", None: "None"}[info["layout"]]
        fps = float(info["rate"])
        lines.append(
            f"    {enum_name(name)}(R.raw.{name}, ViraVideoAlpha.{alpha}, {info['width']}, {info['height']}, "
            f"{fps!r}, {info['frames']}),"
        )
    lines[-1] = lines[-1][:-1] + ";"
    lines.append("}")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("iphone_repo", type=Path, help="camí al repositori de l'iPhone")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent.parent)
    args = parser.parse_args()

    raw = args.root / "app/src/main/res/raw"
    raw.mkdir(parents=True, exist_ok=True)
    entries = []
    for source in sorted((args.iphone_repo / "Vira/Resources").glob("*.mov")):
        name = resource_name(source.stem)
        info = convert(source, raw / f"{name}.mp4")
        entries.append((name, info))
        print(f"{name}: {info['layout'] or 'sense alfa'}, {info['frames']} fotogrames")
    (args.root / KOTLIN).write_text(write_kotlin(entries), encoding="utf-8")
    print(f"Exportats: {len(entries)} vídeos, {sum(1 for _, i in entries if i['layout'])} amb alfa")
    return 0


if __name__ == "__main__":
    sys.exit(main())
