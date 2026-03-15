Important elements to note that differ from Breeze and TSX version. In this Java version, we work with a Fabric mod specific to one version.
- We only handle version 1.21.11 (the current version of the mods), no management of other versions. Manage it indirectly by making several versions of the mods.
- We use DataGen for enchantments and tags, pack.mcmeta and everthing, which ensures there are no bugs; if that's not the case, it must be done. Use Mojang's APIs as much as possible for JSON generation. So we don't have to manage the JSON syntax of keys.
- Unlike the web TSX version, which only managed one pack at a time, here we manage built-ins (minecraft, fabric) as well as all registries, including enchantments from mods, etc.
- Here we work with layering, unlike Breeze which was limited to a single pack. So here we can edit built-ins like Minecraft, and we can disable them because we can. (Even if it's more complicated)
- We used the registers to obtain the list of active elements.