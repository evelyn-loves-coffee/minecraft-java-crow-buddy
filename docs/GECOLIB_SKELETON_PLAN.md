# GeckoLib Skeleton Implementation Plan

## Overview
This document outlines the implementation plan for the GeckoLib skeleton for the Crow Buddy mod. The goal is to establish the necessary plumbing for GeckoLib without committing to specific crow animations yet.

## Phase 0: Environment Setup
*   **Task 0.1:** Verify or initialize the `gradlew` wrapper and ensure `gradle.properties` is correctly configured for a baseline build.

## Phase 1: Dependency & Configuration
*   **Task 1.1:** Update `gradle.properties` to define the GeckoLib version (`5.5.3`).
*   **Task 1.2:** Update `build.gradle` to include the GeckoLib dependency.
*   **Task 1.3:** Update `fabric.mod.json` to include GeckoLib as a hard dependency.
*   **Task 1.4:** Execute `./gradlew build` to verify dependency resolution, artifact availability, and compilation.

## Phase 2: Asset Structure Scaffolding
*   **Task 2.1:** Create the required directory structure in `src/main/resources/assets/crowbuddy/` for:
    *   `geo` (Models)
    *   `animations` (Animation files)
*   **Task 2.2:** Create minimal placeholder files in these directories to ensure the pathing is correctly recognized by the loader.

## Phase 3: Registration Framework
*   **Task 3.1:** Create a centralized `ModEntities` class (common) to handle the registration of `GeoEntity` types.
*   **Task 3.2:** Create a `ModClientEntities` class (client) to handle the registration of `GeoEntityRenderer` and `GeoAnimatableInstance`.
*   **Task 3.3:** Wire these registries into the `CrowBuddy` and `CrowBuddyClient` entrypoints.

## Phase 4: DataGen Integration
*   **Task 4.1:** Initialize the Fabric DataGen pipeline using the standard `FabricDataPack` interface.
*   **Task 4.2:** Implement placeholder providers for GeckoLib models and animations within the DataGen pipeline.

## Phase 5: Verification
*   **Task 5.1:** Perform a full build and typecheck to ensure no registry conflicts or missing dependencies.
*   **Task 5.2:** Verify that the mod is correctly identifying the GeckoLib framework during initialization via registry logs.