# Crow Buddy: Low-Level Design — Phase 5 (Polishing & Verification)

> Implementation status: complete. Automated build/tests pass; the in-game checks in `TEST_PLAN.md` remain manual. Custom `.ogg` files are placeholders until the planned audio in `SOUNDS.md` is sourced and licensed.

## 1. Scope

Phase 5 delivers three client-side polish features and executes the verification plan:
1. **Animated nest BlockEntityRenderer** — visual feedback for eggs, hatching, fledglings
2. **Baby crow model verification** — confirm existing `crow_baby.geo.json` renders correctly
3. **Nest sound wiring** — connect 6 unused `ModSounds` to nest lifecycle events
4. **Functional verification** — execute `TEST_PLAN.md` checklist

## 2. Design Decisions (from Analysis)

| Decision | Resolution |
|----------|-----------|
| Animated nest feedback | **Required** — show eggs, chicks, hatching |
| Baby crow model | **Scaled-down variant** with proportionally larger head (already modeled in `crow_baby.geo.json`) |
| Unused sound registrations | **Keep** — wire up to nest lifecycle events |

---

## 3. High-Level Design (HLD)

### 3.1 Component Overview

```
┌─────────────────────────────────────────────────────┐
│                 Client Layer                         │
│                                                      │
│  CrowBuddyClient                                     │
│  ├── ModClientEntities (crow renderer)               │
│  ├── ModClientNetworking (packet handlers)           │
│  └── CrowNestBlockEntityRenderer (NEW)               │
│       ├── Nest base model (existing block model)     │
│       ├── Egg overlay (STAGE_EGGS)                   │
│       ├── Chick overlay (STAGE_FLEDGLING)            │
│       └── Progress indicators                        │
│                                                      │
├─────────────────────────────────────────────────────┤
│                 Server Layer                         │
│                                                      │
│  CrowNestBlockEntity (MODIFIED)                      │
│  ├── tick() — state machine advancement              │
│  ├── advanceStage() — add sound playback (NEW)       │
│  └── startIncubation() — add sound (NEW)             │
│                                                      │
│  CrowEntity (MODIFIED)                               │
│  └── spawnChildFromBreeding() — add CROW_MATE sound  │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### 3.2 Data Flow

```
Server tick → CrowNestBlockEntity.tick()
    → stateMachine.tick()
    → stage transition triggers sideEffect
    → advanceStage(level, pos):
        1. Play sound (NEW)
        2. Spawn particles (existing)
        3. Spawn baby crow (existing)
    → setChanged() → chunk data sync

Client render → CrowNestBlockEntityRenderer.render()
    → read BE stage from NBT/sync
    → select overlay (eggs/chick/empty)
    → render with tick-based animations
```

---

## 4. Low-Level Design (LLD)

### 4.1 Task 1: Baby Crow Model Verification

**Current state:** Already functional. `CrowGeoModel` selects `MODEL_BABY` when `isBaby()` is true. `crow_baby.geo.json` exists with larger head-to-body ratio.

**Verification steps:**
- Confirm `CrowRenderer` + `CrowGeoModel` correctly renders baby crows
- Confirm baby texture uses same `crow.png` (shared UV map)
- No code changes needed unless rendering issues are found

**Files involved:** `CrowGeoModel.java`, `CrowRenderer.java`, `crow_baby.geo.json`

**Deliverable:** Verified working baby model, or bugfix if issues found.

---

### 4.2 Task 2: Animated Nest BlockEntityRenderer

#### 4.2.1 New File: `CrowNestBlockEntityRenderer.java`

**Package:** `com.crowbuddy.client.renderer`

**Class structure:**
```
CrowNestBlockEntityRenderer extends BlockEntityRenderer<CrowNestBlockEntity>
  ┌────────────────────────────────┐
  │ Fields                         │
  │ - nestModel (BakedModel)       │
  │ - eggModel (BakedModel)        │
  │ - chickModel (BakedModel)      │
  └────────────────────────────────┘
  ┌────────────────────────────────┐
  │ Constructor                    │
  │ - Context parameter            │
  │ - Load baked models            │
  └────────────────────────────────┘
  ┌────────────────────────────────┐
  │ render(BE, Float, PoseStack)   │
  │ 1. Render base nest model      │
  │ 2. Switch on stage:            │
  │    - EGGS: render egg overlay  │
  │    - HATCHING: egg + shake     │
  │    - FLEDGLING: chick overlay  │
  │    - BABY_FLYING: empty        │
  │ 3. Tick-based bob animation    │
  └────────────────────────────────┘
