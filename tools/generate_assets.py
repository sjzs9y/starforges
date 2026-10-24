"""Generate the placeholder assets of the Starforge mod.

The script writes:
  * 16x16 item / block textures (pixel art drawn procedurally)
  * the two 64x32 armor layer textures
  * item / block models and blockstates
  * the language files (en_us, zh_cn)
  * crafting recipes and the block loot table

Run with:  python tools/generate_assets.py
Requires Pillow (bundled with most Python distributions).
"""

from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "starforge"
DATA = ROOT / "src" / "main" / "resources" / "data" / "starforge"

NAMESPACE = "starforge"

# --------------------------------------------------------------------------- palette

DARK = (22, 20, 40, 255)
SHADE = (46, 44, 84, 255)
MID = (78, 84, 152, 255)
LIGHT = (126, 146, 228, 255)
GLOW = (150, 240, 255, 255)
WOOD = (110, 76, 46, 255)
WOOD_DARK = (68, 46, 28, 255)

TOOL_IDS = ["starforge_sword", "starforge_pickaxe", "starforge_axe", "starforge_shovel", "starforge_hoe"]
ARMOR_IDS = ["starforge_helmet", "starforge_chestplate", "starforge_leggings", "starforge_boots"]

EN = {
    "whisper": "Whisper",
    "starforge_ingot": "Starforged Ingot",
    "starforge_block": "Block of Starforged Metal",
    "starforge_sword": "Starforged Sword",
    "starforge_pickaxe": "Starforged Pickaxe",
    "starforge_axe": "Starforged Axe",
    "starforge_shovel": "Starforged Shovel",
    "starforge_hoe": "Starforged Hoe",
    "starforge_helmet": "Starforged Helmet",
    "starforge_chestplate": "Starforged Chestplate",
    "starforge_leggings": "Starforged Leggings",
    "starforge_boots": "Starforged Boots",
}

ZH = {
    "whisper": "轻语轻扬",
    "starforge_ingot": "星辉锭",
    "starforge_block": "星辉金属块",
    "starforge_sword": "星辉剑",
    "starforge_pickaxe": "星辉镐",
    "starforge_axe": "星辉斧",
    "starforge_shovel": "星辉锹",
    "starforge_hoe": "星辉锄",
    "starforge_helmet": "星辉头盔",
    "starforge_chestplate": "星辉胸甲",
    "starforge_leggings": "星辉护腿",
    "starforge_boots": "星辉靴子",
}


# --------------------------------------------------------------------------- drawing helpers


def canvas(size: int = 16) -> Image.Image:
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def pixel_poly(draw: ImageDraw.ImageDraw, points, color):
    draw.polygon(points, fill=color)


def pixel_line(draw: ImageDraw.ImageDraw, points, color, width=1):
    draw.line(points, fill=color, width=width, joint="curve")


def bevel(img: Image.Image, strength: int = 50) -> Image.Image:
    """Cheap automatic bevel: lighten pixels whose upper neighbour is empty, darken lower ones."""
    w, h = img.size
    src = img.copy()
    px = src.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            above = px[x, y - 1] if y > 0 else (0, 0, 0, 0)
            below = px[x, y + 1] if y < h - 1 else (0, 0, 0, 0)
            if above[3] == 0:
                r, g, b = _mix((r, g, b), LIGHT, strength / 100)
            elif below[3] == 0:
                r, g, b = _mix((r, g, b), DARK, strength / 100)
            px[x, y] = (r, g, b, a)
    return img


def _mix(a, b, t: float):
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
    )


def outline(img: Image.Image, color=DARK) -> Image.Image:
    """Add a 1px outline around every opaque region (outside only)."""
    w, h = img.size
    src = img.copy()
    px = src.load()
    out = img.copy()
    op = out.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] != 0:
                continue
            neighbours = [
                (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1),
            ]
            for nx, ny in neighbours:
                if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] != 0:
                    op[x, y] = color
                    break
    return out


def noise(img: Image.Image, amount: int = 10, seed: int = 0) -> Image.Image:
    rng = random.Random(seed)
    w, h = img.size
    px = img.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            d = rng.randint(-amount, amount)
            px[x, y] = (
                max(0, min(255, r + d)),
                max(0, min(255, g + d)),
                max(0, min(255, b + d)),
                a,
            )
    return img


# --------------------------------------------------------------------------- item textures


def tex_ingot() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(2, 10), (11, 5), (14, 8), (5, 13)], fill=MID)
    d.polygon([(2, 10), (11, 5), (12, 6), (3, 11)], fill=LIGHT)
    d.line([(13, 8), (5, 13)], fill=SHADE, width=1)
    # little spark
    d.point((6, 9), fill=GLOW)
    d.point((7, 9), fill=GLOW)
    d.point((6, 8), fill=GLOW)
    img = bevel(img, 35)
    return outline(img)


