"""Render legacy launcher PNGs (square + round) and the TV banner for SetEdit.

The adaptive icon uses vector drawables, but pre-API-26 launchers and the TV
banner need raster art. Both are drawn from the same "sliders" mark so every
surface stays visually consistent.
"""
from PIL import Image, ImageDraw

BRAND = (91, 76, 219, 255)      # #5B4CDB
WHITE = (255, 255, 255, 255)

# Launcher icon sizes in px, keyed by density bucket.
LAUNCHER_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def draw_mark(draw, size, scale, ox, oy):
    """Draw three sliders with offset knobs, scaled to `size`."""
    lw = max(2, int(size * 0.045))
    x0 = ox + size * 0.22 * scale
    x1 = ox + size * 0.78 * scale
    ys = [oy + size * (0.34 + 0.16 * i) * scale for i in range(3)]
    knobs = [0.42, 0.60, 0.36]
    for y in ys:
        draw.line([(x0, y), (x1, y)], fill=WHITE, width=lw)
    r = size * 0.072 * scale
    for y, k in zip(ys, knobs):
        cx = ox + size * (0.22 + 0.56 * k) * scale
        draw.ellipse([cx - r, y - r, cx + r, y + r], fill=WHITE)


def make_icon(size, round_icon):
    ss = 4  # supersample for smooth edges
    img = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    n = size * ss
    if round_icon:
        d.ellipse([0, 0, n - 1, n - 1], fill=BRAND)
    else:
        d.rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=BRAND)
    # Mark occupies the inner ~62% like an adaptive-icon safe zone.
    draw_mark(d, n, 0.62, n * 0.19, n * 0.19)
    return img.resize((size, size), Image.LANCZOS)


def make_banner(w=320, h=180):
    ss = 4
    img = Image.new("RGBA", (w * ss, h * ss), BRAND)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w * ss - 1, h * ss - 1], radius=int(h * ss * 0.12), fill=BRAND)
    mark = h * ss * 0.62
    ox = (w * ss - mark) / 2
    oy = (h * ss - mark) / 2
    lw = max(3, int(mark * 0.075))
    x0, x1 = ox + mark * 0.22, ox + mark * 0.78
    ys = [oy + mark * (0.34 + 0.16 * i) for i in range(3)]
    knobs = [0.42, 0.60, 0.36]
    for y in ys:
        d.line([(x0, y), (x1, y)], fill=WHITE, width=lw)
    r = mark * 0.072
    for y, k in zip(ys, knobs):
        cx = ox + mark * (0.22 + 0.56 * k)
        d.ellipse([cx - r, y - r, cx + r, y + r], fill=WHITE)
    return img.resize((w, h), Image.LANCZOS)


def main():
    base = "app/src/main/res"
    for bucket, size in LAUNCHER_SIZES.items():
        make_icon(size, False).save(f"{base}/mipmap-{bucket}/ic_launcher.png")
        make_icon(size, True).save(f"{base}/mipmap-{bucket}/ic_launcher_round.png")
        print("wrote", bucket, size)
    make_banner().save(f"{base}/mipmap-xhdpi/ic_banner.png")
    make_banner().save(f"{base}/mipmap-xhdpi/ic_banner_foreground.png")
    print("wrote banner")


if __name__ == "__main__":
    main()
