#!/usr/bin/env python3
"""Exporta les imatges, les fonts i els sons de l'iPhone a recursos d'Android.

Com `export-strings.py`, la font és el repositori de l'iPhone (arnautxu/VIRA-APP-I-WEB) i el que
s'escriu aquí no es toca a mà: es torna a generar.

    python3 scripts/export-assets.py ../VIRA-APP-I-WEB

- **Imatges** (`Assets.xcassets`, menys `AppIcon` i `TabIcons`, que són de l'esquelet): de cada
  conjunt es pren la mida més gran. `@3x` va a `drawable-xxhdpi`, `@2x` a `drawable-xhdpi` i una
  imatge sense escala (els fons grans) a `drawable-nodpi`, perquè Android no la reescali. La
  variant fosca va a `drawable-night-…`. Els PNG passen a WebP sense pèrdua (Android 10 el llegeix
  amb transparència i ocupa menys); els JPG es queden com són.
- **Fonts**: les tres Satoshi a `font/` (`satoshi_regular`, `satoshi_medium`, `satoshi_bold`).
- **Sons**: els `.caf` a OGG Vorbis a `raw/`, amb el mateix nom (`sound-desat` → `sound_desat`).
- **Mindfulness**: la veu (`Mindfulness/*.m4a`, AAC, que Android reprodueix tal qual) i el guió amb
  temps (`*.json`) es copien a `raw/` sense tocar-los: `mindfulness-breatheSlow-ca.m4a` →
  `mindfulness_breathe_slow_ca.m4a` i el guió → `mindfulness_breathe_slow_ca_script.json`. El
  guió porta `_script` perquè Android fa el nom del recurs sense l'extensió, i dos fitxers amb el
  mateix nom no compilen.

Els noms de recurs canvien els guions per guions baixos: `viri-hero` → `viri_hero`. Els vídeos
no són aquí: els `.mov` de Viri porten canal alfa en HEVC, que Android no reprodueix (vegeu el fil
de Textos i recursos).
"""

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image

SKIPPED_CATALOG_FOLDERS = {"AppIcon.appiconset", "TabIcons"}
SCALE_FOLDERS = {"3x": "xxhdpi", "2x": "xhdpi", "1x": "mdpi", None: "nodpi"}
SCALE_ORDER = ["3x", "2x", "1x", None]
GENERATED_MARK = ".generat-per-export-assets"


def resource_name(name: str) -> str:
    name = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()
    name = re.sub(r"[^a-z0-9]+", "_", name).strip("_")
    return name if name[:1].isalpha() else "img_" + name


def export_images(catalog: Path, res: Path) -> list[Path]:
    written, skipped = [], []
    names: dict[str, Path] = {}
    for imageset in sorted(catalog.rglob("*.imageset")):
        if any(part in SKIPPED_CATALOG_FOLDERS for part in imageset.relative_to(catalog).parts):
            continue
        name = resource_name(imageset.stem)
        if name in names:
            raise ValueError(f"{imageset} i {names[name]} donen el mateix nom: {name}")
        names[name] = imageset
        images = [i for i in json.loads((imageset / "Contents.json").read_text())["images"] if "filename" in i]
        for dark in (False, True):
            variants = [i for i in images if bool(i.get("appearances")) == dark]
            if not variants:
                continue
            best = min(variants, key=lambda i: SCALE_ORDER.index(i.get("scale")))
            source = imageset / best["filename"]
            if source.suffix.lower() not in (".png", ".jpg", ".jpeg"):
                skipped.append(f"{imageset.name} ({source.suffix})")
                continue
            folder = res / f"drawable{'-night' if dark else ''}-{SCALE_FOLDERS[best.get('scale')]}"
            folder.mkdir(parents=True, exist_ok=True)
            if source.suffix.lower() == ".png":
                target = folder / f"{name}.webp"
                Image.open(source).save(target, "WEBP", lossless=True, method=6)
            else:
                target = folder / f"{name}.jpg"
                shutil.copyfile(source, target)
            written.append(target)
    if skipped:
        print("Imatges que no són PNG ni JPG, no exportades: " + ", ".join(skipped), file=sys.stderr)
    return written


def export_fonts(fonts: Path, res: Path) -> list[Path]:
    folder = res / "font"
    folder.mkdir(parents=True, exist_ok=True)
    written = []
    for font in sorted(fonts.glob("*.otf")):
        target = folder / f"{resource_name(font.stem)}.otf"
        shutil.copyfile(font, target)
        written.append(target)
    return written


def export_sounds(resources: Path, res: Path) -> list[Path]:
    folder = res / "raw"
    folder.mkdir(parents=True, exist_ok=True)
    written = []
    for sound in sorted(resources.glob("*.caf")):
        target = folder / f"{resource_name(sound.stem)}.ogg"
        subprocess.run(
            ["ffmpeg", "-v", "error", "-y", "-i", str(sound), "-c:a", "libvorbis", "-q:a", "5",
             "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", str(target)],
            check=True,
        )
        written.append(target)
    return written


def export_mindfulness(resources: Path, res: Path) -> list[Path]:
    folder = res / "raw"
    written = []
    for source in sorted(resources.glob("Mindfulness/mindfulness-*")):
        suffix = {".m4a": "", ".json": "_script"}.get(source.suffix)
        if suffix is None:
            continue
        target = folder / f"{resource_name(source.stem)}{suffix}{source.suffix}"
        shutil.copyfile(source, target)
        written.append(target)
    return written


def clean_previous(res: Path) -> None:
    """Esborra el que va escriure l'export anterior, i només això, perquè no quedin restes."""
    manifest = res / GENERATED_MARK
    if manifest.exists():
        for line in manifest.read_text().splitlines():
            (res / line).unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("iphone_repo", type=Path, help="camí al repositori de l'iPhone")
    parser.add_argument("--res", type=Path, default=Path(__file__).resolve().parent.parent / "app/src/main/res")
    args = parser.parse_args()

    resources = args.iphone_repo / "Vira/Resources"
    clean_previous(args.res)
    images = export_images(resources / "Assets.xcassets", args.res)
    fonts = export_fonts(resources / "Fonts", args.res)
    sounds = export_sounds(resources, args.res)
    mindfulness = export_mindfulness(resources, args.res)

    written = images + fonts + sounds + mindfulness
    manifest = sorted(str(path.relative_to(args.res)) for path in written)
    (args.res / GENERATED_MARK).write_text("\n".join(manifest) + "\n")
    print(f"Exportats: {len(images)} imatges, {len(fonts)} fonts, {len(sounds)} sons, {len(mindfulness)} fitxers de mindfulness")
    return 0


if __name__ == "__main__":
    sys.exit(main())
