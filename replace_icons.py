#!/usr/bin/env python3
"""
Replace remaining NetBird icons with the Unblockr logo.

Already replaced (skipping):
  - mipmap-*/ic_launcher.webp        (all densities) - done
  - mipmap-*/ic_launcher_round.webp  (all densities) - done
  - mipmap-*/ic_launcher_foreground.webp (all densities) - done
  - drawable/connect_logo.png - done

Still needs replacing:
  - mipmap-*/ic_banner.webp (all 5 densities) - NetBird blue banner
  - drawable/nb_nav_logo.png - NetBird wordmark
  - drawable/ic_netbird_btn.png - NetBird bird icon
  - mipmap-xhdpi/ic_mask_bg.png - NetBird shape mask
"""

import sys
import os

try:
    from PIL import Image, ImageDraw
except ImportError:
    print("Pillow not found. Installing...")
    os.system(f"{sys.executable} -m pip install Pillow")
    from PIL import Image, ImageDraw

BASE = r"C:\Users\mark\StudioProjects\android-client\app\src\main\res"
SOURCE = os.path.join(BASE, "drawable-xxhdpi", "activation_on.png")

src = Image.open(SOURCE).convert("RGBA")
print(f"Source image size: {src.size}")


def make_square(img, size):
    """Resize image to fit within size x size, transparent background, centred."""
    img_copy = img.copy()
    img_copy.thumbnail((size, size), Image.LANCZOS)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = ((size - img_copy.width) // 2, (size - img_copy.height) // 2)
    result.paste(img_copy, offset, img_copy)
    return result


def make_banner(img, width, height):
    """
    TV banner: wide rectangle on Unblockr dark background (#12121A).
    The logo is centred and scaled to 75% of the banner height.
    """
    bg = Image.new("RGBA", (width, height), (18, 18, 26, 255))  # #12121A
    inner_h = int(height * 0.75)
    resized = make_square(img, inner_h)
    x = (width - resized.width) // 2
    y = (height - resized.height) // 2
    bg.paste(resized, (x, y), resized)
    return bg


def save_webp(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "WEBP", lossless=True, quality=100)
    print(f"  Saved: {path}")


def save_png(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG")
    print(f"  Saved: {path}")


# ---------------------------------------------------------------------------
# Banner icons (TV banners)
# Standard TV banner dp size is 320x180dp; scaled per density:
#   mdpi=320x180, hdpi=480x270, xhdpi=640x360, xxhdpi=960x540, xxxhdpi=1280x720
# ---------------------------------------------------------------------------
banner_sizes = {
    "mdpi":    (320, 180),
    "hdpi":    (480, 270),
    "xhdpi":   (640, 360),
    "xxhdpi":  (960, 540),
    "xxxhdpi": (1280, 720),
}

print("\n--- Banner icons (ic_banner.webp) ---")
for density, (w, h) in banner_sizes.items():
    img = make_banner(src, w, h)
    save_webp(img, os.path.join(BASE, f"mipmap-{density}", "ic_banner.webp"))

# ---------------------------------------------------------------------------
# PNG logo / button files
# ---------------------------------------------------------------------------
print("\n--- PNG logo files ---")

# nb_nav_logo.png - navigation drawer logo
# Original was a wide wordmark; we replace with the padlock logo at a square size.
# Using 96px (xhdpi equivalent) - the drawable/ folder is density-independent.
nav_logo = make_square(src, 96)
save_png(nav_logo, os.path.join(BASE, "drawable", "nb_nav_logo.png"))

# ic_netbird_btn.png - small button/toolbar icon, was ~48px bird mark
btn_icon = make_square(src, 48)
save_png(btn_icon, os.path.join(BASE, "drawable", "ic_netbird_btn.png"))

# mipmap-xhdpi/ic_mask_bg.png - adaptive icon mask background shape
# The original was a rounded-rect mask used for the adaptive icon safe zone.
# Replace with the logo at the xhdpi launcher size (96px).
mask_bg = make_square(src, 96)
save_png(mask_bg, os.path.join(BASE, "mipmap-xhdpi", "ic_mask_bg.png"))

print("\nAll done.")