def tex_block() -> Image.Image:
    img = Image.new("RGBA", (16, 16), SHADE)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=DARK)
    d.rectangle([2, 2, 13, 13], fill=MID, outline=DARK)
    d.line([(2, 2), (13, 2)], fill=LIGHT)
    d.line([(2, 2), (2, 13)], fill=LIGHT)
    d.line([(2, 13), (13, 13)], fill=SHADE)
    d.line([(13, 2), (13, 13)], fill=SHADE)
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        d.point((cx, cy), fill=LIGHT)
    # four pointed star in the middle
    d.point((7, 5), fill=GLOW)
    d.point((8, 5), fill=GLOW)
    d.point((6, 6), fill=GLOW)
    d.point((7, 6), fill=GLOW)
    d.point((8, 6), fill=GLOW)
    d.point((9, 6), fill=GLOW)
    d.line([(7, 7), (8, 7)], fill=GLOW)
    d.line([(7, 7), (8, 7)], fill=GLOW)
    d.line([(7, 8), (7, 10)], fill=MID)
    d.line([(6, 7), (9, 7)], fill=GLOW)
    d.point((7, 8), fill=GLOW)
    d.point((8, 8), fill=GLOW)
    img = noise(img, 6, 7)
    return img


def tex_sword() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    # blade
    d.line([(5, 11), (13, 3)], fill=MID, width=3)
    d.line([(5, 10), (12, 3)], fill=LIGHT, width=1)
    # guard
    d.line([(2, 9), (6, 13)], fill=SHADE, width=2)
    # handle
    d.line([(4, 12), (2, 14)], fill=WOOD, width=2)
    d.point((1, 15), fill=WOOD_DARK)
    img = bevel(img, 45)
    return outline(img)


def tex_pickaxe() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.arc([1, 0, 15, 12], start=200, end=340, fill=MID, width=3)
    d.arc([3, 2, 13, 12], start=205, end=335, fill=LIGHT, width=1)
    d.line([(8, 7), (3, 14)], fill=WOOD, width=2)
    d.point((13, 4), fill=LIGHT)
    d.point((3, 5), fill=LIGHT)
    img = bevel(img, 40)
    return outline(img)


def tex_axe() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(8, 2), (13, 2), (15, 6), (12, 9), (8, 9)], fill=MID)
    d.polygon([(13, 2), (15, 6), (12, 9)], fill=LIGHT)
    d.line([(9, 3), (9, 8)], fill=SHADE)
    d.line([(7, 6), (2, 13)], fill=WOOD, width=2)
    img = bevel(img, 40)
    return outline(img)


def tex_shovel() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(9, 2), (13, 2), (14, 6), (11, 9), (9, 7)], fill=MID)
    d.polygon([(9, 2), (11, 2), (12, 6), (11, 7)], fill=LIGHT)
    d.line([(9, 8), (3, 14)], fill=WOOD, width=2)
    img = bevel(img, 40)
    return outline(img)


def tex_hoe() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(9, 2), (14, 2), (14, 4), (12, 5), (9, 5)], fill=MID)
    d.line([(9, 2), (14, 2)], fill=LIGHT)
    d.line([(10, 5), (4, 14)], fill=WOOD, width=2)
    img = bevel(img, 40)
    return outline(img)


def tex_helmet() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=MID)
    d.rectangle([4, 8, 11, 13], fill=(0, 0, 0, 0))
    d.arc([2, 2, 13, 13], start=180, end=360, fill=LIGHT, width=1)
    d.line([(4, 8), (11, 8)], fill=SHADE)
    d.point((5, 6), fill=GLOW)
    d.point((10, 6), fill=GLOW)
    img = bevel(img, 35)
    return outline(img)


def tex_chestplate() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(4, 2), (6, 2), (7, 4), (8, 4), (9, 2), (11, 2), (13, 5), (12, 13), (3, 13), (2, 5)], fill=MID)
    d.line([(3, 5), (5, 3)], fill=LIGHT)
    d.line([(3, 5), (3, 12)], fill=LIGHT)
    d.line([(5, 9), (11, 9)], fill=SHADE)
    d.point((7, 10), fill=GLOW)
    d.point((8, 10), fill=GLOW)
    img = bevel(img, 35)
    return outline(img)


