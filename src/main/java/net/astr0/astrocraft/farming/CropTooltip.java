package net.astr0.astrocraft.farming;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Generates crop metadata tooltip lines for seed items.
 *
 * Usage — in your Item's addTooltip() or a TooltipEvent handler:
 *
 *   CropTooltip.appendTo(stack, tooltipLines);
 */
public final class CropTooltip {

    private CropTooltip() {}

    // ── Rarity colours ────────────────────────────────────────────────────

    private static final Map<String, ChatFormatting> RARITY_COLOUR = Map.of(
            "Common",    ChatFormatting.WHITE,
            "Rare",      ChatFormatting.AQUA,
            "Legendary", ChatFormatting.GOLD
    );

    // ── Climate colours & icons ───────────────────────────────────────────

    private static final Map<String, ChatFormatting> CLIMATE_COLOUR = Map.of(
            "arid",     ChatFormatting.YELLOW,
            "frost",    ChatFormatting.AQUA,
            "shaded",   ChatFormatting.GREEN,
            "soggy",    ChatFormatting.BLUE,
            "tropical", ChatFormatting.LIGHT_PURPLE,
            "windy",    ChatFormatting.WHITE
    );

    private static final Map<String, String> CLIMATE_ICON = Map.of(
            "arid",     "☀",
            "frost",    "❄",
            "shaded",   "🌲",
            "soggy",    "💧",
            "tropical", "🌴",
            "windy",    "🍃"
    );

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Appends up to two tooltip lines to the provided list if the item is
     * present in the CropRegistry. Does nothing if the item is unknown.
     *
     *   Line 1:  [Type]  ◆ Rarity
     *   Line 2:  Found in: ☀ Arid  ❄ Frost  ...
     */
    public static void appendTo(ItemStack stack, List<Component> tooltip) {
        CropRegistry.INSTANCE.get(stack).ifPresent(entry -> {
            tooltip.add(buildSummaryLine(entry));
            if (!entry.climates().isEmpty()) {
                tooltip.add(buildClimateLine(entry));
            }
        });
    }

    // ── Line builders ─────────────────────────────────────────────────────

    private static Component buildSummaryLine(CropEntry entry) {
        ChatFormatting rarityColour = RARITY_COLOUR.getOrDefault(entry.rarity(), ChatFormatting.GRAY);

        // Normalise type label: "GRAIN_TYPE" → "Grain"
        String typeLabel = formatTypeLabel(entry.type());

        // [Type]
        MutableComponent typeTag = Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(typeLabel).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));

        // ◆ Rarity
        MutableComponent rarityPart = Component.literal("  ◆ ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(entry.rarity()).withStyle(rarityColour));

        return typeTag.append(rarityPart);
    }

    private static Component buildClimateLine(CropEntry entry) {
        MutableComponent line = Component.literal("Found in: ")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(false));

        String[] sorted = entry.climates().stream().sorted().toArray(String[]::new);

        for (int i = 0; i < sorted.length; i++) {
            String climate = sorted[i];
            ChatFormatting colour = CLIMATE_COLOUR.getOrDefault(climate, ChatFormatting.GRAY);
            String icon = CLIMATE_ICON.getOrDefault(climate, "•");
            String label = capitalise(climate);

            MutableComponent token = Component.literal(icon + " " + label).withStyle(colour);
            line.append(token);

            if (i < sorted.length - 1) {
                line.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        return line;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** "GRAIN_TYPE" → "Grain",  "Crop" → "Crop",  "BERRY_TYPE" → "Berry" */
    private static String formatTypeLabel(String raw) {
        String cleaned = raw.replace("_TYPE", "").replace("_", " ");
        if (cleaned.isEmpty()) return "Crop";
        return capitalise(cleaned.toLowerCase());
    }

    private static String capitalise(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
