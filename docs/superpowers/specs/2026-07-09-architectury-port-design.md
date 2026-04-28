# MachineLib → Architectury (NeoForge + Fabric) Port — Design

**Date:** 2026-07-09
**Branch:** `feat/architectury-multiloader`
**Status:** Approved (Approach A) — proceeding to phased implementation

## 1. Goal

Port MachineLib from a Fabric-only library to an Architectury-based multiloader
library that publishes artifacts for **both NeoForge and Fabric** on Minecraft
**1.21.1**. The downstream consumer (Galacticraft) is also moving multiloader, so
a **breaking, platform-neutral public API redesign is acceptable** and will be
coordinated in lockstep.

## 2. Decisions locked in (from brainstorming)

| Decision | Choice |
|---|---|
| API stability | Breaking redesign OK — Galacticraft updated in lockstep |
| Target MC version | Stay on **1.21.1** (isolate the port from MC API churn) |
| Sequencing | **Phased**: restructure → keep Fabric green → stand up NeoForge |
| Integrations (REI/JEI/EMI/WTHIT/Cloth/ModMenu) | **Core first**, integrations in a later phase |
| Core abstraction | **Approach A** — neutral core in `common`, platform-native adapters at the edge |

## 3. Why this port is tractable

MachineLib's storage engine is *already* platform-neutral, and the two worst
multiloader obstacles are absent:

- **No mixins.** Nothing to split into per-loader mixin configs.
- **No access widener / access transformer.** No cross-loader accessor bridging.
- **Registration is vanilla.** Only `BuiltInRegistries` reads and a single
  `Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, …)` — both work
  unchanged on NeoForge and Fabric.
- **`FabricLoader` used in only 2 spots** — config dir and an `isModLoaded`
  check — both map directly to Architectury `Platform`.
- **Core storage is generic over resource type** (`ResourceStorage<Resource,
  Slot>`, `long` amounts, neutral slots/filters/serialization). Fabric types
  leak at exactly two boundary points (see §5).

The realistic difficulty is concentrated in: the transfer/energy/fluid
**exposure boundary**, **fluid units**, **client rendering** (FRAPI vs NeoForge
model data + fluid attributes), **networking**, and **menu opening data**.

## 4. Target module structure

Standard Architectury multiloader layout using **architectury-loom** +
**architectury-plugin**:

```
MachineLib/
  settings.gradle.kts        # includes :common, :fabric, :neoforge
  build.gradle.kts           # shared config via subprojects/allprojects
  gradle.properties          # + neoforge.version, architectury plugin versions
  common/                    # loader-agnostic: MC + Architectury API only
    src/main/java/…/machinelib/   (api + impl neutral core)
    src/main/resources/           (assets, data, common lang/textures)
  fabric/
    src/main/java/…/machinelib/fabric/   (platform impls, entrypoints)
    src/main/resources/fabric.mod.json
    src/testmod + src/test                (gametests, unit tests — Fabric hosts them)
  neoforge/
    src/main/java/…/machinelib/neoforge/ (platform impls, entrypoints)
    src/main/resources/META-INF/neoforge.mods.toml
```

- `common` compiles against Minecraft + `architectury` (common) only — no
  `net.fabricmc.*`, no `net.neoforged.*`.
- Platform-specific behavior in `common` is reached via Architectury
  `@ExpectPlatform` static bridges and a small number of hand-rolled service
  interfaces loaded per platform.
- The existing `api/` vs `impl/` package split is preserved; most files move to
  `common` unchanged.

## 5. Core abstraction (Approach A)

### 5.1 The two Fabric leak points to sever

1. `ResourceStorage.getExposedStorage(ResourceFlow)` returns
   `ExposedStorage extends Storage<Variant>` (Fabric).
2. `MachineEnergyStorage extends EnergyStorage` and
   `ExposedEnergyStorage extends EnergyStorage` (team-reborn, Fabric).

### 5.2 New neutral contract (in `common`)

- `MachineEnergyStorage`, `MachineItemStorage`, `MachineFluidStorage`, and
  `ResourceSlot` expose **only** MachineLib-native methods (amount/capacity/
  insert/extract/predicates — most already exist). They no longer extend any
  loader type.
