# Crow Buddy: Phase 4 Low-Level Design (LLD)

## 1. Crow Spawning

- Fabric `BiomeModifications` added crows to overworld creature spawns.
- Spawn weight was set to 2 with groups of two to four crows.
- Crow nests were excluded from chunk generation.

## 2. Breeding Nest Construction

- Vanilla breeding selected one parent to enter `inMatingState`; the other received its breeding cooldown but did not build.
- `CrowNestBuildGoal` searched exposed canopy positions in expanding rings up to 16 blocks from the parent.
- A site was accepted only when its support belonged to `#minecraft:leaves`, the block above was air, the sky was visible, and the position was inside the world border.
- Search complexity was bounded to 1,089 heightmap columns, or `O(r²)` at radius 16. Invalidated targets were rechecked every 100 ticks for up to 1,200 ticks.
- The parent walked or flew to the site, revalidated it, placed the internal-only nest block, and started incubation.
- No player-obtainable nest item was registered.

## 3. Lifecycle

1. `IDLE` served as the safe unloaded and default state.
2. `EGGS` incubated for 12,000 ticks.
3. `HATCHING` lasted for 100 ticks.
4. Hatch completion spawned exactly one baby and removed the nest without a drop.

Legacy saved post-hatch stages were designed to be removed on their next server tick without spawning another baby. Removal or decay of the supporting leaves also removed the nest.

## 4. Trampling

- The block received a shallow collision shape that matched its model.
- Non-careful walking received a 1-in-100 break chance, while landing received a 1-in-3 chance.
- Players and living mobs could trample nests; crows and sneaking entities could not.
- Mob trampling respected `mobGriefing`, while player trampling did not.

## 5. PAWS Verification

| Pillar | Phase 4 rule |
|---|---|
| Performance | Canopy discovery used a bounded `O(r²)` heightmap search and retried no more frequently than every 100 ticks. |
| Auditability | Nest construction, timeout, and trampling paths emitted debug logs, while lifecycle stages persisted explicit IDs and timers. |
| Workability | Placement was revalidated on arrival, hatch spawning retried after failure, and legacy post-hatch stages were removed without duplicate babies. |
| Scalability | `#minecraft:leaves` enabled data-driven foliage compatibility, and nest placement remained isolated in one AI goal. |

Verification covered lifecycle edge cases, trample eligibility, leaf-support behavior, natural crow spawning, and the absence of nest world generation.
