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
*   **GeckoLib Version:** `5.5.3`

### Assumptions & Verification
| # | Assumption | Risk | Verification Needed |
|---|-----------|------|---------------------|
| 1 | GeckoLib artifact coordinate is `software.bernie.geckolib:geckolib-fabric-${minecraft_version}:${geckolib_version}` | Medium | Verify artifact naming for 5.5.3 release |
| 2 | `ModItems`/`ModEntities` registration pattern is correct for MC 26.2 | High | May have shifted to `Registry.registerForDataLoader` or Fabric `EventRegistry` |
| 3 | GeckoLib should use `modApi` rather than `modImplementation` | Low | Decision based on whether other mods should access GeckoLib through this mod |
| 4 | Loom mod name `"modid"` → `"crowbuddy"` change is necessary | Low | May be purely internal to Loom with no runtime effect |
| 5 | DataGen uses `FabricDataPack` interface with custom `ModDataPack` class | Medium | Decompiled bytecode shows `createPack()` called with no argument — API may be simpler |
| 6 | Networking uses `ServerPlayNetworking` from Fabric API | Low | Channel registration pattern for MC 26.2 unconfirmed |

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
*   Initialize Fabric DataGen pipeline.
*   Implement DataGen providers for Items, Entities, and Tags.

### Phase 3: Networking & Core Mechanics
*   Implement `ModNetworking` layer for client-server communication.
*   Implement Scavenging logic (Item retrieval in mouth, weighted priorities, frequency).
*   Implement Swarm Intelligence (Distress system: triggers, rules, sound management).
*   Implement "Sit" behavior and its interaction with other behaviors.

### Phase 4: World & Environment
*   Implement procedural spawning (temperature, humidity, forest density analysis).
*   Implement Crow Nests as procedural Features with Block Tag compatibility.

### Phase 5: Polishing & Verification
*   Finalize assets (models, textures, sounds).
*   Conduct full build and typecheck.
*   Verify registry logs and initialization.
*   Final testing against PAWS standards.

## 4. Technical Unknowns & Open Questions
*   **Item Retrieval:** Specific physics/logic for "grabbing" and "carrying" items in the mouth.
*   **Swarm Logic:** Signaling mechanism (broadcast vs proximity) for the distress system.
*   **Procedural Spawning:** Hooking into biome generation for nests.
*   **Sound Management:** Screeching duration and cancellation logic.
*   **Asset Specs:** Final visual design requirements.
*   **Registration Pattern:** Confirming correct registry pattern for MC 26.2.
*   **Mixin Utility:** Decision on keeping the empty `CrowBuddyMixin`.
*   **DataGen Scope:** Whether to include actual providers or just scaffolding in initial pre-requisites.
*   **Placeholder Assets:** Timing of creating lang files, item models, and dummy textures.