```

**Stage rendering details:**

| Stage | Visual | Animation |
|-------|--------|-----------|
| `STAGE_IDLE` | Base nest only | None |
| `STAGE_EGGS` | Nest + 3 egg ellipses | Gentle bob (sin wave, period 80 ticks) |
| `STAGE_HATCHING` | Nest + 3 eggs | Shake (rotation ±5°, period 10 ticks) |
| `STAGE_FLEDGLING` | Nest + chick silhouette | Bob + slight scale pulse |
| `STAGE_BABY_FLYING` | Nest + feather remnants | None |

**Rendering approach:** Use `VertexConsumer` with `RenderType.entityTranslucent()` for overlays. Render eggs as small spheres using `RenderBuilders.entityOutline()` or simple cube primitives. Chick rendered as a scaled-down crow model or simple geometry.

**Alternative approach (simpler):** Use `MultiModelData` or layer multiple baked models with `RenderLayer`. However, given the nest is `noCollision()` and `noOcclusion()`, the simplest approach is to render the base block model then overlay stage-specific geometry using `PoseStack` transforms.

**Recommended approach:** Render base model via `BlockRenderDispatcher`, then use `Tesselator` + `BufferBuilder` for stage-specific overlays (eggs as small cubes, chick as scaled geometry).

#### 4.2.2 Registration in `CrowBuddyClient.java`

Add:
```java
BlockEntityRenderers.register(ModBlocks.getCrowNestBE(), CrowNestBlockEntityRenderer::new);
```

Remove the `// registerNestRenderer()` comment stub.

#### 4.2.3 Files Modified

| File | Change |
|------|--------|
| `CrowBuddyClient.java` | Add `BlockEntityRenderers.register()` call |
| `CrowNestBlock.java` | No change (already `EntityBlock`) |

#### 4.2.4 New Files

| File | Lines (est.) | Purpose |
|------|--------------|---------|
| `CrowNestBlockEntityRenderer.java` | ~80 | Stage-aware nest renderer |

---

### 4.3 Task 3: Nest Sound Wiring

#### 4.3.1 Sound-to-Event Mapping

| Sound Event | Trigger Point | File |
|-------------|--------------|------|
| `CROW_EGG_LAY` | `startIncubation()` called | `CrowNestBlockEntity.java` |
| `CROW_HATCH` | `EGGS_TO_HATCHING` side effect | `CrowNestBlockEntity.java` |
| `CROW_FLEDGLING` | `HATCHING_TO_FLEDGLING` side effect | `CrowNestBlockEntity.java` |
| `CROW_BABY_FLIGHT` | `BABY_FLYING_TO_IDLE` side effect | `CrowNestBlockEntity.java` |
| `CROW_MATE` | `spawnChildFromBreeding()` called | `CrowEntity.java` |
| `CROW_GROW` | Baby crow ages to adult (override `ageUp()`) | `CrowEntity.java` |

#### 4.3.2 Implementation Details

**`CrowNestBlockEntity.startIncubation()`:**
```java
public void startIncubation() {
    this.stateMachine.startIncubation();
    this.setChanged();
    if (this.level != null && !this.level.isClientSide()) {
        playSound(ModSounds.CROW_EGG_LAY);
    }
}
```

**`CrowNestBlockEntity.advanceStage()` — add sound per case:**
```java
case EGGS_TO_HATCHING -> {
    playSound(ModSounds.CROW_HATCH);
    // existing particles...
}
case HATCHING_TO_FLEDGLING -> {
    playSound(ModSounds.CROW_FLEDGLING);
    // existing spawn + particles...
}
case BABY_FLYING_TO_IDLE -> {
    playSound(ModSounds.CROW_BABY_FLIGHT);
    // existing particles...
}
```

**Helper method in `CrowNestBlockEntity`:**
```java
private void playSound(SoundEvent sound) {
    level.playSound(null, getWorldPosition(), sound,
        SoundSource.NEUTRAL, 0.5f, 1.0f);
}
```

**`CrowEntity.spawnChildFromBreeding()`:**
Add after existing heart particles:
```java
this.playSound(ModSounds.CROW_MATE, 1.0f, 1.0f);
```

**`CrowEntity` — override `ageUp`:**
```java
@Override
public boolean ageUp(int amount, boolean skipGrowth) {
    if (this.isBaby() && !skipGrowth) {
        this.playSound(ModSounds.CROW_GROW, 1.0f, 1.0f);
    }
    return super.ageUp(amount, skipGrowth);
}
```

#### 4.3.3 Files Modified

| File | Change |
|------|--------|
| `CrowNestBlockEntity.java` | Add `playSound()` helper; wire 4 sounds in `advanceStage()` and `startIncubation()` |
| `CrowEntity.java` | Add `CROW_MATE` in `spawnChildFromBreeding()`; override `ageUp()` for `CROW_GROW` |

---

### 4.4 Task 4: Comment Cleanup

