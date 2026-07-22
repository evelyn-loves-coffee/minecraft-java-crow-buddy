# Crow Buddy: Phase 5 Low-Level Design (LLD)

## 1. Client Rendering

- `CrowGeoModel` selects adult and baby geometry based on entity age.
- `CrowNestBlockEntityRenderer` renders eggs during `EGGS` and applies a position-derived static tilt during `HATCHING`; it does not currently animate that tilt over time.
- The renderer has no fledgling or remnant stage because hatch completion removes the nest.

## 2. Sound Mapping

| Sound | Trigger |
|---|---|
| `CROW_MATE` | Breeding completes |
| `CROW_EGG_LAY` | Parent constructs and starts a nest |
| `CROW_HATCH` | Incubation enters hatching |
| `CROW_FLEDGLING` | Baby spawns and nest disappears |
| `CROW_GROW` | Baby ages into an adult |
| `CROW_DISTRESS` | Swarm/distress behavior |
| `CROW_BABY_FLIGHT` | Registered and retained for planned audio, but currently has no gameplay trigger |

The checked-in `.ogg` files remain placeholders. Candidate source links are retained in `SOUNDS.md`.

## 3. Verification

- Automated verification uses `./gradlew clean build --warning-mode all`.
- In-game verification covers model rendering, sound timing, parent travel, hatch removal, and trampling.
