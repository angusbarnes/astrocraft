package net.astr0.astrocraft.farming;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Immutable data holder for a single crop's metadata as loaded from JSON.
 *
 * Mapped by the item's ResourceLocation (e.g. "pamhc2crops:tomatoseeditem").
 */
public record CropEntry(
        ResourceLocation item,
        String type,       // e.g. "Crop", "Grain", "Herb"
        String rarity,     // "Common", "Rare", or "Legendary"
        Set<String> climates // e.g. {"arid", "tropical"}
) {
    public boolean hasClimate(String climate) {
        return climates.contains(climate);
    }

    public boolean isRarity(String r) {
        return rarity.equalsIgnoreCase(r);
    }
}