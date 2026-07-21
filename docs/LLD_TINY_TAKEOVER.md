# Crow Buddy: Low-Level Design — Tiny Takeover Compliance

## 1. Scope

This LLD covers all missing Tiny Takeover compliance requirements identified in the PAWS analysis (`images/paws_analysis_tiny_takeover.md`). Five tasks:

1. **Baby crow texture separation** — dedicated `crow_baby.png` with speckled juvenile plumage
2. **Spawn egg item** — creative-mode "Spawn Crow" item using `SpawnEggItem`
3. **Creative tab registration** — populate all mod items in correct vanilla creative tabs
4. **Breeding item override** — enable golden dandelion freeze-aging + seed-based breeding trigger
5. **Verify golden dandelion freeze-aging** — confirm vanilla `AgeableMob` behavior works end-to-end

---

## 2. Design Decisions (from PAWS Analysis)

| Decision | Resolution | Evidence |
|----------|-----------|----------|
| Spawn egg constructor | Use `Item.Properties().spawnEgg(EntityType)` builder (NOT old `SpawnEggItem(EntityType, int, int, Settings)`) | Fabric API 0.154.2+26.2 docs; `ModItems.java:12-14` shows `Item.Properties` pattern |
| `CreativeModeTabs.SPAWN_EGGS` existence | Use with fallback try-catch | PAWS Q1 — unverified in MC 26.2; `modifyOutputEvent` confirmed by Fabric API |
| `isBreedingItem()` override | **Required** — accept golden dandelion + black oil sunflower seeds | PAWS Q4 — nest breeding does not trigger without valid breeding item; `getBreedOffspring()` returns `null` (safe) |
| Golden dandelion freeze-aging | Handled by `isBreedingItem()` override; no separate code needed | PAWS Q2 — same mechanism enables both features |
| Baby texture UV coordinates | Identical to adult; verified in `crow_baby.geo.json` | `crow.geo.json` vs `crow_baby.geo.json` — same `[uv]` values per bone |
| Spawn egg colors | Auto-derived from entity type via `Item.Properties().spawnEgg()` | PAWS Q3 — custom colors may not be supported in MC 26.2 |

---

## 3. High-Level Design (HLD)

### 3.1 Component Overview

```
┌───────────────────────────────────────────────────────────┐
│                    Common Layer                           │
│                                                           │
│  ModItems (MODIFIED)                                      │
│  ├── BLACK_OIL_SUNFLOWER_SEEDS  (existing)               │
│  ├── BLACK_FEATHER              (existing)               │
│  ├── CROW_NEST_ITEM             (existing)               │
│  └── CROW_SPAWN_EGG             (NEW — SpawnEggItem)     │
│                                                           │
│  CrowEntity (MODIFIED)                                    │
│  └── isBreedingItem() — accept golden dandelion + seeds   │
│                                                           │
├───────────────────────────────────────────────────────────┤
│                    Client Layer                           │
│                                                           │
│  CrowBuddyClient (MODIFIED)                              │
│  ├── ModClientEntities (existing)                         │
│  ├── ModClientNetworking (existing)                       │
│  └── Creative tab callbacks (NEW — 3 modifyOutputEvent)  │
│                                                           │
│  CrowGeoModel (MODIFIED)                                 │
│  ├── getTextureResource() — conditional baby/adult        │
│  └── TEXTURE_ADULT, TEXTURE_BABY (split constants)        │
│                                                           │
│  CrowRenderer (no change)                                 │
│                                                           │
├───────────────────────────────────────────────────────────┤
│                    Assets Layer                           │
│                                                           │
│  textures/entity/crow.png       (existing — adult)        │
│  textures/entity/crow_baby.png  (NEW — baby)              │
│  models/item/crow_spawn_egg.json (NEW — spawn egg model)  │
│  lang/en_us.json                (MODIFIED — 2 new keys)   │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

### 3.2 Data Flow — Breeding Item Resolution

```
Player right-clicks crow with golden dandelion / seeds
  → CrowEntity.mobInteract() called
  → vanilla ItemUseAnim handler checks isBreedingItem(stack)
  → isBreedingItem() returns true (overridden)
  → canFallInLove() returns true (tame adult)
  → tryToFallInLove() succeeds
  → hearts particles → spawnChildFromBreeding()
  → nesting state → CrowNestBlockEntity starts lifecycle
