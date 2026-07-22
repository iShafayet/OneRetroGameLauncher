#!/usr/bin/env python3
"""
Build app/src/main/assets/arcade/rom_map.json from FBNeo (+ optional MAME) DAT files.

Usage:
  python3 scripts/generate_arcade_rom_map.py
  python3 scripts/generate_arcade_rom_map.py --fbneo-dat /path/to/fbneo.dat
  python3 scripts/generate_arcade_rom_map.py --fbneo-dat fbneo.dat --mame-dat "MAME 0.280 (arcade).dat"

Downloads FBNeo arcade DAT when --fbneo-dat is omitted.
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

FBNEO_DAT_URL = (
    "https://raw.githubusercontent.com/libretro/FBNeo/master/dats/"
    "FinalBurn%20Neo%20(ClrMame%20Pro%20XML%2C%20Arcade%20only).dat"
)
SPEC_VERSION = 1
REPO_ROOT = Path(__file__).resolve().parents[1]
OUT_PATH = REPO_ROOT / "app/src/main/assets/arcade/rom_map.json"

# MAME device zips that are not marked isbios in FBNeo but should not appear as games.
EXTRA_IGNORE = frozenset(
    {
        "chdman",
        "hlsl",
        "mame",
        "mess",
        "unibios",
    }
)


def download(url: str, dest: Path) -> None:
    print(f"Downloading {url} …")
    dest.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve(url, dest)


def parse_dat(path: Path) -> tuple[dict[str, str], set[str]]:
    titles: dict[str, str] = {}
    ignore: set[str] = set()
    root = ET.parse(path).getroot()
    for game in root.findall("game"):
        name = (game.get("name") or "").strip().lower()
        if not name:
            continue
        desc_el = game.find("description")
        description = (desc_el.text or "").strip() if desc_el is not None else ""
        is_bios = game.get("isbios") == "yes"
        is_device = game.get("isdevice") == "yes"
        if is_bios or is_device:
            ignore.add(name)
            continue
        if description:
            titles[name] = description
        else:
            titles.setdefault(name, name)
    return titles, ignore


def merge_mame(titles: dict[str, str], ignore: set[str], path: Path) -> None:
    mame_titles, mame_ignore = parse_dat(path)
    ignore.update(mame_ignore)
    for key, value in mame_titles.items():
        titles.setdefault(key, value)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate bundled arcade ROM map JSON")
    parser.add_argument("--fbneo-dat", type=Path, help="FBNeo ClrMame Pro XML (arcade)")
    parser.add_argument("--mame-dat", type=Path, help="Optional MAME arcade DAT to merge")
    parser.add_argument("--out", type=Path, default=OUT_PATH)
    args = parser.parse_args()

    cache_dir = REPO_ROOT / "scripts/.cache"
    cache_dir.mkdir(parents=True, exist_ok=True)

    fbneo_path = args.fbneo_dat
    if fbneo_path is None:
        fbneo_path = cache_dir / "fbneo_arcade.dat"
        if not fbneo_path.is_file():
            download(FBNEO_DAT_URL, fbneo_path)
    elif not fbneo_path.is_file():
        print(f"FBNeo DAT not found: {fbneo_path}", file=sys.stderr)
        return 1

    titles, ignore = parse_dat(fbneo_path)
    ignore.update(EXTRA_IGNORE)

    sources = [f"fbneo:{fbneo_path.name}"]
    if args.mame_dat:
        if not args.mame_dat.is_file():
            print(f"MAME DAT not found: {args.mame_dat}", file=sys.stderr)
            return 1
        merge_mame(titles, ignore, args.mame_dat)
        sources.append(f"mame:{args.mame_dat.name}")

    payload = {
        "specVersion": SPEC_VERSION,
        "sources": sources,
        "ignore": sorted(ignore),
        "titles": dict(sorted(titles.items())),
    }

    args.out.parent.mkdir(parents=True, exist_ok=True)
    raw = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    args.out.write_text(raw + "\n", encoding="utf-8")
    print(
        f"Wrote {args.out} ({len(raw):,} bytes, "
        f"{len(titles):,} titles, {len(ignore):,} ignored)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
