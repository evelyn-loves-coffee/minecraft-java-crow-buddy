# Design Decisions & Open Questions

## Assumptions

| # | Assumption | Risk | Verification Needed |
|---|-----------|------|---------------------|
| 1 | GeckoLib artifact coordinate is `software.bernie.geckolib:geckolib-fabric-${minecraft_version}:${geckolib_version}` on Modrinth | Medium | Verify artifact naming for 5.5.3 release |
| 2 | `ModItems`/`ModEntities` registration pattern (direct `BuiltInRegistries` registration) is correct for MC 26.2 | High | May have shifted to `Registry.registerForDataLoader` or Fabric `EventRegistry` |
| 3 | GeckoLib should use `modApi` (exposed to other mods) rather than `modImplementation` | Low | Decision based on whether other mods should access GeckoLib through this mod |
| 4 | Loom mod name `"modid"` → `"crowbuddy"` change is necessary | Low | May be purely internal to Loom with no runtime effect |
| 5 | DataGen uses `FabricDataPack` interface with custom `ModDataPack` class | Medium | Decompiled bytecode shows `createPack()` called with no argument — API may be simpler |
| 6 | Networking uses `ServerPlayNetworking` from Fabric API | Low | Channel registration pattern for MC 26.2 unconfirmed |

## Open Questions

| # | Question | Impact | Decision Needed |
|---|----------|--------|-----------------|
| 1 | Should GeckoLib be added as a pre-requisite dependency now, or deferred until Crow renderer/model implementation? | Build complexity vs. readiness | Add now for completeness, or defer to reduce initial dependency surface |
| 2 | Should the empty `CrowBuddyMixin` (injecting `MinecraftServer.loadLevel()`) be kept or removed? | Unnecessary mixin overhead if unused | Keep as placeholder for future server-side hooks, or remove to clean up |
| 3 | Should DataGen pre-requisites include actual providers (item tags, spawn rules) or just empty pipeline scaffolding? | Scope of pre-requisite work | Scaffolding only keeps it focused; providers add immediate value for feeding logic |
| 4 | Should placeholder assets (lang file, item model JSON, dummy textures) be created during pre-requisites? | Asset structure readiness | Create minimal structure now, or defer until items/entities have actual definitions |

## Resolved Decisions
<!-- Add resolved decisions here as they are answered -->