```

### 3.3 Data Flow — Creative Tab

```
Client init → CrowBuddyClient.onInitializeClient()
  → CreativeModeTabEvents.modifyOutputEvent(SPAWN_EGGS)
    → output.accept(CROW_SPAWN_EGG)
  → CreativeModeTabEvents.modifyOutputEvent(INGREDIENTS)
    → output.accept(BLACK_FEATHER)
    → output.accept(BLACK_OIL_SUNFLOWER_SEEDS)
  → CreativeModeTabEvents.modifyOutputEvent(BUILDING_BLOCKS)
    → output.accept(CROW_NEST_ITEM)
```

---

## 4. Low-Level Design (LLD)

### 4.1 Task 1: Baby Crow Texture Separation

#### 4.1.1 New Asset: `textures/entity/crow_baby.png`

| Property | Value |
|----------|-------|
| Dimensions | 64×32 |
| Format | PNG with alpha channel |
| UV layout | Identical to `crow.png` (same `[uv]` coordinates in `crow_baby.geo.json`) |
| Art style | Speckled/mottled brown-and-white plumage (juvenile crow plumage) |
| Path | `src/main/resources/assets/crowbuddy/textures/entity/crow_baby.png` |

UV regions (same as adult):

| Bone | UV Origin | Pixel Region |
|------|-----------|-------------|
| Head | `[0, 0]` | cols 0–8, rows 0–4 |
| Beak | `[0, 8]` | cols 0–4, rows 8–10 |
| Wings | `[0, 16]` | cols 0–4, rows 16–22 |
| Body | `[16, 0]` | cols 16–36, rows 0–10 |
| Tail | `[32, 16]` | cols 32–44, rows 16–22 |
| Legs | `[48, 16]` | cols 48–56, rows 16–20 |
| Feet | `[52, 16]` | cols 52–56, rows 16–18 |

#### 4.1.2 Modified: `CrowGeoModel.java`

**File:** `src/client/java/com/crowbuddy/client/model/CrowGeoModel.java`

**Changes:**
- Split static `TEXTURE` constant into `TEXTURE_ADULT` and `TEXTURE_BABY`
- Modify `getTextureResource()` to return conditional texture

**Before (lines 14, 29–31):**
```java
private static final Identifier TEXTURE = CrowBuddy.id("textures/entity/crow");

@Override
public Identifier getTextureResource(GeoRenderState renderState) {
    return TEXTURE;
}
```

**After:**
```java
private static final Identifier TEXTURE_ADULT = CrowBuddy.id("textures/entity/crow");
private static final Identifier TEXTURE_BABY = CrowBuddy.id("textures/entity/crow_baby");

