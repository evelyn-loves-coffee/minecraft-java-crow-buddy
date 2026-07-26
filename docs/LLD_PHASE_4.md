# Crow Buddy: Phase 4 Low-Level Design (LLD)

## 1. Crow Spawning

- Fabric `BiomeModifications` adds crows to overworld creature spawns in all non-ocean, non-river biomes.
- Spawn weight is 1 with groups of one to two crows.
- Crow nests are excluded from chunk generation.

## 2. Breeding Nest Construction

- Vanilla breeding selects one parent to enter `inMatingState`; the other receives its breeding cooldown but does not build.
- `CrowNestBuildGoal` searches exposed canopy positions in expanding rings up to 48 blocks from the parent.
- A site is accepted only when its support belongs to `#minecraft:leaves`, the block above is air, the sky is visible, and the position is inside the world border.
- Search complexity is bounded to 10,125 heightmap columns, or `O(r²)` at radius 48. Invalidated targets are rechecked every 100 ticks for up to 1,200 ticks.
- The parent walks or flies to the site via `CrowNavigator`, revalidates it, places the internal-only nest block, and starts incubation.
- No player-obtainable nest item is registered.

## 3. Lifecycle

1. `IDLE` serves as the safe unloaded and default state.
2. `EGGS` incubates for 12,000 ticks (10 minutes).
3. `HATCHING` lasts for 100 ticks (5 seconds).
4. Hatch completion spawns exactly one baby crow and removes the nest without a drop.

Legacy saved post-hatch stages (3 and 4) are removed on their next server tick without spawning another baby. Removal or decay of the supporting leaves also removes the nest.

Lifecycle transitions trigger sound events:
- `CROW_EGG_LAY`: plays when incubation starts (IDLE → EGGS)
- `CROW_HATCH`: plays when incubation enters hatching (EGGS → HATCHING), with happy villager particles
- `CROW_FLEDGLING`: plays when baby spawns and nest is removed (HATCHING complete), with crit particles

## 4. Trampling

- The block uses a shallow collision shape (box 1-15 x 0-5 x 1-15) that matches its model.
- Non-careful walking (`stepOn`) has a 1-in-100 break chance; landing (`fallOn`) has a 1-in-3 chance.
- Players and living mobs can trample nests; crows and sneaking (carefully stepping) entities cannot.
- Mob trampling respects `mobGriefing`; player trampling does not.
- Trampling plays `minecraft:turtle_egg_break` sound and logs the event at debug level.

## 5. PAWS Verification

| Pillar | Phase 4 rule |
|---|---|
| Performance | Canopy discovery uses a bounded `O(r²)` heightmap search (max 10,125 columns at radius 48) and retries no more frequently than every 100 ticks. |
| Auditability | Nest construction, timeout, and trampling paths emit debug logs; lifecycle stages persist explicit IDs and timers. |
| Workability | Placement is revalidated on arrival, hatch spawning retries after failure, and legacy post-hatch stages are removed without duplicate babies. |
| Scalability | `#minecraft:leaves` enables data-driven foliage compatibility; nest placement is isolated in one AI goal. |

Verification covers lifecycle edge cases, trample eligibility, leaf-support behavior, natural crow spawning, and the absence of nest world generation.
