package net.astr0.astrocraft.compat.tic;

import net.astr0.astrocraft.Astrocraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;

@Mod.EventBusSubscriber(modid = Astrocraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrudeQualityModifier extends NoLevelsModifier implements ToolStatsModifierHook, TooltipModifierHook {

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.TOOLTIP);
    }


    @Override
    public void addToolStats(IToolContext tool, ModifierEntry modifierEntry, ModifierStatsBuilder builder) {
        // If the tool has no determined quality, then it is crude by default
        // Even poorly made tools will have some quality amount
        // Technically the crude modifier should be removed when this is no longer true
        // but for safety we only apply the penality if we explicitly test this case
        if (!tool.getPersistentData().contains(TiCKeys.QUALITY_KEY)) {
            float multiplier = 0.5f;

            // Punish poorly constructed tools across all stats
            ToolStats.DURABILITY.multiply(builder, multiplier);
            ToolStats.USE_ITEM_SPEED.multiply(builder, multiplier);
            ToolStats.ATTACK_DAMAGE.multiply(builder, multiplier);
            ToolStats.ATTACK_SPEED.multiply(builder, multiplier);
            ToolStats.MINING_SPEED.multiply(builder, multiplier);
            ToolStats.ARMOR.multiply(builder, multiplier);
            ToolStats.ARMOR_TOUGHNESS.multiply(builder, multiplier);
            ToolStats.KNOCKBACK_RESISTANCE.multiply(builder, multiplier);
            ToolStats.BLOCK_AMOUNT.multiply(builder, multiplier);
            ToolStats.BLOCK_ANGLE.multiply(builder, multiplier);
            ToolStats.DRAW_SPEED.multiply(builder, multiplier);
            ToolStats.VELOCITY.multiply(builder, multiplier);
            ToolStats.ACCURACY.multiply(builder, multiplier);
            ToolStats.PROJECTILE_DAMAGE.multiply(builder, multiplier);
            ToolStats.WATER_INERTIA.multiply(builder, multiplier);
            ToolStats.SEA_LUCK.multiply(builder, multiplier);
            ToolStats.LURE.multiply(builder, multiplier);
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifierEntry, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        tooltip.add(Component.literal("This item has been poorly made")
                .withStyle(ChatFormatting.DARK_RED));
    }
}