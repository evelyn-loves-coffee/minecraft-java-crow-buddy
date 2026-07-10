# Crow Buddy: Master Implementation Plan

## 1. Project Overview
The Crow Buddy mod aims to add a vanilla-plus, tameable crow entity to Minecraft. It mirrors real-life crow presence through biome-specific spawning and shares behaviors with Parrots (flight, shoulder-sitting) while introducing unique crow-centric mechanics.

## 2. Technical Choices & Design Decisions
| Feature | Choice | Rationale |
| :--- | :--- | :--- |
| **Animation** | GeckoLib | Supports complex, fluid, and highly detailed crow-like behaviors. |
| **Data Storage** | Data Components | Leverages modern, optimized, and strongly-typed Minecraft 26.2+ architecture. |
| **Dependencies** | GeckoLib (via `modImplementation`) | Integrated as a prerequisite dependency. |
| **Assets** | Minimal structure/Placeholder | Implement minimal structure (lang files, dummy textures) in Phase 1. |
| **Registration** | Minecraft 26.2 Modern API | Align with "Tiny Takeover" standards and future-proof the mod. |

## 3. Roadmap to v1.0

### Phase 1: Dependency & Environment Setup
*   Update `build.gradle` with GeckoLib dependencies (`modImplementation`).
*   Verify build with `./gradlew build`.
*   Finalize mixin configurations (e.g., `crowbuddy.client.mixins.json`).
*   Create minimal asset directory structure (`geo`, `animations`) with placeholder files.

### Phase 2: Foundation - Registration & Data Generation
*   Implement centralized `ModEntities` (common) with GeckoLib support.
*   Implement centralized `ModClientEntities` (client) for renderers/animations.
*   Implement centralized `ModItems` using Data Components.
*   Initialize Fabric DataGen pipeline.
*   Implement DataGen providers for Items, Entities, and Tags (full providers).

### Phase 3: Networking & Core Mechanics
*   Implement `ModNetworking` layer for client-server communication.
*   Implement Scavenging logic (Proximity-based acquisition, mouth state, server-side AI).
*   Implement Swarm Intelligence (Distress system: single emission, 8 crows, 32-block radius via `distanceSquared`, `Entity ID` + `BlockPos` payloads).
*   Implement "Sit" behavior and interaction logic.

### Phase 4: World & Environment
*   Implement procedural spawning via `BiomeModifications`.
*   Implement Crow Nests as procedural Features with Block Tag compatibility.

### Phase 5: Polishing & Verification
*   Finalize assets (models, textures, sounds).
*   Conduct full build and typecheck.
*   Verify registry logs and initialization.
*   Final testing against PAWS standards.
