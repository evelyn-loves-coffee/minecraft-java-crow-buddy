# Crow Buddy: Master Implementation Plan

## 1. Project Overview
The Crow Buddy mod aims to add a vanilla-plus, tameable crow entity to Minecraft. It mirrors real-life crow presence through biome-specific spawning and shares behaviors with Parrots (flight, shoulder-sitting) while introducing unique crow-centric mechanics.

## 1.1. Overall Status
| Phase | Status | Completion |
| :--- | :--- | :--- |
| Phase 1: Dependency & Environment Setup | ✅ Complete | 100% |
| Phase 2: Foundation - Registration & Data Generation | ✅ Complete | 100% (DataGen deferred to Phase 5) |
| Phase 3: Networking & Core Mechanics | ⏳ Not Started | 0% |
| Phase 4: World & Environment | ⏳ Not Started | 0% |
| Phase 5: Polishing & Verification | ⏳ Not Started | 0% |

## 2. Technical Choices & Design Decisions
| Feature | Choice | Rationale |
| :--- | :--- | :--- |
| **Animation** | GeckoLib | Supports complex, fluid, and highly detailed crow-like behaviors. |
| **Data Storage** | Data Components | Leverages modern, optimized, and strongly-typed Minecraft 26.2+ architecture. |
| **Dependencies** | GeckoLib (via `implementation`) | Integrated as a prerequisite dependency. |
| **Assets** | Minimal structure/Placeholder | Implement minimal structure (lang files, dummy textures) in Phase 1. |
| **Registration** | Minecraft 26.2 Modern API | Align with "Tiny Takeover" standards and future-proof the mod. |

## 3. Roadmap to v1.0

### Phase 1: Dependency & Environment Setup
*   ~~Update `build.gradle` with GeckoLib dependencies (`modImplementation`).~~
*   ~~Verify build with `./gradlew build`.~~
*   ~~Finalize mixin configurations (e.g., `crowbuddy.client.mixins.json`).~~
*   ~~Create minimal asset directory structure (`geckolib/models`, `geckolib/animations`) with files.~~

### Phase 2: Foundation - Registration & Data Generation
*   ~~Implement centralized `ModEntities` (common) with GeckoLib support.~~
*   ~~Implement `CrowEntity` class with GeckoLib `GeoAnimatable` support.~~
*   ~~Implement `CrowGeoModel` from reference template at `Downloads/crow-model/geckolib5/CrowGeoModel.java` (package → `com.crowbuddy`).~~
*   ~~Implement `CrowRenderer` (extends `GeoEntityRenderer<CrowEntity, LivingEntityRenderState>`).~~
*   ~~Implement centralized `ModClientEntities` (client) for renderer registration via reflection.~~
*   ~~Implement centralized `ModItems` with `BLACK_OIL_SUNFLOWER_SEEDS`.~~
*   ~~Register entity and item in `CrowBuddy.onInitialize()` using MC 26.2 `Registry.register()` API.~~
*   ~~Update `fabric.mod.json` and `build.gradle` for source sets and entrypoints.~~
*   **Deferred to Phase 5**: DataGen pipeline and providers (Fabric API nested jars not resolvable in Loom 1.17 compile classpath).

### Phase 3: Networking & Core Mechanics
*   Implement `ModNetworking` layer for client-server communication.
*   Implement Scavenging logic (Proximity-based acquisition, mouth state, server-side AI).
*   Implement Swarm Intelligence (Distress system: single emission, 8 crows, 32-block radius via `distanceSquared`, `Entity ID` + `BlockPos` payloads).
*   Implement "Sit" behavior and interaction logic.
*   **Implement `CrowBuddyMixin` using the Dispatcher Pattern to trigger server-side swarm/event logic.**

### Phase 4: World & Environment
*   Implement procedural spawning via `BiomeModifications`.
*   Implement Crow Nests as procedural Features with Block Tag compatibility.

### Phase 5: Polishing & Verification
*   **DataGen pipeline**: Initialize Fabric DataGen infrastructure with `DataGeneratorEntrypoint`.
*   Implement DataGen providers for Items, Entities, and Tags (full providers).
*   Finalize assets (models, textures, sounds).
*   Conduct full build and typecheck.
*   Verify registry logs and initialization.
*   Final testing against PAWS standards.
