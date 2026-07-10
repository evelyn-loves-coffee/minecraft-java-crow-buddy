# Crow Buddy: Master Implementation Plan

## 1. Project Overview
The Crow Buddy mod aims to add a vanilla-plus, tameable crow entity to Minecraft. It mirrors real-life crow presence through biome-specific spawning and shares behaviors with Parrots (flight, shoulder-sitting) while introducing unique crow-centric mechanics.

### Implementation Strategy
*   **Approach:** "From Scratch" to allow for custom flight physics, unique AI goals, and optimized performance.
*   **NBT Tracking:**
    *   `Crow_Satiation`: Tracks health/food levels to determine behavior frequency.
    *   `Crow_Relationship`: Tracks player aggression.
*   **Technical Standards:**
    *   **PAWS:** Performance, Auditability, Workability, Scalability.
    *   **Entity Standards:** Adheres to "Tiny Takeover" (Minecraft 26.1+) overhaul patterns.

## 2. Technical Choices & Design Decisions
| Feature | Choice | Rationale |
| :--- | :--- | :--- |
| **Animation** | GeckoLib | Supports complex, fluid, and highly detailed crow-like behaviors. |
| **Data Storage** | Data Components | Leverages modern, optimized, and strongly-typed Minecraft 26.2+ architecture. |
| **Dependencies** | Situational | Use libraries only when they provide high benefit/efficiency without unnecessary bloat. |
| **Assets** | Placeholders | Initial development will use placeholder assets until final designs are available. |

### Resolved Decisions
*   **Mod ID:** `crowbuddy`
*   **GeckoLib Version:** `5.5.3` (via `modApi`)
*   **Build Config:** `geckolibVersion=5.5.3` in `gradle.properties`
*   **Registration Pattern:** Fabric `EventRegistry`
*   **Item Retrieval:** Item in mouth state
*   **Distress System:** Single emission, affects 8 crows within 32 blocks. Packets include Initiator Entity ID and 4-second TTL.
*   **Scavenging System:** Server-side AI logic; Networking used to sync "Item in Mouth" and "Drop Off" states.
*   **Networking Payloads:** 
    *   `DistressPacket`: `initiatorId` (int), `timestamp` (long).
    *   `ScavengingSync`: `entityId` (int), `itemId` (int), `count` (int), `mouthPosition` (float[3]).
    *   `DropOffPacket`: `entityId` (int), `itemId` (int), `count` (int), `dropLocation` (float[3]).
*   **DataGen Strategy:** Phase 2: Scaffolding/Base classes. Phase 3/4: Content/Metadata providers.
    *   *Providers:* `ItemProvider`, `EntityProvider`, `TagProvider`, `LootTableProvider`.
*   **Procedural Spawning:** Simple spawn table based on tags
*   **Sound Management:** 4-second timer on screech; stops immediately if crow dies or sits.
*   **Crow Nest Mechanics:** Craftable block. Only drops with Silk Touch.
*   **Defense & Relationship Logic:**
    *   *Untamed:* Immediate Distress/Swarm on player attack.
    *   *Tamed:* 30s sliding window. Hit 1: Single attack. Hit 2+: Distress/Swarm.
    *   *Interaction Override:* Owner right-click always forces "Sitting" state (even during Distress).

### Assumptions & Verification
| # | Assumption | Risk | Verification Needed |
|---|-----------|------|---------------------|
| 1 | GeckoLib artifact coordinate is `software.bernie.geckolib:geckolib-fabric-${minecraft_version}:${geckolib_version}` | Low | Confirmed for 5.5.3 |
| 2 | `ModItems`/`ModEntities` registration pattern is correct for MC 26.2 | Low | Confirmed: Use Fabric `EventRegistry` |
| 3 | GeckoLib should use `modApi` rather than `modImplementation` | Low | Confirmed: Use `modApi` |
| 4 | Loom mod name `"modid"` → `"crowbuddy"` change is necessary | Low | May be purely internal to Loom with no runtime effect |
| 5 | DataGen uses simplified `FabricDataPack` interface | Low | Confirmed: `createPack()` called with no argument |
| 6 | Networking uses `ServerPlayNetworking` from Fabric API | Low | Confirmed: Standard channel registration |


## 3. Roadmap to v1.0

### Phase 1: Dependency & Environment Setup (Current Focus)
*   Update `build.gradle` with GeckoLib dependencies.
*   Verify build with `./gradlew build`.
*   Finalize mixin configurations (e.g., decide on `CrowBuddyMixin` utility).
*   Create asset directory structure (`geo`, `animations`) with placeholder files.

### Phase 2: Foundation - Registration & Data Generation
*   Implement centralized `ModEntities` (common) with GeckoLib support.
*   Implement centralized `ModClientEntities` (client) for renderers/animations.
*   Implement centralized `ModItems` using Data Components.
*   Wire registries into `CrowBuddy` and `CrowBuddyClient` entrypoints.
*   Initialize Fabric DataGen pipeline (Scaffolding).
*   Implement DataGen providers for Items, Entities, and Tags (Base classes).

### Phase 3: Networking & Core Mechanics
*   Implement `ModNetworking` layer for client-server communication (Payloads & Channels).
*   Implement Scavenging logic (Item in mouth state, Server-side AI, State Sync).
*   Implement Swarm Intelligence (Distress system: single emission, 8 crows, 32 blocks, Initiator ID + 4s TTL).
*   Implement "Sit" behavior and its interaction with other behaviors.

### Phase 4: World & Environment
*   Implement procedural spawning (Simple spawn table based on tags).
*   Implement Crow Nests as procedural Features with Block Tag compatibility.

### Phase 5: Polishing & Verification
*   Finalize assets (models, textures, sounds).
    *   Required: `.geo.json` (Geometry), `.animation.json` (Animations), `.png` (Textures), `.ogg` (Audio).
*   Conduct full build and typecheck.
*   Verify registry logs and initialization.
*   Final testing against PAWS standards.

## 4. Technical Unknowns & Open Questions
*   **Placeholder Assets:** Timing of creating lang files, item models, and dummy textures.