- **Exposure becomes registration-driven, not a return value.** `common` no
  longer hands out a Fabric `Storage`. Instead a platform SPI registers each
  machine block-entity type's storages as the platform-native capability:
  - **Fabric** (`fabric` sourceset): adapters implementing
    `Storage<ItemVariant>` / `Storage<FluidVariant>` and team-reborn
    `EnergyStorage`, registered via `ItemStorage.SIDED` / `FluidStorage.SIDED` /
    `EnergyStorage.SIDED` `registerForBlockEntity(...)`.
  - **NeoForge** (`neoforge` sourceset): adapters implementing `IItemHandler` /
    `IFluidHandler` / `IEnergyStorage`, registered in `RegisterCapabilitiesEvent`
    against `Capabilities.Item/Fluid/EnergyStorage.BLOCK`.
- Adapters wrap the neutral `ResourceStorage`/slots through their existing
  read/insert/extract methods, honoring `ResourceFlow` (input/output/both) and
  per-slot filters. The Fabric adapters port the existing `impl/compat/transfer/
  Exposed*Impl` classes; the NeoForge adapters are new but structurally parallel.

### 5.3 Fluid representation and units

- **Internal neutral unit stays droplets (81 000 / bucket)** — MachineLib's
  current unit. No change to the core or to the numbers Galacticraft uses.
- The **NeoForge** `IFluidHandler` adapter converts at the boundary:
  `1 mB = 81 droplets` (81 000 / 1 000). Conversions floor to whole mB on
  export and multiply on import; sub-mB remainders are held internally and never
  lost (the adapter reports floor, keeps the remainder in the neutral store).
- Fluid *identity* stays `net.minecraft.world.level.material.Fluid` +
  data-component patch, which both loaders share. Fabric wraps it in
  `FluidVariant`; NeoForge wraps it in `FluidStack` — only inside the adapters.

### 5.4 Energy width

- MachineLib energy is `long`; Fabric team-reborn energy is `long` (no change).
  **NeoForge `IEnergyStorage` is `int`** — the NeoForge adapter saturates to
  `Integer.MAX_VALUE` on report and clamps requests, so large capacities remain
  representable internally and simply cap the per-op int view.

## 6. Platform touchpoint map

| Concern | Fabric today | Common strategy | NeoForge target |
|---|---|---|---|
| Item/Fluid/Energy exposure | `*.SIDED` lookups + team-reborn energy | Registration SPI (§5.2) | `RegisterCapabilitiesEvent` + caps |
| Block API lookup / cache | `fabric-api-lookup-v1`, `AdjacentBlockApiCache` | Neutral `BlockCapabilityCache`-style helper via `@ExpectPlatform` | NeoForge `BlockCapabilityCache` |
| Networking (4 C2S, 4 S2C payloads) | `fabric-networking-v1` + `PayloadTypeRegistry` | **Architectury `NetworkManager`** with vanilla `StreamCodec` payloads | Architectury `NetworkManager` |
| Menus / extended open data | `ExtendedScreenHandlerType` | **Architectury `MenuRegistry` + `ExtendedMenuProvider`** | Architectury `MenuRegistry` |
| Registration | `BuiltInRegistries` / `Registry.register` | Keep vanilla registration (works on both) | Same |
| Config dir + `isModLoaded` | `FabricLoader` | **Architectury `Platform`** | Architectury `Platform` |
| Data components | `Registry.register(DATA_COMPONENT_TYPE)` | Unchanged (vanilla) | Unchanged |
| Client init | `ClientModInitializer` | Architectury client entry / `@ExpectPlatform` | NeoForge client mod bus |
| Block model / connected look | `fabric-model-loading-v1` + FRAPI mesh | Neutral model hook via `@ExpectPlatform` | NeoForge model + `getModelData()` |
| Render block-entity data | `blockview.v2` `RenderAttachedBlockView` | Neutral accessor via `@ExpectPlatform` | NeoForge `getModelData()` |
| Fluid attributes (name/color/sprite) | `FluidVariantAttributes` | Client fluid-render helper via `@ExpectPlatform` | `IClientFluidTypeExtensions` |
| Gametests + unit tests | `fabric-gametest-v1`, loader-junit | Host in **`fabric`** sourceset (unchanged) | Optional NeoForge gametest later |
| Mod metadata | `fabric.mod.json` | Split | `neoforge.mods.toml` + common `pack.mcmeta` |
| Integrations (REI/JEI/EMI/WTHIT/Cloth/ModMenu) | Fabric entrypoints | **Deferred to a later phase** | Per-loader integration modules |

## 7. Public API changes (breaking — coordinate with Galacticraft)

