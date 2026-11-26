# 1.21.1 build

Same features as the 26.1.2 build, version 0.4.1. Nothing cut. Built against Railcraft Reborn 1.2.10 and
Thaumaturge 1.1.0 for 1.21.1.

Changes: crowbars take a `Tier` (Thaumaturge's thaumium/void tiers), Railcraft's older locomotive/fuel signatures
(`consumeFuel()`, `interact(Player, Hand)`, MinecartFactory with ServerLevel), FluidStack tank fills, CompoundTag
save/load, locomotive paint tints moved to an item color handler.

## Cut

Nothing. Every 26.1.2 0.4.1 block, item, entity, recipe, data map and texture has a 1.21.1 counterpart.
The only 26.1.2 files left out are the `assets/arcanelocomotion/items/*.json` item definitions, which 1.21.1
does not read; their models live under `models/item` instead.