def tex_leggings() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 2, 12, 5], fill=MID)
    d.rectangle([3, 6, 6, 14], fill=MID)
    d.rectangle([9, 6, 12, 14], fill=MID)
    d.line([(3, 2), (12, 2)], fill=LIGHT)
    d.line([(3, 2), (3, 13)], fill=LIGHT)
    d.line([(4, 5), (11, 5)], fill=SHADE)
    img = bevel(img, 30)
    return outline(img)


def tex_boots() -> Image.Image:
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(5, 2), (8, 2), (8, 11), (12, 11), (12, 13), (5, 13)], fill=MID)
    d.polygon([(2, 2), (5, 2), (5, 13), (1, 13), (1, 11), (2, 11)], fill=MID)
    d.line([(5, 2), (8, 2)], fill=LIGHT)
    d.line([(2, 2), (2, 12)], fill=LIGHT)
    d.line([(5, 11), (12, 11)], fill=SHADE)
    img = bevel(img, 30)
    return outline(img)


# --------------------------------------------------------------------------- armor layers


def armor_layer(seed: int) -> Image.Image:
    """64x32 armor texture: flat colour plus a bit of grain and a highlight trim."""
    img = Image.new("RGBA", (64, 32), MID)
    px = img.load()
    rng = random.Random(seed)
    for y in range(32):
        for x in range(64):
            base = MID
            if (x + y) % 7 == 0:
                base = SHADE
            d = rng.randint(-8, 8)
            px[x, y] = (base[0] + d, base[1] + d, base[2] + d, 255)
    d = ImageDraw.Draw(img)
    # generic trims that land on the visible parts of the humanoid UV layout
    d.line([(0, 4), (63, 4)], fill=LIGHT)
    d.line([(0, 20), (63, 20)], fill=SHADE)
    for x in range(0, 64, 8):
        d.point((x + 3, 8), fill=GLOW)
        d.point((x + 3, 24), fill=GLOW)
    return bevel(img, 18)


# --------------------------------------------------------------------------- writers


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def save(img: Image.Image, rel: str) -> None:
    path = ASSETS / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def write_models() -> None:
    for item_id in TOOL_IDS:
        write_json(ASSETS / "models" / "item" / f"{item_id}.json", {
            "parent": "minecraft:item/handheld",
            "textures": {"layer0": f"{NAMESPACE}:item/{item_id}"},
        })
    for item_id in ["starforge_ingot", "whisper", *ARMOR_IDS]:
        write_json(ASSETS / "models" / "item" / f"{item_id}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NAMESPACE}:item/{item_id}"},
        })

    write_json(ASSETS / "models" / "block" / "starforge_block.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"{NAMESPACE}:block/starforge_block"},
    })
    write_json(ASSETS / "models" / "item" / "starforge_block.json", {
        "parent": f"{NAMESPACE}:block/starforge_block",
    })
    write_json(ASSETS / "blockstates" / "starforge_block.json", {
        "variants": {"": {"model": f"{NAMESPACE}:block/starforge_block"}},
    })


EXTRA_LANG = {
    "en_us.json": {
        "itemGroup.starforge": "Starforge",
        "key.categories.starforge": "Starforge",
        "key.starforge.open_whisper": "Open Whisper Menu",
        "screen.starforge.whisper": "Whisper",
        "whisper.starforge.invincible": "Invincible",
        "whisper.starforge.flight": "Flight",
        "whisper.starforge.one_hit": "One Hit Kill",
        "whisper.starforge.night_vision": "Night Vision",
        "whisper.starforge.saturation": "Saturation",
        "whisper.starforge.water_breathing": "Water Breathing",
        "whisper.starforge.true_invisibility": "True Invisibility",
        "whisper.starforge.precise": "Precise Mode",
        "whisper.starforge.lucky": "Lucky Mode",
        "gui.starforge.on": "ON",
        "gui.starforge.off": "OFF",
        "tooltip.starforge.whisper.hint": "Press G to open the ability menu",
        "tooltip.starforge.whisper.attack": "Right-click: kill everything within 15x15",
    },
    "zh_cn.json": {
        "itemGroup.starforge": "星辉工坊",
        "key.categories.starforge": "星辉工坊",
        "key.starforge.open_whisper": "打开轻语轻扬界面",
        "screen.starforge.whisper": "轻语轻扬",
        "whisper.starforge.invincible": "无敌",
        "whisper.starforge.flight": "飞行",
        "whisper.starforge.one_hit": "一击必杀",
        "whisper.starforge.night_vision": "夜视",
        "whisper.starforge.saturation": "饱和",
        "whisper.starforge.water_breathing": "水下呼吸",
        "whisper.starforge.true_invisibility": "真·隐身",
        "whisper.starforge.precise": "精准模式",
        "whisper.starforge.lucky": "幸运模式",
        "gui.starforge.on": "开",
        "gui.starforge.off": "关",
        "tooltip.starforge.whisper.hint": "按 G 打开功能开关",
        "tooltip.starforge.whisper.attack": "右键：击杀 15×15 范围内的生物（需开启一击必杀）",
    },
}


