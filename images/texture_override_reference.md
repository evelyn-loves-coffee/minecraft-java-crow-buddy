# Texture Override Reference — Crow Buddy

Generated: 2025-07-20

All textures use namespace `crowbuddy` and reside under `assets/crowbuddy/`.

---

## Tiny Takeover Compliance Checklist

| Requirement | Status | Notes |
|---|---|---|
| Baby/adult entity distinction | ✅ Done | `CrowEntity` extends `TamableAnimal` → `AgeableMob`; `isBaby()` works; `ageBoundaryReached()` plays growth sound |
| Separate baby texture | ❌ Pending | Adult and baby share `textures/entity/crow.png`; need `crow_baby.png` + code change in `CrowGeoModel.java` |
| Creative "Spawn Crow" item | ❌ Pending | No spawn egg or spawn entity item registered; not in creative inventory |
| Golden dandelion freeze-aging | ✅ Done | Handled automatically by vanilla `AgeableMob` — right-click baby with golden dandelion to freeze growth |
| Baby follows parent | ✅ Done | `FollowParentGoal` registered conditionally in `registerGoals()` (line 100) |
| Baby-specific AI | ✅ Done | Baby skips adult goals (scavenge, swarm, tempt) and uses `FollowParentGoal` instead |
| Breeding mechanic | ✅ Done | `canFallInLove()` returns true for tame adults; `spawnChildFromBreeding()` triggers nest-based breeding |
| Creative tab registration | ❌ Pending | No items appear in creative mode inventory |

### Golden Dandelion Detail

No code changes needed. Vanilla's `AgeableMob` already handles this:
- Right-click a baby mob with a **golden dandelion** → aging freezes (baby stays baby indefinitely)
- Right-click the same mob with a golden dandelion again → aging resumes
- This works automatically because `CrowEntity` extends `TamableAnimal` which extends `AgeableMob`

---

## Texture Requirements Table

| # | Texture | ModelType | Object ID | Mod ID | Default UV Mode | Texture Size | File Path | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | crow.png | Entity | `geometry.crowbuddy.crow` | `crowbuddy` | Per-face | 64×32 | `textures/entity/crow.png` | ✅ Present |
| 2 | crow_baby.png | Entity | `geometry.crowbuddy.crow_baby` | `crowbuddy` | Per-face | 64×32 | `textures/entity/crow_baby.png` | ❌ Missing (shares adult) |
| 3 | crow_spawn_egg | Item | N/A (vanilla `spawn_egg` model) | `crowbuddy` | N/A (color-rendered) | N/A | N/A (color-based) | ❌ Missing (new) |
| 4 | crow_nest.png | Block | N/A (vanilla `cube_all` model) | `crowbuddy` | Box | 64×64 | `textures/block/crow_nest.png` | ✅ Present |
| 5 | black_feather.png | Item | N/A (vanilla `generated` model) | `crowbuddy` | N/A (flat sprite) | 16×16 | `textures/item/black_feather.png` | ❌ Missing |
| 6 | black_oil_sunflower_seeds.png | Item | N/A (vanilla `generated` model, no explicit model JSON) | `crowbuddy` | N/A (flat sprite) | 16×16 | `textures/item/black_oil_sunflower_seeds.png` | ✅ Present |
| 7 | icon.png | Object | N/A | `crowbuddy` | N/A | 256×256 | `icon.png` | ✅ Present |

---

## New Assets Required

### Baby Crow Texture (`crow_baby.png`)

Currently adult and baby share `textures/entity/crow.png`. To give the baby its own texture:

**Required code change** in `CrowGeoModel.java` — replace the static `TEXTURE` with conditional logic in `getTextureResource`:

```java
// Replace line 14 and getTextureResource method:
private static final Identifier TEXTURE_ADULT = CrowBuddy.id("textures/entity/crow");
private static final Identifier TEXTURE_BABY = CrowBuddy.id("textures/entity/crow_baby");

@Override
public Identifier getTextureResource(GeoRenderState renderState) {
    Boolean isBaby = renderState.getOrDefaultGeckolibData(IS_BABY, false);
    return isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
}
```

**Texture spec:**
- Dimensions: 64×32 (same UV layout as adult — identical UV coordinates, different art)
- Baby crows typically have speckled/mottled brown-and-white plumage before molting to black
- Same bone UV regions as adult; just paint different colors/patterns

### Spawn Egg / Spawn Item (`crow_spawn_egg`)

