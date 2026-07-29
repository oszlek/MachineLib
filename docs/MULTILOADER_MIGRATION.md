# MachineLib multiloader migration

MachineLib 0.7 targets Minecraft 1.21.1 through separate `common`, `fabric`, and
`neoforge` artifacts. Loader-neutral APIs belong in downstream common source;
native capability adapters belong in the matching platform source set.

## Published artifacts

- `dev.galacticraft:MachineLib-common:<version>`
- `dev.galacticraft:MachineLib-fabric:<version>`
- `dev.galacticraft:MachineLib-neoforge:<version>`

Fabric and NeoForge artifacts include the common runtime classes. An Architectury
consumer should compile common against `MachineLib-common` and each platform
module against the corresponding remapped loader artifact.

## Storage API changes

- `MachineEnergyStorage` is loader-neutral and no longer implements Team Reborn
  Energy's `EnergyStorage`.
- `ResourceStorage#getExposedStorage` and the `TransactionContext` overloads on
  `StorageAccess` were removed. Register machine block-entity types with
  `MachineBlockEntity.registerProviders`; automation should query native loader
  capabilities.
- Fabric exposure types remain in the Fabric artifact. NeoForge exposes the same
  storage through `IItemHandler`, `IFluidHandler`, and `IEnergyStorage`.
- Internal fluid amounts remain droplets (`81,000` per bucket). NeoForge boundary
  APIs use millibuckets while preserving sub-millibucket internal remainders.
- `ItemSource`, `FluidSource`, and `EnergySource` are common and work on both
  loaders.

## Data components and fluids

- Fields in `MLDataComponents` are now `RegistrySupplier<DataComponentType<?>>`.
  Call `.get()` when passing a component type to vanilla APIs.
- `MLDataComponents.FLUID` stores neutral `MLFluidStack` instead of Fabric's
  `FluidVariant`.
- Common code represents a fluid variant as `Fluid` plus `DataComponentPatch`.
  Wrap it in `FluidVariant` only on Fabric and `FluidStack` only on NeoForge.

## Item-backed fluid capabilities

The Fabric artifact retains its Transfer API implementations. The NeoForge
artifact supplies the same MachineLib class names backed by `IFluidHandlerItem`;
register an instance through NeoForge's item capability registration event.

## Menus, packets, models, and integrations

- Extended menus use Architectury `MenuRegistry` and `ExtendedMenuProvider`.
- Payloads use Architectury `NetworkManager`; do not register MachineLib payloads
  a second time.
- Machine model data generation is available from common `MachineModelProvider`.
- Fabric supports ModMenu, Cloth Config, REI, JEI, EMI, and WTHIT. NeoForge
  supports the equivalent Cloth Config screen, REI, JEI, EMI, and Jade.
