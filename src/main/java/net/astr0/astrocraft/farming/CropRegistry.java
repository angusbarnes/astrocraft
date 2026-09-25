package net.astr0.astrocraft.farming;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

/**
 * Loads crop metadata from all JSON files found under:
 *   data/<any_namespace>/crop_registry/*.json
 *
 * Each file is a JSON array of crop objects (see example format below).
 * Reloads automatically on /reload or world load — no restart needed.
 *
 * Failsafe: entries whose mod is not loaded, or whose item is not registered,
 * are silently skipped (with a debug/warn log) rather than crashing.
 *
 * --- Example JSON (data/yourmod/crop_registry/pams_crops.json) -----------
 * [
 *   {
 *     "item":     "pamhc2crops:tomatoseeditem",
 *     "type":     "Crop",
 *     "rarity":   "Common",
 *     "climates": ["arid", "tropical"]
 *   },
 *   {
 *     "item":     "pamhc2crops:chilipepperseeditem",
 *     "type":     "Crop",
 *     "rarity":   "Legendary",
 *     "climates": ["arid"]
 *   }
 * ]
 * -------------------------------------------------------------------------
 */
public class CropRegistry extends SimpleJsonResourceReloadListener {

    public static final CropRegistry INSTANCE = new CropRegistry();

    private static final Gson   GSON   = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Live registry — replaced atomically on each reload. */
    private Map<ResourceLocation, CropEntry> registry = Map.of();

    private CropRegistry() {
        // "crop_registry" → scans data/<ns>/crop_registry/*.json across all packs
        super(GSON, "crop_registry");
    }

    // ── Resource loading ──────────────────────────────────────────────────

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> dataMap,
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, CropEntry> loaded  = new HashMap<>();
        int skippedMod  = 0;
        int skippedItem = 0;
        int errors      = 0;

        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : dataMap.entrySet()) {
            try {
                JsonArray array = fileEntry.getValue().getAsJsonArray();

                for (JsonElement element : array) {
                    JsonObject obj    = element.getAsJsonObject();
                    String     rawId  = obj.get("item").getAsString();
                    ResourceLocation itemId = new ResourceLocation(rawId);

                    // ── Failsafe 1: mod not installed → silent skip ────────
                    if (!ModList.get().isLoaded(itemId.getNamespace())) {
                        LOGGER.debug("CropRegistry: skipping '{}' — mod '{}' not loaded",
                                itemId, itemId.getNamespace());
                        skippedMod++;
                        continue;
                    }

                    // ── Failsafe 2: item not registered (typo, removed) ───
                    if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                        LOGGER.warn("CropRegistry: skipping '{}' — item not registered", itemId);
                        skippedItem++;
                        continue;
                    }

                    // ── Parse climates array ───────────────────────────────
                    Set<String> climates = new HashSet<>();
                    if (obj.has("climates")) {
                        obj.getAsJsonArray("climates")
                                .forEach(c -> climates.add(c.getAsString().toLowerCase()));
                    }

                    loaded.put(itemId, new CropEntry(
                            itemId,
                            obj.has("type")   ? obj.get("type").getAsString()   : "Unknown",
                            obj.has("rarity") ? obj.get("rarity").getAsString() : "Common",
                            Collections.unmodifiableSet(climates)
                    ));
                }

            } catch (Exception e) {
                LOGGER.error("CropRegistry: failed to parse file '{}': {}",
                        fileEntry.getKey(), e.getMessage());
                errors++;
            }
        }

        this.registry = Collections.unmodifiableMap(loaded);
        LOGGER.info("CropRegistry: loaded {} entries | skipped {} (mod absent), {} (bad item) | {} file errors",
                loaded.size(), skippedMod, skippedItem, errors);
    }

    // ── Query API ─────────────────────────────────────────────────────────

    /**
     * Look up an entry by ItemStack. Returns empty if the item is unknown,
     * unregistered, or the stack is empty — never throws.
     */
    public Optional<CropEntry> get(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? Optional.empty() : Optional.ofNullable(registry.get(key));
    }

    /** Direct lookup by ResourceLocation, e.g. for programmatic use. */
    public Optional<CropEntry> get(ResourceLocation itemId) {
        return Optional.ofNullable(registry.get(itemId));
    }

    /** True if this item is tracked in the registry at all. */
    public boolean isKnownCrop(ItemStack stack) {
        return get(stack).isPresent();
    }

    /** True if this item is a crop that belongs to the given climate. */
    public boolean hasClimate(ItemStack stack, String climate) {
        return get(stack).map(e -> e.hasClimate(climate)).orElse(false);
    }

    /** Returns the rarity string, or "Unknown" if the item isn't registered. */
    public String getRarity(ItemStack stack) {
        return get(stack).map(CropEntry::rarity).orElse("Unknown");
    }

    /** All entries that include a given climate. */
    public List<CropEntry> getAllInClimate(String climate) {
        return registry.values().stream()
                .filter(e -> e.hasClimate(climate))
                .toList();
    }

    /** All entries with a given rarity. */
    public List<CropEntry> getAllByRarity(String rarity) {
        return registry.values().stream()
                .filter(e -> e.isRarity(rarity))
                .toList();
    }

    /** Raw access to the full registry (read-only). */
    public Map<ResourceLocation, CropEntry> getAll() {
        return registry;
    }

    // ── Dev / bootstrap ───────────────────────────────────────────────────

    /**
     * Merges a list of default entries into the registry without overwriting
     * any entry that was already loaded from a datapack file.
     * Intended for hardcoded fallbacks (e.g. VanillaCrops) during development.
     */
    void mergeDefaults(java.util.List<CropEntry> defaults) {
        Map<ResourceLocation, CropEntry> merged = new HashMap<>(registry);
        for (CropEntry entry : defaults) {
            merged.putIfAbsent(entry.item(), entry);
        }
        this.registry = Collections.unmodifiableMap(merged);
    }
}