**Tiny Takeover compliance:** Creative mode requires a "Spawn Crow" item. In Tiny Takeover-era Minecraft, spawn items are named "Spawn <Entity>" rather than "<Entity> Spawn Egg". The item renders procedurally using two colors (no texture file needed):

| Property | Value |
|---|---|
| Background color | `0x3B3B3B` (dark gray, matching crow body) |
| Spot color | `0x999999` (light gray, matching frequency spots) |
| Model parent | `minecraft:item/spawn_egg` |
| Display name | "Spawn Crow" (Tiny Takeover convention) |
| Creative tab | Must be manually added via Fabric's `ModifyCreativeTabs` event |

**Required files:**

1. `models/item/crow_spawn_egg.json`:
```json
{
  "parent": "minecraft:item/spawn_egg"
}
```

2. Item registration in `ModItems.java` — **⚠️ MC 26.2 uses `Item.Properties().spawnEgg()` builder, NOT constructor colors**:
```java
public static final SpawnEggItem CROW_SPAWN_EGG = new SpawnEggItem(
    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("crow_spawn_egg"))).spawnEgg(ModEntities.CROW)
);
```
Then register in `ModItems.register()`:
```java
net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("crow_spawn_egg"), CROW_SPAWN_EGG);
```

**NOTE:** Color parameters (`0x3B3B3B`, `0x999999`) are set via `Item.Properties.spawnEgg(EntityType)` — the entity type determines colors automatically. If custom colors are needed, verify `SpawnEggItem` API in GeckoLib 5.5.3 / MC 26.2.

3. Lang entry in `lang/en_us.json`:
```json
"item.crowbuddy.crow_spawn_egg": "Spawn Crow"
```

4. Creative mode registration — use Fabric API's `CreativeModeTabEvents` (Fabric API 0.154.2+26.2). Register in `CrowBuddyClient.java`:
```java
// Add spawn egg to Spawn Eggs tab:
CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
    .register(output -> output.accept(ModItems.CROW_SPAWN_EGG));

// Add items to Ingredients tab:
CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
    .register(output -> {
        output.accept(ModItems.BLACK_FEATHER);
        output.accept(ModItems.BLACK_OIL_SUNFLOWER_SEEDS);
    });

// Add nest block item to Building Blocks tab:
CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
    .register(output -> output.accept(ModItems.CROW_NEST_ITEM));
```

---

## UV Mode Explained

| Mode | What It Means | Applies To |
|---|---|---|
| **Per-face** | Each face of a cube gets its own independent UV rectangle. You paint each face separately on the texture sheet. | GeckoLib entity models (`crow.geo.json`, `crow_baby.geo.json`) |
| **Box** | A single texture is wrapped around all 6 faces of a cube. The same image appears on top, bottom, and all sides. | Vanilla block models using `minecraft:block/cube_all` parent (`crow_nest`) |
| **N/A (flat sprite)** | No UV mapping — the entire texture is a single 2D image rendered as-is in the inventory, crafting grid, or as a dropped item. | Flat item models using `minecraft:item/generated` parent (`black_feather`, `black_oil_sunflower_seeds`) |
| **N/A (color-rendered)** | No texture file — the item renders procedurally from two RGB colors (background + freckle spots). | Spawn eggs using `minecraft:item/spawn_egg` parent (`crow_spawn_egg`) |

---

## Detailed UV Layout — Entity Textures (64×32)

Adult (`crow.png`) and baby (`crow_baby.png`) each have their own texture sheet but **share identical UV coordinates**. The baby geo model uses the same `[uv]` values as the adult — only the bone scales differ. Paint both sheets using the same UV grid below, but with different art (adult = solid black, baby = speckled brown/white).

| Bone | UV Origin | Pixel Columns | Pixel Rows | Cube Dimensions | Mirror |
|---|---|---|---|---|---|
| Head | `[0, 0]` | 0–8 | 0–4 | 4×4×4 | No |
| Beak | `[0, 8]` | 0–4 | 8–10 | 2×1.5×2.5 | No |
| Left Wing | `[0, 16]` | 0–4 | 16–22 | 1×6×6 | No |
| Right Wing | `[0, 16]` | 0–4 | 16–22 | 1×6×6 | Yes (UV mirrored) |
| Body | `[16, 0]` | 16–36 | 0–10 | 5×6×8 | No |
| Tail | `[32, 16]` | 32–44 | 16–22 | 4×2×5 | No |
| Left Leg | `[48, 16]` | 48–56 | 16–20 | 1×4×1 | No |
| Right Leg | `[48, 16]` | 48–56 | 16–20 | 1×4×1 | Yes (UV mirrored) |
| Left Foot | `[52, 16]` | 52–56 | 16–18 | 1×0.5×2 | No |
| Right Foot | `[52, 16]` | 52–56 | 16–18 | 1×0.5×2 | Yes (UV mirrored) |