@Override
public Identifier getTextureResource(GeoRenderState renderState) {
    Boolean isBaby = renderState.getOrDefaultGeckolibData(IS_BABY, false);
    return isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
}
```

**Rationale:** `IS_BABY` data ticket is already set in `addAdditionalStateData()` (line 36). `getOrDefaultGeckolibData()` safely defaults to `false`. No thread-safety concerns — texture lookup is client-only, single-threaded.

---

### 4.2 Task 2: Spawn Egg Item

#### 4.2.1 Modified: `ModItems.java`

**File:** `src/main/java/com/crowbuddy/item/ModItems.java`

**Add import:**
```java
import net.minecraft.world.item.SpawnEggItem;
```

**Add field (after line 14):**
```java
public static final SpawnEggItem CROW_SPAWN_EGG = new SpawnEggItem(
    new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, CrowBuddy.id("crow_spawn_egg")))
        .spawnEgg(ModEntities.CROW)
);
```

**Add registration in `register()` (after line 20):**
```java
net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, CrowBuddy.id("crow_spawn_egg"), CROW_SPAWN_EGG);
```

**Rationale:** Uses `Item.Properties().spawnEgg(EntityType)` builder per Fabric API 0.154.2+26.2 docs. This is the same pattern used by `ModItems.java:12-14` for `.setId()`. `ModEntities.CROW` is guaranteed to be registered first (see `CrowBuddy.java:25`).

#### 4.2.2 New Asset: `models/item/crow_spawn_egg.json`

**Path:** `src/main/resources/assets/crowbuddy/models/item/crow_spawn_egg.json`

```json
{
  "parent": "minecraft:item/spawn_egg"
}
```

#### 4.2.3 Modified: `lang/en_us.json`

**File:** `src/main/resources/assets/crowbuddy/lang/en_us.json`

**Add keys:**
```json
{
  "item.crowbuddy.black_oil_sunflower_seeds": "Black Oil Sunflower Seeds",
  "item.crowbuddy.black_feather": "Black Feather",
  "item.crowbuddy.crow_spawn_egg": "Spawn Crow",
  "entity.crowbuddy.crow": "Crow",
  "block.crowbuddy.crow_nest": "Crow Nest"
}
```

**Note:** Display name follows Tiny Takeover convention: "Spawn Crow" (not "Crow Spawn Egg").

---

### 4.3 Task 3: Creative Tab Registration

#### 4.3.1 Modified: `CrowBuddyClient.java`

**File:** `src/client/java/com/crowbuddy/client/CrowBuddyClient.java`

**Add imports:**
```java
import com.crowbuddy.item.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.client.rendering.v1.CreativeModeTabs;
```

**Add in `onInitializeClient()` (after line 19):**
```java
// Tiny Takeover: register items in creative tabs
CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
    .register(output -> output.accept(ModItems.CROW_SPAWN_EGG));

CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
    .register(output -> {
        output.accept(ModItems.BLACK_FEATHER);
        output.accept(ModItems.BLACK_OIL_SUNFLOWER_SEEDS);
    });

CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
    .register(output -> output.accept(ModItems.CROW_NEST_ITEM));
