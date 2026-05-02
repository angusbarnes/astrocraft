package net.astr0.astrocraft.compat.tic;

import net.astr0.astrocraft.Astrocraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

public class ForgedQualityModifier extends Modifier implements ToolStatsModifierHook, TooltipModifierHook {

    public static final ResourceLocation QUALITY_KEY = ResourceLocation.fromNamespaceAndPath(Astrocraft.MODID, "forge_quality");

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.TOOLTIP);
    }


    @Override
    public void addToolStats(IToolContext tool, ModifierEntry modifierEntry, ModifierStatsBuilder builder) {
        // 1. Get the quality score (0.0f to 1.0f) from the tool's NBT
        float quality = tool.getPersistentData().getFloat(QUALITY_KEY);

        // 2. Math: If 1.0 is perfect, let's say it adds +25% stats.
        // If 0.0 is terrible, it applies a -25% penalty.
        // Formula: multiplier = 0.75 + (quality * 0.5)
        float multiplier = 0.75f + (quality * 0.5f);

        // 3. Apply the scaling
        ToolStats.DURABILITY.multiply(builder, multiplier);
        ToolStats.ATTACK_DAMAGE.multiply(builder, multiplier);
        ToolStats.MINING_SPEED.multiply(builder, multiplier);
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifierEntry, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        float quality = tool.getPersistentData().getFloat(QUALITY_KEY);
        int percent = Math.round(quality * 100);

        // Adds a nice visual indicator: "Forged Quality: 85%"
        tooltip.add(Component.translatable("modifier.historystages.forged.quality", percent)
                .withStyle(ChatFormatting.GOLD));
    }
}
