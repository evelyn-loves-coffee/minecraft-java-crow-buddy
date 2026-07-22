# Crow Buddy: Phase 2 Low-Level Design (LLD)

## 1. Initialization Order

`CrowBuddy.onInitialize()` established common registrations in dependency order:

1. It registered the crow entity type and its attributes.
2. It registered the internal crow-nest block and block-entity type.
3. It registered gameplay items and the crow spawn egg.
4. It registered crow sound events.
5. It registered clientbound networking payload types.
6. It registered gameplay event callbacks and sunflower loot modification.
7. It added natural crow spawning.
8. It registered per-level swarm cleanup on server-level unload.

All identifiers were created through `CrowBuddy.id(path)`, so registrations consistently used the `crowbuddy` namespace.

## 2. Entity Foundation

- `ModEntities.CROW` registered a `MobCategory.CREATURE` entity measuring 0.4 × 0.6 blocks.
- Client tracking range was set to 64 blocks with an update interval of one tick.
- `CrowEntity` extended `TamableAnimal` and implemented GeckoLib's `GeoAnimatable` contract.
- Attribute registration supplied health, movement, follow range, temptation range, and attack damage.
- Entity registration and attribute setup emitted logs; reflection failures during default-attribute installation were caught and logged rather than hidden.

## 3. Client Foundation

The client entry point performed client-only setup:

1. It registered the crow renderer.
2. It registered distress and scavenge packet receivers.
3. It registered the nest block-entity renderer.
4. It added the spawn egg and ingredient items to vanilla creative tabs.

`CrowGeoModel` selected adult or baby geometry, animation, and texture resources. Client classes were not loaded from the common entry point.

## 4. Block, Item, Sound, and Payload Registration

- The crow nest was retained as a registered block and block entity because breeding AI created it directly; no player-obtainable nest item was registered.
- Registered items were black oil sunflower seeds, black feathers, and the crow spawn egg.
- Black feathers participated in the `crowbuddy:feathers` item tag and compatibility recipes.
- Seven crow sound events were registered from one ordered collection. Their JSON definitions and placeholder `.ogg` resources remained data-driven.
- `DistressPayload` and `ScavengePayload` were registered as clientbound play payloads with typed codecs. Sends were guarded by `ServerPlayNetworking.canSend`.

## 5. Data and Resource Pipeline

- Fabric Data Generation was enabled with client resources in `build.gradle`.
- The repository did not define custom DataGen provider classes; block states, models, tags, loot tables, recipes, language entries, sounds, and GeckoLib assets were maintained as checked-in resources.
- Resource identifiers mirrored registry identifiers so runtime lookup remained deterministic.
- Resource processing injected the project version into mod metadata but otherwise preserved authored JSON and binary assets.

## 6. Registration Invariants

- Common registries were mutated only during common initialization.
- Renderer and client receiver registration occurred only during client initialization.
- Registry IDs were unique and namespace-qualified.
- The nest block existed without an item form, which prevented players from bypassing crow-driven construction.
- Natural spawning registered crows only; nests were not injected into world generation.

## 7. PAWS Verification

| Pillar | Phase 2 rule |
|---|---|
| Performance | Registration was one-time startup work; network sends checked client capability first. |
| Auditability | Registration groups logged their initialization and used centralized identifiers. |
| Workability | Common/client boundaries prevented dedicated-server class-loading failures. |
| Scalability | Entities, blocks, items, sounds, networking, and client rendering used separate registration modules. |

Verification required a successful clean build, registry startup logs in a development client, and resource loading without missing-model, missing-sound, or duplicate-ID errors.
