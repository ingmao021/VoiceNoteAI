"""Genera los recursos de felpa de la app Wear a partir de docs/propuesta_diseño.png.

Uso (desde la raíz del repo, con Pillow instalado):
    python .claude/skills/wear-ui-ux/scripts/build_assets.py docs/propuesta_diseño.png wear/src/main/res/drawable-nodpi

Produce:
    fur_background.webp   fondo crema sin el disco ni el texto (redondo, esquinas transparentes)
    patch_idle.webp       disco azul con el micrófono bordado, tal cual el diseño
    patch_<estado>.webp   disco de felpa liso teñido con el color de cada estado (el icono lo pinta Compose)

Las coordenadas son las de la imagen de 2000 px; si cambia el diseño, se ajustan las constantes.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

# Geometría medida sobre propuesta_diseño.png (2000 x 2000).
SCREEN_CENTER, SCREEN_RADIUS = (994, 998), 985
DISK_CENTER, DISK_RADIUS = (1005, 840), 500
TEXT_BOX = (500, 1405, 1490, 1530)

BACKGROUND_SIZE = 480
PATCH_SIZE = 320

# Colores de cada estado (deben coincidir con FurPalette en Theme.kt).
STATE_COLORS = {
    "recording": (0xD6, 0x3A, 0x3A),
    "processing": (0xE0, 0x9A, 0x14),
    "success": (0x2E, 0x8B, 0x57),
    "error": (0xC2, 0x45, 0x1C),
}
SOURCE_BLUE = (33, 88, 162)  # media de la felpa azul del diseño


def circle_mask(size, center, radius, feather=0):
    mask = Image.new("L", size, 0)
    cx, cy = center
    ImageDraw.Draw(mask).ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=255)
    return mask.filter(ImageFilter.GaussianBlur(feather)) if feather else mask


def remove_text(img):
    """Tapa el texto con la felpa de justo debajo, volteada, y un borde difuso."""
    x0, y0, x1, y1 = TEXT_BOX
    height = y1 - y0
    patch = img.crop((x0, y1, x1, y1 + height)).transpose(Image.FLIP_TOP_BOTTOM)
    mask = Image.new("L", patch.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((12, 12, patch.width - 12, patch.height - 12), 40, fill=255)
    img.paste(patch, (x0, y0), mask.filter(ImageFilter.GaussianBlur(10)))


def remove_disk(img):
    """Rellena el disco con felpa del anillo exterior, reflejada en sentido radial."""
    src = img.copy()
    spx, dpx = src.load(), img.load()
    cx, cy = DISK_CENTER
    inner = DISK_RADIUS + 40  # incluye el halo de sombra del disco
    for y in range(cy - inner, cy + inner):
        for x in range(cx - inner, cx + inner):
            dx, dy = x - cx, y - cy
            r = (dx * dx + dy * dy) ** 0.5
            if r >= inner:
                continue
            # r en [0, inner) se lleva a [inner + 10, inner + 10 + 0.55 * inner): siempre dentro de la pantalla.
            target = inner + 10 + (inner - r) * 0.55
            k = target / max(r, 1)
            sx, sy = int(cx + dx * k), int(cy + dy * k)
            dpx[x, y] = spx[sx, sy]
    # Suaviza la costura del reflejo.
    ring = circle_mask(img.size, DISK_CENTER, inner + 6, 8)
    blurred = img.filter(ImageFilter.GaussianBlur(1.2))
    img.paste(blurred, (0, 0), ImageChops.subtract(ring, circle_mask(img.size, DISK_CENTER, inner - 30, 8)))


def disk_alpha(crop):
    """Alfa del disco: azul = opaco, crema = transparente; dentro del disco todo opaco (incluido el micrófono)."""
    px = crop.load()
    alpha = Image.new("L", crop.size)
    ap = alpha.load()
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b = px[x, y]
            ap[x, y] = max(0, min(255, int((b - r - 10) * 255 / 70)))
    core = circle_mask(crop.size, (crop.width // 2, crop.height // 2), int(crop.width * 0.42), 6)
    return ImageChops.lighter(alpha, core).filter(ImageFilter.GaussianBlur(0.8))


def remove_microphone(crop):
    """Cambia el micrófono bordado (y su sombra) por felpa del mismo radio en otro ángulo.

    La felpa está iluminada en sentido radial, así que rotar alrededor del centro conserva el tono
    y la dirección del pelo; si ningún ángulo cae fuera del bordado, se prueba un radio mayor.
    """
    spx = crop.load()
    cx, cy = crop.width // 2, crop.height // 2
    half_band = int(crop.width * 0.19)  # el micrófono ocupa una franja vertical central
    mask = Image.new("L", crop.size, 0)
    mpx = mask.load()
    for y in range(int(crop.height * 0.2), int(crop.height * 0.8)):
        for x in range(cx - half_band, cx + half_band):
            r, g, b = spx[x, y]
            if b - r < 40:  # no es azul: es bordado
                mpx[x, y] = 255
    # Se ensancha la máscara para llevarse también la sombra del bordado.
    mask = mask.filter(ImageFilter.MaxFilter(25))
    mpx = mask.load()
    filler = crop.copy()
    fpx = filler.load()
    angles = [math.radians(a) for a in (90, -90, 60, -60, 120, -120, 30, -30, 150, -150, 180)]
    for y in range(crop.height):
        for x in range(crop.width):
            if not mpx[x, y]:
                continue
            dx, dy = x - cx, y - cy
            radius, theta = math.hypot(dx, dy), math.atan2(dy, dx)
            for extra in range(0, 400, 20):
                hit = None
                for a in angles:
                    sx = int(cx + (radius + extra) * math.cos(theta + a))
                    sy = int(cy + (radius + extra) * math.sin(theta + a))
                    if 0 <= sx < crop.width and 0 <= sy < crop.height and not mpx[sx, sy]:
                        hit = spx[sx, sy]
                        break
                if hit:
                    fpx[x, y] = hit
                    break
    crop.paste(filler, (0, 0), mask.filter(ImageFilter.GaussianBlur(5)))
    # En el centro ningún ángulo sirve y la rotación deja un remolino: se tapa con un trozo de felpa limpia
    # del lado izquierdo del disco, girado 90° para que el pelo siga la misma dirección que alrededor.
    patch_radius = int(crop.width * 0.17)
    sx = cx - int(crop.width * 0.29)
    clean = crop.crop((sx - patch_radius, cy - patch_radius, sx + patch_radius, cy + patch_radius))
    clean = clean.rotate(-90)
    feather = circle_mask(clean.size, (patch_radius, patch_radius), patch_radius - 14, 10)
    crop.paste(clean, (cx - patch_radius, cy - patch_radius), feather)
    return crop


def tint(crop, color):
    """Conserva la textura (luminancia relativa) y cambia el color base."""
    src_lum = sum(SOURCE_BLUE) / 3
    px = crop.load()
    out = crop.copy()
    opx = out.load()
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b = px[x, y]
            f = ((r + g + b) / 3) / src_lum
            opx[x, y] = tuple(max(0, min(255, int(c * f))) for c in color)
    return out


def main(source, out_dir):
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    img = Image.open(source).convert("RGB")

    # Disco de INICIAL, tal cual.
    cx, cy = DISK_CENTER
    box = (cx - DISK_RADIUS, cy - DISK_RADIUS, cx + DISK_RADIUS, cy + DISK_RADIUS)
    disk = img.crop(box)
    alpha = disk_alpha(disk)
    idle = disk.convert("RGBA")
    idle.putalpha(alpha)
    idle.resize((PATCH_SIZE, PATCH_SIZE), Image.LANCZOS).save(out / "patch_idle.webp", "WEBP", quality=85, method=6)

    # Discos lisos teñidos.
    plain = remove_microphone(disk.copy())
    for name, color in STATE_COLORS.items():
        tinted = tint(plain, color).convert("RGBA")
        tinted.putalpha(alpha)
        tinted.resize((PATCH_SIZE, PATCH_SIZE), Image.LANCZOS).save(
            out / f"patch_{name}.webp", "WEBP", quality=85, method=6
        )

    # Fondo sin texto ni disco, recortado al círculo de la pantalla.
    remove_text(img)
    remove_disk(img)
    sx, sy = SCREEN_CENTER
    box = (sx - SCREEN_RADIUS, sy - SCREEN_RADIUS, sx + SCREEN_RADIUS, sy + SCREEN_RADIUS)
    bg = img.crop(box).convert("RGBA")
    bg.putalpha(circle_mask(bg.size, (SCREEN_RADIUS, SCREEN_RADIUS), SCREEN_RADIUS - 2, 2))
    bg.resize((BACKGROUND_SIZE, BACKGROUND_SIZE), Image.LANCZOS).save(
        out / "fur_background.webp", "WEBP", quality=80, method=6
    )


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
