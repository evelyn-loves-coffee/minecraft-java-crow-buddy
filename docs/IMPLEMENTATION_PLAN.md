# Implementation Plan: Crow Buddy

## Overview
This document outlines the technical roadmap for implementing the Crow Buddy mod, adhering to the PAWS (Performance, Auditability, Workability, Scalability) standard.

## Decisions & Technical Choices
| Feature | Choice | Rationale |
| :--- | :--- | :--- |
| **Animation** | GeckoLib | Supports complex, fluid, and highly detailed crow-like behaviors. |
| **Data Storage** | Data Components | Leverages modern, optimized, and strongly-typed Minecraft 26.2+ architecture. |
| **Dependencies** | Situational | Use libraries only when they provide high benefit/efficiency without unnecessary bloat. |
| **Assets** | Placeholders | Initial development will use placeholder assets until final designs are available. |

## Pre-requisite Roadmap
The following steps must be completed before implementing core functionality:

1. **Dependency Management**
   - Integrate GeckoLib into `build.gradle`.
2. **Environment Configuration**
   - Create `src/main/resources/crowbuddy.client.mixins.json`.
   - Ensure all entrypoints and mixin configurations are valid.
3. **Foundation: Registration Framework**
   - Implement centralized `ModItems` using Data Components.
   - Implement centralized `ModEntities` with GeckoLib support.
   - Implement centralized `ModClientEntities` for renderers.
4. **Foundation: Data Generation (DataGen)**
   - Setup Fabric DataGen pipeline.
   - Implement providers for Items, Entities, and Tags (essential for diet/feeding logic).
5. **Foundation: Networking**
   - Implement a robust `ModNetworking` layer for client-server communication (required for Swarm mechanics).

## Remaining Technical Unknowns
* **Item Retrieval Mechanics:** Specific physics/logic for "grabbing" and "carrying" items in the mouth.
* **Swarm Logic:** The specific signaling mechanism (broadcast/proximity) for the distress system.
* **Procedural Spawning Algorithm:** The method for hooking into biome generation for procedural nesting/spawning.
* **Sound Management:** Logic for the "Screeching" duration and cancellation.
* **Model/Texture Specifications:** Final visual design requirements.