def write_lang() -> None:
    for file_name, names in (("en_us.json", EN), ("zh_cn.json", ZH)):
        entries = dict(EXTRA_LANG[file_name])
        for item_id, name in names.items():
            key = f"block.{NAMESPACE}.{item_id}" if item_id == "starforge_block" else f"item.{NAMESPACE}.{item_id}"
            entries[key] = name
        write_json(ASSETS / "lang" / file_name, dict(sorted(entries.items())))


def shaped(pattern, key, result, count=1, category="equipment"):
    return {
        "type": "minecraft:crafting_shaped",
        "category": category,
        "pattern": pattern,
        "key": key,
        "result": {"id": result, "count": count},
    }


def write_recipes() -> None:
    ingot = {NAMESPACE: f"{NAMESPACE}:starforge_ingot"}
    stick = {"item": "minecraft:stick"}
    mat = {NAMESPACE: f"{NAMESPACE}:starforge_ingot"}
    recipes = {
        "starforge_block": {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "pattern": ["###", "###", "###"],
            "key": {"#": mat},
            "result": {"id": f"{NAMESPACE}:starforge_block", "count": 1},
        },
        "starforge_ingot_from_block": {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [{"item": f"{NAMESPACE}:starforge_block"}],
            "result": {"id": f"{NAMESPACE}:starforge_ingot", "count": 9},
        },
        "starforge_sword": shaped(["X", "X", "#"], {"X": mat, "#": stick}, f"{NAMESPACE}:starforge_sword"),        "starforge_pickaxe": shaped(["XXX", " # ", " # "], {"X": mat, "#": stick}, f"{NAMESPACE}:starforge_pickaxe"),
        "starforge_axe": shaped(["XX", "X#", " #"], {"X": mat, "#": stick}, f"{NAMESPACE}:starforge_axe"),
        "starforge_shovel": shaped(["X", "#", "#"], {"X": mat, "#": stick}, f"{NAMESPACE}:starforge_shovel"),
        "starforge_hoe": shaped(["XX", " #", " #"], {"X": mat, "#": stick}, f"{NAMESPACE}:starforge_hoe"),
        "starforge_helmet": shaped(["XXX", "X X"], {"X": mat}, f"{NAMESPACE}:starforge_helmet"),
        "starforge_chestplate": shaped(["X X", "XXX", "XXX"], {"X": mat}, f"{NAMESPACE}:starforge_chestplate"),
        "starforge_leggings": shaped(["XXX", "X X", "X X"], {"X": mat}, f"{NAMESPACE}:starforge_leggings"),
        "starforge_boots": shaped(["X X", "X X"], {"X": mat}, f"{NAMESPACE}:starforge_boots"),
        # 轻语轻扬: 1 stick + 1 crafting table + 3 planks
        "whisper": shaped(
            [" S ", "PCP", " P "],
            {"S": {"item": "minecraft:stick"}, "P": {"tag": "minecraft:planks"}, "C": {"item": "minecraft:crafting_table"}},
            f"{NAMESPACE}:whisper",
            category="misc",
        ),
    }
    for name, body in recipes.items():
        write_json(DATA / "recipe" / f"{name}.json", body)


def write_loot() -> None:
    write_json(DATA / "loot_table" / "blocks" / "starforge_block.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{NAMESPACE}:starforge_block"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })


def main() -> None:
    save(tex_ingot(), "textures/item/starforge_ingot.png")
    save(tex_block(), "textures/block/starforge_block.png")
    save(tex_sword(), "textures/item/starforge_sword.png")
    save(tex_pickaxe(), "textures/item/starforge_pickaxe.png")
    save(tex_axe(), "textures/item/starforge_axe.png")
    save(tex_shovel(), "textures/item/starforge_shovel.png")
    save(tex_hoe(), "textures/item/starforge_hoe.png")
    save(tex_helmet(), "textures/item/starforge_helmet.png")
    save(tex_chestplate(), "textures/item/starforge_chestplate.png")
    save(tex_leggings(), "textures/item/starforge_leggings.png")
    save(tex_boots(), "textures/item/starforge_boots.png")

    save(armor_layer(1), "textures/models/armor/starforge_layer_1.png")
    save(armor_layer(2), "textures/models/armor/starforge_layer_2.png")

    write_models()
    write_lang()
    write_recipes()
    write_loot()
    print("assets written to", ROOT)


if __name__ == "__main__":
    main()
