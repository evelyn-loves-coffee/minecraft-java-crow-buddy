# Crow Buddy: Phase 5 Low-Level Design (LLD)

## 1. Client Rendering

- `CrowGeoModel` selected adult and baby geometry based on entity age.
- `CrowNestBlockEntityRenderer` rendered eggs during `EGGS` and applied a position-derived static tilt during `HATCHING`; the tilt was not animated over time.
- The renderer omitted fledgling and remnant stages because hatch completion removed the nest.

## 2. Sound Mapping

| Sound | Trigger |
|---|---|
| `CROW_MATE` | Breeding completed |
| `CROW_EGG_LAY` | A parent constructed and started a nest |
| `CROW_HATCH` | Incubation entered hatching |
| `CROW_FLEDGLING` | A baby spawned and the nest disappeared |
| `CROW_GROW` | A baby aged into an adult |
| `CROW_DISTRESS` | Swarm or distress behavior activated |
| `CROW_BABY_FLIGHT` | The event was registered and retained for planned audio, but no gameplay trigger was enacted |

The checked-in `.ogg` files remained placeholders. Candidate source links were retained in `SOUNDS.md`.

## 3. Verification

- Automated verification used `./gradlew clean build --warning-mode all`.
- In-game verification was designed to cover model rendering, sound timing, parent travel, hatch removal, and trampling.