Remove Phase 4 stub comments from `CrowBuddyClient.java`:
```java
// Phase 4: BlockEntity renderer for CrowNestBlockEntity
// registerNestRenderer();

// Phase 4: Baby crow model (GeckoLib baby variant)
// registerBabyModel();

// Phase 4: Particle type registration (if custom particles needed)
// registerParticles();
```

Replace with actual registration call for nest renderer. Keep particle comment as "Future:" if desired.

---

## 5. Verification Plan

### 5.1 Build Verification

| Check | Command | Pass Criteria |
|-------|---------|---------------|
| Compile | `./gradlew build` | Zero errors |
| Unit tests | `./gradlew test` | All 5 test classes pass |
| Jar generation | `./gradlew jar` | JAR produced in `build/libs/` |

### 5.2 Functional Verification (In-Game)

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Nest egg visual | Feed 2 crows breeding seeds → observe nest | 3 eggs visible in nest, gentle bob animation |
| Nest hatching visual | Wait for incubation (10 min) | Eggs shake, crack particles, chick appears |
| Chick fledgling | Wait for fledgling stage (20 min) | Chick visible, bob animation |
| Baby crow spawn | Wait for baby flying stage | Baby crow spawns with correct model (large head) |
| Nest reset visual | After baby flies away | Empty nest with feather remnants |
| CROW_MATE sound | Trigger breeding | Distinct crow mating sound plays |
| CROW_EGG_LAY sound | Breeding completes, eggs laid | Egg-lay sound at nest position |
| CROW_HATCH sound | Hatching begins | Hatch sound at nest position |
| CROW_FLEDGLING sound | Chick appears | Fledgling sound at nest position |
| CROW_BABY_FLIGHT sound | Baby flies away | Baby flight sound at nest position |
| CROW_GROW sound | Baby crow ages to adult | Grow sound plays on crow entity |
| Swarm distress | Attack a crow | Existing: distress sound + particles on clients |
| Scavenge visual | Crow picks up item | Existing: ITEM_PICKUP sound + POOF particles |

### 5.3 PAWS Compliance Verification

| Pillar | Check | Method |
|--------|-------|--------|
| **P**erformance | Renderer uses early-return for IDLE stage | Code review |
| **P**erformance | No per-tick allocations in renderer | Code review (pre-allocate buffers) |
| **A**uditability | Sound playback logged in dev mode | Add `LOGGER.debug()` for each sound trigger |
| **W**orkability | Renderer handles null level gracefully | Null check in constructor |
| **W**orkability | Sound playback guarded by `!level.isClientSide()` | Code review |
| **S**calability | Renderer is self-contained, no shared state | Code review |

---

## 6. File Inventory

### New Files (2)

| File | Package | Lines (est.) |
|------|---------|--------------|
| `CrowNestBlockEntityRenderer.java` | `com.crowbuddy.client.renderer` | ~80 |
| `LLD_PHASE_5.md` | `docs/` | — |

### Modified Files (4)

| File | Change Summary | LOC Impact |
|------|----------------|------------|
| `CrowBuddyClient.java` | Add nest renderer registration, remove stubs | +3, -9 |
| `CrowNestBlockEntity.java` | Add `playSound()` helper, wire 4 sounds | +15 |
| `CrowEntity.java` | Add `CROW_MATE` sound, override `ageUp()` | +8 |
| `TEST_PLAN.md` | Update checklist with Phase 5 items | +5 |

---

## 7. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| GeckoLib API mismatch for baby model | Medium | `CrowGeoModel` already uses `IS_BABY` data ticket; verify at build time |
| BlockEntityRenderer null level (client BE) | Medium | Guard with `level == null` check in render method |
| Sound playback on client side | Low | All `playSound` calls guarded by `!level.isClientSide()` |
| Egg/chick rendering occlusion | Low | Nest block has `noOcclusion()`; use `RenderType.entityTranslucent()` |
| Performance: per-tick Tesselator allocation | Medium | Pre-allocate `BufferBuilder` or use `RenderType` with `Tesselator.getInstance()` |

---

## 8. Confidence Level

**High** — All dependencies are available (GeckoLib baby model exists, sound events registered, block entity infrastructure in place). Primary risk is renderer implementation detail, which is straightforward Minecraft rendering.

---

## 9. Path to Improvement (Post-Phase 5)

| Item | Phase | Notes |
|------|-------|-------|
| Custom particle types | Phase 6 | Replace vanilla particles with crow-specific particles |
| Mixin integration | Phase 6 | If vanilla hooking needed (e.g., feather drops) |
| `onNonCrowDamaged()` logic | Phase 6 | Crows defending non-player entities |
| Nest model variants | Phase 6 | Different nest appearances per biome |
