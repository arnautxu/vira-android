#!/usr/bin/env python3
"""Exporta els textos de l'iPhone (Localizable.xcstrings) a recursos d'Android.

Els textos de Vira no s'escriuen a mà: la font de veritat és el catàleg de l'iPhone
(`Vira/Resources/Localizable.xcstrings` de arnautxu/VIRA-APP-I-WEB). Aquest script en llegeix
els tres idiomes i escriu `strings_vira.xml` a `values/` (català, l'idioma de partida),
`values-es/` i `values-en/`. Els fitxers generats no es toquen a mà: es tornen a generar.

    python3 scripts/export-strings.py ../VIRA-APP-I-WEB/Vira/Resources/Localizable.xcstrings

Com es converteix cada cosa:
- Clau: `account.email.signIn.title` → `account_email_sign_in_title`. Els marcadors de format
  de la clau (`a11y.items_count %lld`) cauen del nom.
- Format: `%@` → `%1$s`, `%lld`/`%d` → `%1$d`, sempre amb posició perquè Android ho demana
  amb més d'un argument. Un `%` solt d'un text amb arguments passa a `%%`.
- Plurals: `<plurals>` amb les mateixes quantitats. Android no fa servir mai `zero` en català,
  castellà ni anglès, així que el text del zero de l'iPhone surt també com a `<nom>_zero`, i
  `viraPluralString` (core/resources) el tria quan el recompte és 0, com fa l'iPhone.
- Les claus sense cap traducció (`""`, `", "`, `"%@"`) no són textos i no s'exporten.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from xml.sax.saxutils import escape

LANGUAGES = {"ca": "values", "es": "values-es", "en": "values-en"}
SOURCE_LANGUAGE = "ca"
OUTPUT_NAME = "strings_vira.xml"
PLURAL_QUANTITIES = ["zero", "one", "two", "few", "many", "other"]

# Paraules reservades de Java: un recurs no pot dir-se així (R.string.default no compila).
JAVA_KEYWORDS = set(
    "abstract assert boolean break byte case catch char class const continue default do double "
    "else enum extends final finally float for goto if implements import instanceof int "
    "interface long native new package private protected public return short static strictfp "
    "super switch synchronized this throw throws transient try void volatile while true false "
    "null".split()
)

FORMAT_SPEC = re.compile(r"%(?:(\d+)\$)?(?:ll|l)?([@dDuixXfeEgG])")


def resource_name(key: str) -> str:
    name = FORMAT_SPEC.sub("", key)
    name = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()
    name = re.sub(r"[^a-z0-9]+", "_", name).strip("_")
    if name[:1].isdigit() or name in JAVA_KEYWORDS:
        name = "s_" + name
    return name


def convert_format(value: str) -> str:
    """Passa els marcadors de l'iPhone als d'Android, numerats per ordre d'aparició."""
    counter = 0

    def replace(match: re.Match) -> str:
        nonlocal counter
        counter += 1
        position = match.group(1) or str(counter)
        kind = "s" if match.group(2) == "@" else ("f" if match.group(2) in "feEgG" else "d")
        return f"%{position}${kind}"

    # Primer s'amaguen els `%%` ja escapats perquè no es confonguin amb marcadors.
    value = value.replace("%%", "\0")
    converted = FORMAT_SPEC.sub(replace, value)
    if counter:
        converted = converted.replace("%", "%%")
        converted = re.sub(r"%%(\d+\$[sdf])", r"%\1", converted)
    return converted.replace("\0", "%%")


def android_escape(value: str) -> str:
    value = value.replace("\\", "\\\\")
    value = escape(value)
    value = value.replace("'", "\\'").replace('"', '\\"')
    value = value.replace("\n", "\\n").replace("\t", "\\t")
    if value[:1] in ("@", "?"):
        value = "\\" + value
    # Android retalla els espais dels extrems i en col·lapsa els seguits; aquests s'han de quedar.
    value = re.sub(r"^ +| +$", lambda m: "\\u0020" * len(m.group(0)), value)
    value = re.sub(r"(?<= ) +", lambda m: "\\u0020" * len(m.group(0)), value)
    return value


def localized(entry: dict, language: str):
    """Torna (text, plurals) d'una clau en un idioma; plurals és un dict quantitat → text."""
    localization = entry.get("localizations", {}).get(language)
    if not localization:
        return None, None
    if "stringUnit" in localization:
        return localization["stringUnit"]["value"], None
    plural = localization.get("variations", {}).get("plural")
    if plural:
        return None, {q: v["stringUnit"]["value"] for q, v in plural.items()}
    raise ValueError(f"variació no suportada: {list(localization.get('variations', {}))}")


def export(catalog: dict, source_label: str) -> dict:
    strings = catalog["strings"]
    names: dict[str, str] = {}
    for key, entry in strings.items():
        if not entry.get("localizations"):
            continue
        name = resource_name(key)
        if not name:
            raise ValueError(f"la clau {key!r} no dona cap nom de recurs")
        if name in names:
            raise ValueError(f"{key!r} i {names[name]!r} donen el mateix nom: {name}")
        names[name] = key

    files = {}
    for language, folder in LANGUAGES.items():
        lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            f"<!-- Generat per scripts/export-strings.py des de {source_label}. No es toca a mà. -->",
            "<resources>",
        ]
        for name in sorted(names):
            key = names[name]
            value, plurals = localized(strings[key], language)
            if value is None and plurals is None:
                value, plurals = localized(strings[key], SOURCE_LANGUAGE)
            if plurals is not None:
                lines.append(f'    <plurals name="{name}">')
                for quantity in PLURAL_QUANTITIES:
                    if quantity in plurals:
                        text = android_escape(convert_format(plurals[quantity]))
                        lines.append(f'        <item quantity="{quantity}">{text}</item>')
                lines.append("    </plurals>")
                if "zero" in plurals:
                    text = android_escape(convert_format(plurals["zero"]))
                    lines.append(f'    <string name="{name}_zero">{text}</string>')
            else:
                lines.append(f'    <string name="{name}">{android_escape(convert_format(value))}</string>')
        lines.append("</resources>")
        files[folder] = "\n".join(lines) + "\n"
    return files


def source_label(catalog_path: Path) -> str:
    try:
        commit = subprocess.run(
            ["git", "-C", str(catalog_path.parent), "log", "-1", "--format=%h", "--", catalog_path.name],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        commit = ""
    return f"Localizable.xcstrings de l'iPhone{f' (commit {commit})' if commit else ''}"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("catalog", type=Path, help="camí a Localizable.xcstrings de l'iPhone")
    parser.add_argument("--res", type=Path, default=Path(__file__).resolve().parent.parent / "app/src/main/res")
    parser.add_argument("--check", action="store_true", help="no escriu res; falla si els fitxers no estan al dia")
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    if catalog.get("sourceLanguage") != SOURCE_LANGUAGE:
        print(f"L'idioma de partida del catàleg no és el català: {catalog.get('sourceLanguage')}", file=sys.stderr)
        return 1

    files = export(catalog, source_label(args.catalog))
    stale = []
    for folder, content in files.items():
        path = args.res / folder / OUTPUT_NAME
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                stale.append(str(path))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
    if stale:
        print("No estan al dia amb el catàleg de l'iPhone:\n  " + "\n  ".join(stale), file=sys.stderr)
        return 1
    count = sum(1 for line in files["values"].splitlines() if line.startswith(("    <string ", "    <plurals ")))
    print(f"{'Al dia' if args.check else 'Exportats'}: {count} recursos × {len(files)} idiomes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
