# /// script
# requires-python = ">=3.11"
# dependencies = ["pillow>=11", "cairosvg>=2.7"]
# ///
"""Builds the bundled exercise illustrations from two open line-art collections (CC BY-SA 4.0).

Every predefined exercise in ExerciseSeed.kt names an illustration with `art = "<slug>"`:
- `<slug>`: Everkinetic (https://github.com/everkinetic/data), start and end position;
- `wg-<slug>`: Workout Guide by Bryl Lim (https://github.com/bryllim/workout-guide), which extends
  Everkinetic in the same style; the frames used are listed in WORKOUT_GUIDE_FRAMES.
Both are downloaded from pinned commits. Each drawing becomes an alpha mask (so the app tints it
with the theme colour), is trimmed, scaled down and written to
`app/src/main/assets/exercises/<art>-1.webp` (and `-2.webp` when there is an end position).

Usage (from the repository root):  uv run tools/exercise_art.py [--check]
"""

from __future__ import annotations

import io
import re
import sys
import urllib.request
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parent.parent
SEED = ROOT / "app/src/main/java/io/github/luannamorim/myfit/data/db/seed/ExerciseSeed.kt"
OUT = ROOT / "app/src/main/assets/exercises"

EVERKINETIC_SHA = "446bb9a3d0c3beb6b84f7c9d77dfc8af707a2ab6"
EK_RAW = f"https://raw.githubusercontent.com/everkinetic/data/{EVERKINETIC_SHA}"

WORKOUT_GUIDE_SHA = "aac599224bb9780305239607ef98540b7e0ce389"
WG_RAW = f"https://raw.githubusercontent.com/bryllim/workout-guide/{WORKOUT_GUIDE_SHA}/packages/workout-guide/assets"

# Workout Guide has three frames per exercise; these are the (start, end) frames used. The
# skipped ones repeat a position or were traced with doubled outlines. A static hold has one.
WORKOUT_GUIDE_FRAMES = {
    "burpee": (1, 3),
    "cable-lateral-raise": (2, 1),
    "cycling": (1, 3),
    "elliptical": (1, 2),
    "face-pull": (3, 1),
    "farmer-carry": (1, 3),
    "goblet-squat": (3, 1),
    "hip-thrust": (3, 1),
    "jump-rope": (1, 2),
    "kettlebell-romanian-deadlift": (3, 1),
    "kettlebell-swing": (3, 1),
    "machine-row": (3, 1),
    "pike-push-up": (3, 1),
    "plank": (1,),
    "rowing": (3, 1),
    "running": (1, 2),
    "single-leg-calf-raise": (1, 2),
    "single-leg-romanian-deadlift": (1,),
    "stair-climber": (1, 3),
    "wrist-curl": (1, 2),
}

# Slugs whose drawings live only under dist/png/<id>-{relaxation,tension}.png.
DIST_ONLY = {
    "biceps-curls-with-barbell": "0211",
    "alternating-biceps-curl-with-dumbbell": "0223",
    "biceps-hammer-curl-with-dumbbell": "0227",
    "concentration-curls-with-dumbbell": "0220",
    "cross-body-crunch": "0289",
    "seated-calf-raise-using-machine": "0279",
    "triceps-kickback-with-dumbbell": "0204",
    "tate-press-with-dumbbell": "0203",
    "biceps-curl-with-machine": "0253",
    "decline-crunch": "0292",
}

# Everkinetic drawings missing from the repository, taken from the copies published by wger.de
# (same artwork, licensed there as CC BY-SA 3.0).
WGER = {
    "bent-over-row-with-barbell": (
        "https://wger.de/media/exercise-images/109/Barbell-rear-delt-row-1.png",
        "https://wger.de/media/exercise-images/109/Barbell-rear-delt-row-2.png",
    ),
}

MAX_SIDE = 480  # px; drawn at most ~200 dp wide on screen
MARGIN = 0.04


def slugs() -> list[str]:
    found = re.findall(r'art\s*=\s*"([a-z0-9-]+)"', SEED.read_text(encoding="utf-8"))
    return sorted(set(found))


def sources(slug: str) -> tuple[str, ...]:
    if slug.startswith("wg-"):
        name = slug.removeprefix("wg-")
        return tuple(f"{WG_RAW}/{name}/frame-{i}.svg" for i in WORKOUT_GUIDE_FRAMES[name])
    if slug in WGER:
        return WGER[slug]
    if slug in DIST_ONLY:
        n = DIST_ONLY[slug]
        return f"{EK_RAW}/dist/png/{n}-relaxation.png", f"{EK_RAW}/dist/png/{n}-tension.png"
    return f"{EK_RAW}/src/images-web/{slug}-1.png", f"{EK_RAW}/src/images-web/{slug}-2.png"


def fetch(url: str) -> Image.Image:
    req = urllib.request.Request(url, headers={"User-Agent": "myfit-build-tools"})
    with urllib.request.urlopen(req, timeout=60) as r:
        data = r.read()
    if url.endswith(".svg"):
        import cairosvg  # Workout Guide frames: white shapes on transparency

        rgba = Image.open(io.BytesIO(cairosvg.svg2png(bytestring=data, output_width=1024, output_height=1024)))
        mask = rgba.convert("RGBA").getchannel("A")
        mask.info["mask"] = True
        return mask
    return Image.open(io.BytesIO(data)).copy()


def to_mask(img: Image.Image) -> Image.Image:
    """Black lines on white (or a ready mask) → opaque lines on transparency, trimmed and scaled."""
    if img.mode == "L" and img.info.get("mask"):
        alpha = img
    else:
        if img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info):
            rgba = img.convert("RGBA")
            white = Image.new("RGBA", rgba.size, "white")
            white.alpha_composite(rgba)
            img = white
        lum = img.convert("L")
        alpha = ImageOps.invert(lum).point(lambda v: 0 if v < 24 else min(255, int(v * 1.15)))
    box = alpha.getbbox()
    if box is None:
        raise ValueError("empty drawing")
    alpha = alpha.crop(box)
    pad = int(max(alpha.size) * MARGIN)
    canvas = Image.new("L", (alpha.width + 2 * pad, alpha.height + 2 * pad), 0)
    canvas.paste(alpha, (pad, pad))
    canvas.thumbnail((MAX_SIDE, MAX_SIDE), Image.Resampling.LANCZOS)
    # 16 alpha levels are plenty for anti-aliased line art and halve the file size.
    step = 255 / 15
    canvas = canvas.point(lambda v: int(round(round(v / step) * step)))
    out = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    out.putalpha(canvas)
    return out


def main() -> int:
    check = "--check" in sys.argv
    wanted = slugs()
    OUT.mkdir(parents=True, exist_ok=True)
    expected = {f"{s}-{i}.webp" for s in wanted for i in range(1, len(sources(s)) + 1)}
    present = {p.name for p in OUT.glob("*.webp")}
    if check:
        missing, extra = expected - present, present - expected
        for name in sorted(missing):
            print("missing", name)
        for name in sorted(extra):
            print("unused", name)
        return 1 if missing or extra else 0
    for name in sorted(present - expected):
        (OUT / name).unlink()
    for slug in wanted:
        for i, url in enumerate(sources(slug), start=1):
            target = OUT / f"{slug}-{i}.webp"
            if target.exists():
                continue
            to_mask(fetch(url)).save(target, "WEBP", lossless=True, quality=50, method=4)
            print("wrote", target.relative_to(ROOT))
    total = sum(p.stat().st_size for p in OUT.glob("*.webp"))
    print(f"{len(wanted)} illustrations, {total / 1024:.0f} KiB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
