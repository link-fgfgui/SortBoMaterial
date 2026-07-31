# Show Ingredients In Recipe Tree

A client-only [Forge] mod that integrates [EMI]'s recipe tree (BoM) with
[Project Expansion]'s Arcane Transmutation Tablet.

When EMI is in **crafting mode** and has an active recipe tree, the tablet's
item list is resorted so that materials the player still needs (insufficient
in inventory) are pinned to the front of the first page. Materials already
satisfied drop back to ProjectE's default EMC-descending order, so once
you've gathered enough of an item it stops being pinned. **Viewing mode**
is left untouched — only crafting mode reorders the list.

## How it works

A single Mixin targets ProjectE's
`moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory#updateClientTargets()V`
and replaces the `Stream.sorted(Comparator)` call via `@ModifyArg`. Because
the full knowledge list (not just the 16 visible slots) is sorted, paging
follows naturally: the first 12 unmet matter items land on page 1, the
next 12 on page 2, and so on. The comparator priority is:

1. Required by BoM **and** unmet (inventory insufficient)
2. Everything else (BoM-met or non-BoM)

with EMC descending as the tiebreaker inside each bucket, preserving
ProjectE's default visual order when no BoM is active.

BoM data is read from EMI's `dev.emi.emi.bom.BoM` tree. The `bom` package
is `public` at runtime but not part of EMI's published `:api` jar, so the
full EMI jar is used as `compileOnly` (the player's installed EMI provides
the classes at runtime).

## Requirements

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.0 or later |
| EMI | 1.1.16+1.20.1+forge or later |
| ProjectE | 1.20.1 release |
| Project Expansion | 1.20.1 release |

EMI, ProjectE, and Project Expansion are **optional** at runtime — if any
is missing the mod simply does nothing. The mod itself is client-only
(`clientSideOnly=true` in `mods.toml`) and will not load on a
dedicated server.

## Building

```shell
./gradlew jar
```

The built jar lands in `build/libs/`.

## Credits

Built on top of [ProjectE] by the ProjectE team, [Project Expansion] by
DonovanDMC, and [EMI] by Emi.