### Visual UV Map

```
Row\Col  0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63
0  ┌─── HEAD ───┐                                                            ┌────────── BODY ────────────┐
1  │             │                                                            │                            │
2  │             │                                                            │                            │
3  │             │                                                            │                            │
4  └─────────────┘                                                            │                            │
5  ┌─────┐                                                                                     │                            │
6  │ BEAK│                                                                                        │                            │
7  └─────┘                                                                                     │                            │
8  ┌─────┐                                                                                     │                            │
9  │ BEAK│                                                                                        │                            │
10 └─────┘                                                                                     └─────────────────────────┘
11
12
13
14
15
16 ┌─── WING ───┐                                              ┌──── TAIL ─────┐           ┌── LEG ─┐
17 │             │                                              │                │           │        │
18 │             │                                              │                │           │ FOOT │
19 │             │                                              │                │           │        │
20 │             │                                              │                │           └────────┘
21 │             │                                              │                │
22 └─────────────┘                                              └────────────────┘
23
24
25
26
27
28
```

---

## Baby Crow Texture — crow_baby.png (64×32)

Same UV layout as adult (`crow.png`), but with baby-appropriate art.

| Property | Value |
|---|---|
| Dimensions | 64×32 |
| UV coordinates | Identical to adult (same `[uv]` values in `crow_baby.geo.json`) |
| Art style | Speckled/mottled brown-and-white plumage (real juvenile crows are not solid black) |
| Eyes | Larger relative to head, more prominent |
| Beak | Slightly lighter gray (juvenile beak coloration) |

---

## Block Texture — crow_nest.png (64×64)

Uses `minecraft:block/cube_all` — the same texture wraps all 6 faces.

| Region | Purpose |
|---|---|
| Center | Nest body (twigs, mud, grass) |
| Top-center | Transparent opening (eggs/chicks render here via block entity renderer) |
| Corners & edges | Transparent (block has `noCollision()` and `noOcclusion()`) |
| Bottom | Underside of nest |

---

## Item Textures (16×16)

Flat 2D sprites rendered by `minecraft:item/generated` parent model.

### Spawn Egg (no texture)

Spawn eggs render procedurally — no PNG needed. Defined by two colors passed to `SpawnEggItem`:

| Color | Hex | Purpose |
|---|---|---|
| Background | Auto-derived from entity | Dark gray egg body |
| Spots | Auto-derived from entity | Light gray spots |

**NOTE:** In MC 26.2, `Item.Properties().spawnEgg(EntityType)` auto-derives colors from the entity. Custom colors (`0x3B3B3B`/`0x999999`) may not be supported — verify against GeckoLib 5.5.3 / MC 26.2 API.

### black_feather.png
- Single black crow feather, diagonal or vertical orientation
- Transparent background
- Black/dark gray body with subtle lighter gray highlights on shaft and barbs

### black_oil_sunflower_seeds.png
- 3–5 seeds in a small cluster near center
- Black seeds with white/light gray stripes
- Transparent background

---

## Identifier Reference

| Asset | Full Identifier | Resolves To |
|---|---|---|
| Adult geo model | `crowbuddy:entity/crow` | `assets/crowbuddy/geckolib/models/entity/crow.geo.json` |
| Baby geo model | `crowbuddy:entity/crow_baby` | `assets/crowbuddy/geckolib/models/entity/crow_baby.geo.json` |
| Animations | `crowbuddy:entity/crow` | `assets/crowbuddy/geckolib/animations/entity/crow.animation.json` |
| Adult texture | `crowbuddy:textures/entity/crow` | `assets/crowbuddy/textures/entity/crow.png` |
| Baby texture | `crowbuddy:textures/entity/crow_baby` | `assets/crowbuddy/textures/entity/crow_baby.png` |
| Block texture | `crowbuddy:block/crow_nest` | `assets/crowbuddy/textures/block/crow_nest.png` |
| Feather texture | `crowbuddy:item/black_feather` | `assets/crowbuddy/textures/item/black_feather.png` |
| Seeds texture | `crowbuddy:item/black_oil_sunflower_seeds` | `assets/crowbuddy/textures/item/black_oil_sunflower_seeds.png` |
| Spawn egg model | `crowbuddy:item/crow_spawn_egg` | `assets/crowbuddy/models/item/crow_spawn_egg.json` |
