# Design Decisions & Open Questions

## Assumptions
| # | Assumption | Risk | Verification Needed |
|---|-----------|------|---------------------|
| 1 | GeckoLib artifact coordinate is `software.bernie.geckolib:geckolib-fabric-${minecraft_version}:${geckolib_version}` on Modrinth | Medium | Verify artifact naming for 5.5.3 release |
| 2 | `ModItems`/`ModEntities` registration pattern (direct `BuiltInRegistries` registration) is correct for MC 26.2 | High | May have shifted to `Registry.registerForDataLoader` or Fabric `EventRegistry` |
| 3 | GeckoLib should use `modApi` (exposed to other mods) rather than `modImplementation` | Low | Decision based on whether other mods should access GeckoLib through this mod |
| 4 | Loom mod name `"modid"` $\to$ `"crowbuddy"` change is necessary | Low | May be purely internal to Loom with no runtime effect |
| 5 | DataGen uses `FabricDataPack` interface with custom `ModDataPack` class | Medium | Decompiled bytecode shows `createPack()` called with no argument — API may be simpler |
| 6 | Networking uses `ServerPlayNetworking` from Fabric API | Low | Channel registration pattern for MC 26.2 unconfirmed |

## Open Questions
| # | Question | Impact | Decision Needed |
|---|----------|--------|-----------------|
| 1 | Should the empty `CrowBuddyMixin` (injecting `MinecraftServer.loadLevel()`) be kept or removed? | Unnecessary mixin overhead if unused | Keep as placeholder for future server-side hooks, or remove to clean up |

## Resolved Decisions
| # | Decision | Detail |
|---|----------|--------|
| 1 | **GeckoLib Integration** | Add as a prerequisite dependency immediately. |
| 2 | **Registration API** | Use the Minecraft 26.2 modern registration patterns. |
| 3 | **Dependency Scope** | Use `modImplementation` (internal to mod). |
| 4 | **DataGen Scope** | Implement full providers (Items, Entities, Tags) from start. |
| 5 | **Asset Readiness** | Implement minimal structure (lang files, dummy textures) in Phase 1. |