1. `MachineEnergyStorage` no longer `extends team.reborn…EnergyStorage`.
2. `ResourceStorage.getExposedStorage(...)` removed from the neutral API;
   exposure is registration-driven. Any Galacticraft code that pulled a Fabric
   `Storage` off a MachineLib storage instead queries the platform capability.
3. `ExposedStorage` / `ExposedEnergyStorage` / `ExposedSlot` become
   platform-internal adapter types (moved out of the public `common` API).
4. Fluid amounts remain droplets in the API (no numeric change for consumers).

A short **migration note** for Galacticraft will be produced when Phase 4 lands.

## 8. Phased implementation plan (high level)

Detailed step-by-step plan produced next via the writing-plans skill. Phases:

- **Phase 0 — Toolchain & module skeleton.** architectury-loom + plugin;
  `settings.gradle.kts` with `:common`/`:fabric`/`:neoforge`; shared build
  config; `neoforge.mods.toml` + slimmed `fabric.mod.json`; empty platform
  entrypoints. Builds produce (empty) jars on both loaders.
- **Phase 1 — Move neutral core to `common`, keep Fabric green.** Relocate
  `api`/`impl` neutral code to `common`; move Fabric-specific impls
  (exposure, networking, client, entrypoint) to `fabric`. Sever the two leak
  points (§5.1) behind the new SPI, with the Fabric adapters wired. **Gate:
  Fabric client/server run + all unit tests + gametests pass, zero behavior
  change.**
- **Phase 2 — Networking, menus, config, platform services on Architectury.**
  Move payloads to `NetworkManager`, menus to `MenuRegistry`, config/loader
  checks to `Platform`. Re-verify Fabric green.
- **Phase 3 — Stand up NeoForge core.** NeoForge entrypoint, capability
  registration, `IItemHandler`/`IFluidHandler`/`IEnergyStorage` adapters, block
  capability caches, client model/fluid rendering. **Gate: NeoForge client/
  server run; a NeoForge smoke testmod exposes and transfers item/fluid/energy.**
- **Phase 4 — Cross-platform verification & migration note.** Parity gametests
  where feasible on both loaders; publish both artifacts; Galacticraft migration
  note.
- **Phase 5 (later) — Integrations.** REI/JEI/EMI/WTHIT/Cloth/ModMenu per loader.

## 9. Testing strategy

- **Unit tests** (`src/test`, 13 files) and **gametests** (`src/testmod`, 18
  files) stay hosted in the **`fabric`** sourceset initially — the reference
  behavior. Because the core lives in `common`, these tests exercise the neutral
  engine directly and act as the regression gate through Phases 1–2.
- Every phase has an explicit **run + test gate** before moving on (matches the
  chosen phased sequencing).
- Phase 3 adds a minimal NeoForge smoke testmod; a parity gametest suite on
  NeoForge is a Phase 4 stretch, not a blocker for first NeoForge build.

## 10. Concrete versions / dependencies

- Minecraft `1.21.1`, Java 21.
- **NeoForge** `21.1.x` (pin latest stable 1.21.1 build; new `neoforge.version`).
- **Architectury API** `13.0.6` (already referenced) — `architectury-fabric`,
  `architectury-neoforge`, `architectury` (common).
- **architectury-loom** (loom fork) + **architectury-plugin** (Gradle).
- **Energy:** team-reborn `energy 4.1.0` on Fabric only; native caps on NeoForge.
- badpackets is superseded by Architectury `NetworkManager` — dropped unless a
  specific need surfaces.

## 11. Risks & mitigations

- **Client rendering divergence (highest risk).** FRAPI meshes / model loading
  differ most between loaders. Mitigation: isolate all client model/fluid code
  behind `@ExpectPlatform` in Phase 1; NeoForge client rendering is a focused
  Phase 3 workstream with its own gate.
- **Fluid unit precision.** Sub-mB remainders on NeoForge. Mitigation: adapter
  holds remainders internally; never rounds the neutral store (§5.3).
- **Energy int saturation.** Documented, adapter-local (§5.4).
- **Downstream breakage.** Coordinated via the Galacticraft migration note (§7).
- **Architectury networking parity.** Validate C2S/S2C ordering + login-time
  behavior early in Phase 2 on Fabric before NeoForge depends on it.

## 12. Out of scope

- Minecraft version upgrade beyond 1.21.1.
- Recipe-viewer / tooltip / config-screen integrations (deferred to Phase 5).
- Redesign of MachineLib features unrelated to platform abstraction.
