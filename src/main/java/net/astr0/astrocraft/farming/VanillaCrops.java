package net.astr0.astrocraft.farming;


import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * Hardcoded CropEntry definitions for vanilla Minecraft crops.
 * Useful for testing without a datapack loaded.
 *
 * Usage — call once during mod init, e.g. in your main mod class:
 *
 *   VanillaCrops.bootstrap();
 */
public final class VanillaCrops {

    private VanillaCrops() {}

    // ── Entries ───────────────────────────────────────────────────────────

    public static final List<CropEntry> ENTRIES = List.of(

            entry("minecraft:wheat_seeds",    "GRAIN_TYPE", "Common", "windy"),
            entry("minecraft:beetroot_seeds", "DIRT_TYPE",  "Common", "frost"),
            entry("minecraft:melon_seeds",    "BERRY_TYPE", "Common", "tropical", "soggy"),
            entry("minecraft:pumpkin_seeds",  "BERRY_TYPE", "Common", "windy"),
            entry("minecraft:carrot",         "DIRT_TYPE",  "Common", "windy", "frost"),
            entry("minecraft:potato",         "DIRT_TYPE",  "Common", "windy", "frost"),
            entry("minecraft:cocoa_beans",    "BERRY_TYPE", "Common", "tropical", "shaded"),
            entry("minecraft:sweet_berries",  "BERRY_TYPE", "Common", "frost", "shaded"),
            entry("minecraft:glow_berries",   "BERRY_TYPE", "Rare",   "shaded"),
            entry("minecraft:sugar_cane",     "GRASS_TYPE", "Common", "soggy", "tropical"),
            entry("minecraft:bamboo",         "GRASS_TYPE", "Common", "tropical", "soggy"),
            entry("minecraft:nether_wart",    "DIRT_TYPE",  "Rare",   "arid")
    );

    // ── Bootstrap ─────────────────────────────────────────────────────────

    /**
     * Injects these entries directly into the live CropRegistry.
     * Only adds entries that are not already present (so datapack data
     * always takes precedence if a reload has already occurred).
     */
    public static void bootstrap() {
        // CropRegistry.registry is unmodifiable after apply(), so we use
        // the package-private merge helper — see note below.
        CropRegistry.INSTANCE.mergeDefaults(ENTRIES);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private static CropEntry entry(String itemId, String type, String rarity, String... climates) {
        return new CropEntry(
                new ResourceLocation(itemId),
                type,
                rarity,
                Set.of(climates)
        );
    }
}