```

**Rationale:** `modifyOutputEvent` is confirmed by Fabric API 0.154.2+26.2 docs. If `CreativeModeTabs.SPAWN_EGGS` does not exist in MC 26.2, this will throw a compile-time error (safe — caught during build). Fallback: wrap in try-catch or use `CREATIVE_INVENTORY_ALL`.

---

### 4.4 Task 4: Breeding Item Override

#### 4.4.1 Modified: `CrowEntity.java`

**File:** `src/main/java/com/crowbuddy/entity/CrowEntity.java`

**Add import (near line 30):**
```java
import net.minecraft.world.item.Items;
```

**Add method (after `isFood()` at line 228):**
```java
@Override
public boolean isBreedingItem(ItemStack itemStack) {
    return isFood(itemStack) || itemStack.is(Items.GOLDEN_DANDELION);
}
```

**Rationale:** Two features depend on this single override:

1. **Golden dandelion freeze-aging:** Vanilla `AgeableMob.freezeAge()` is triggered when the entity enters the "in love" state with a golden dandelion. `isBreedingItem()` must return `true` for golden dandelion so `tryToFallInLove()` succeeds.

2. **Nest-based breeding trigger:** The breeding flow requires `canFallInLove()` → `tryToFallInLove()` → `spawnChildFromBreeding()`. Without a valid breeding item, the player cannot trigger this chain. `isFood()` already accepts seeds, but `isBreedingItem()` (separate from `isFood()`) is what vanilla checks for the "love" mechanic.

**Safety:** `getBreedOffspring()` returns `null` (line 298-301), so no vanilla child is spawned. The actual offspring is handled by `CrowNestBlockEntity` via `spawnChildFromBreeding()` → `setInMatingState(true)`.

**Edge cases:**
- Baby crow: `canFallInLove()` returns `false` (line 305: `!this.isBaby()`), so babies cannot breed. ✅
- Untamed crow: `canFallInLove()` returns `false` (line 305: `isTame()`), so wild crows cannot breed. ✅
- Golden dandelion on adult tame crow: enters "in love" state, `spawnChildFromBreeding()` fires, nest lifecycle starts. ✅

---

### 4.5 Task 5: Verify Golden Dandelion Freeze-Aging

#### 4.5.1 Expected Behavior (no code changes)

Vanilla `AgeableMob` handles golden dandelion freeze-aging automatically:
1. Player right-clicks baby crow with golden dandelion
2. `isBreedingItem(golden_dandelion)` → returns `true` (Task 4)
3. `canFallInLove()` → returns `false` (baby), BUT golden dandelion has a separate path
4. Vanilla checks for golden dandelion via `mobInteract()` → calls `ageUp(-amount, false)` to freeze

**Verification steps (in-game):**
1. Spawn baby crow via spawn egg
2. Right-click with golden dandelion
3. Confirm growth bar freezes (no progression)
4. Right-click again → growth resumes

**Risk:** If vanilla's golden dandelion handler is tied to `isBreedingItem()` for babies (separate from the "in love" path), the override in Task 4 resolves this. If it uses a different path (item tag check), no additional code is needed.

#### 4.5.2 Fallback: Manual Age Freeze Override

If golden dandelion does not freeze aging automatically, add to `CrowEntity.java`:

```java
@Override
public InteractionResult mobInteract(Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (this.isBaby() && stack.is(Items.GOLDEN_DANDELION)) {
        if (!this.level().isClientSide()) {
            this.ageUp(-((int)(this.getAge() * 0.1F)), false);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
    return super.mobInteract(player, hand);
}
```

**This is a fallback only.** Implement Task 4 first; verify in-game; add this only if freeze-aging fails.

---

## 5. Verification Plan

### 5.1 Build Verification

| Check | Command | Pass Criteria |
|-------|---------|---------------|
| Compile | `./gradlew build` | Zero errors |
| Unit tests | `./gradlew test` | All existing tests pass |
| Jar generation | `./gradlew jar` | JAR produced in `build/libs/` |

### 5.2 Functional Verification (In-Game)

| # | Test | Steps | Expected Result |
|---|------|-------|-----------------|
| 1 | Baby texture renders | Spawn baby crow via spawn egg | Speckled brown/white plumage (not solid black) |
| 2 | Adult texture renders | Spawn adult crow (via growth or nest) | Solid black plumage |
| 3 | Spawn egg appears in creative | Open creative inventory → Spawn Eggs tab | "Spawn Crow" item visible |
| 4 | Spawn egg works | Click "Spawn Crow" item | Adult crow spawns at target location |
| 5 | Feather in creative | Open creative inventory → Ingredients tab | "Black Feather" visible |
| 6 | Seeds in creative | Open creative inventory → Ingredients tab | "Black Oil Sunflower Seeds" visible |
| 7 | Nest in creative | Open creative inventory → Building Blocks tab | "Crow Nest" visible |
| 8 | Golden dandelion freeze-aging | Spawn baby crow → right-click with golden dandelion | Growth bar freezes |
| 9 | Golden dandelion resume | Right-click same baby crow with golden dandelion again | Growth resumes |
| 10 | Breeding triggers nest | Feed 2 tame adult crows seeds near nest | Hearts → mating state → nest starts incubation |
| 11 | Breeding with golden dandelion | Feed 2 tame adults golden dandelion near nest | Same as #10 |

### 5.3 PAWS Compliance Verification

| Pillar | Check | Method |
|--------|-------|--------|
| **P**erformance | No runtime cost from isBreedingItem() override (simple `isFood()` + tag check) | Code review |
| **P**erformance | Spawn egg is static item, no tick logic | Code review |
| **A**uditability | All registrations logged via `CrowBuddy.LOGGER` | Verify log output |
| **A**uditability | Lang key follows `<namespace>.<type>.<id>` pattern | `en_us.json` review |
| **W**orkability | `getBreedOffspring()` returns `null` — no vanilla child spawned | In-game test #10 |
| **W**orkability | `canFallInLove()` guard prevents baby/wild breeding | In-game tests #8, #10 |
| **S**calability | Spawn egg pattern reusable for future entities | `ModItems.java` review |
| **S**calability | Creative tab callbacks isolated in client layer | `CrowBuddyClient.java` review |

---

## 6. File Inventory

### New Assets (2)

| File | Path | Purpose |
|------|------|---------|
| `crow_baby.png` | `assets/crowbuddy/textures/entity/` | Baby crow texture (64×32) |
| `crow_spawn_egg.json` | `assets/crowbuddy/models/item/` | Spawn egg item model |

### Modified Files (5)

| File | Change Summary | LOC Impact |
|------|----------------|------------|
| `CrowGeoModel.java` | Split `TEXTURE` → `TEXTURE_ADULT` + `TEXTURE_BABY`; conditional `getTextureResource()` | +3, -2 |
| `ModItems.java` | Add `CROW_SPAWN_EGG` field + registration | +5 |
| `CrowBuddyClient.java` | Add 3 `CreativeModeTabEvents.modifyOutputEvent()` callbacks | +10 |
| `CrowEntity.java` | Add `isBreedingItem()` override (golden dandelion + seeds) | +4 |
| `en_us.json` | Add `"item.crowbuddy.crow_spawn_egg": "Spawn Crow"` | +1 |

**Total LOC impact:** +23, -2 = **+21 net lines**

---

## 7. Risk Assessment

| # | Risk | Severity | Mitigation | Verified? |
|---|------|----------|------------|-----------|
| R1 | `CreativeModeTabs.SPAWN_EGGS` may not exist in MC 26.2 | HIGH | Compile-time error; fallback to `CREATIVE_INVENTORY_ALL` | ❌ |
| R2 | `Item.Properties().spawnEgg()` API changed in MC 26.2 | MEDIUM | Compile-time error; check Fabric API docs | ❌ |
| R3 | Golden dandelion freeze-aging not triggered by `isBreedingItem()` alone | MEDIUM | Verify in-game; fallback `mobInteract()` override ready | ❌ |
| R4 | Spawn egg colors auto-derived don't match desired crow colors | LOW | Aesthetic only; investigate `SpawnEggItem` color override | ❌ |
| R5 | Baby texture UV mismatch | LOW | UV coordinates verified identical in both `.geo.json` files | ✅ |

**Overall risk: MEDIUM** — R1 and R2 are compile-time failures (safe). R3 has a documented fallback. R4-R5 are low-impact.

---

## 8. Confidence Level

| Task | Confidence | Reason |
|------|------------|--------|
| Baby texture separation | **95%** | GeckoLib `DataTicket` pattern already in use; UVs verified identical |
| Spawn egg registration | **80%** | `Item.Properties().spawnEgg()` confirmed by Fabric API; MC 26.2 specifics assumed |
| Creative tab registration | **70%** | `modifyOutputEvent` pattern confirmed; `SPAWN_EGGS` tab existence assumed |
| Breeding item override | **90%** | Standard `isBreedingItem()` override; `getBreedOffspring()` returns `null` (safe) |
| Golden dandelion freeze-aging | **50%** | Depends on vanilla `AgeableMob` internals; fallback documented |

**Overall confidence: 77%** — limited by MC 26.2 API uncertainty. All risks have compile-time detection or documented fallbacks.

---

## 9. Path to Improvement

### Immediate (this implementation)

1. Run `./gradlew build` after each file change to verify MC 26.2 API compatibility
2. If `CreativeModeTabs.SPAWN_EGGS` fails: use `CreativeModeTabs.CREATIVE_INVENTORY_ALL` as fallback
3. If `Item.Properties().spawnEgg()` fails: check `SpawnEggItem` source for alternative constructor
4. Test golden dandelion freeze-aging in-game before adding fallback `mobInteract()` override

### Post-Implementation

1. Extract `register()` helper in `ModItems.java` — reduces per-item boilerplate
2. Add integration tests for breeding flow (mock `AgeableMob` behavior)
3. Create `ModSpawnEggs` helper class if more entities are added
4. Document MC 26.2 API assumptions in `docs/mc_releases/` for future reference
