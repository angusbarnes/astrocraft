package net.astr0.astrocraft.mixin;

import net.astr0.astrocraft.Astrocraft;
import net.astr0.astrocraft.compat.tic.TiCKeys;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.recipe.RecipeResult;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;

@Mixin(ToolBuildingRecipe.class)
public class MixinToolBuildingRecipe {

    @Inject(
            // We target getValidatedResult. Note the return type in the JVM signature is just RecipeResult due to type erasure.
            method = "getValidatedResult(Lslimeknights/tconstruct/library/recipe/tinkerstation/ITinkerStationContainer;Lnet/minecraft/core/RegistryAccess;)Lslimeknights/tconstruct/library/recipe/RecipeResult;",
            at = @At("RETURN"),
            remap = false // Essential for TiC internal methods
    )
    private void astrocraft$applyForgedQualityToNewTool(ITinkerStationContainer inv, RegistryAccess access, CallbackInfoReturnable<RecipeResult<?>> cir) {
        Astrocraft.LOGGER.warn("Attempting to modify return value of tool build recipe");
        RecipeResult<?> recipeResult = cir.getReturnValue();

        // 1. Only proceed if the recipe successfully generated a tool
        if (recipeResult != null && recipeResult.isSuccess()) {

            // Extract the LazyToolStack and force it to build the actual ItemStack
            Object lazyStackObj = recipeResult.getResult();
            if (!(lazyStackObj instanceof slimeknights.tconstruct.library.tools.nbt.LazyToolStack lazyStack)) return;

            ItemStack finalStack = lazyStack.getStack();
            if (finalStack.isEmpty() || !(finalStack.getItem() instanceof IModifiable)) return;

            // 2. Calculate the average quality from the input slots
            float totalQuality = 0f;
            int partCount = 0;

            for (int i = 0; i < inv.getInputCount(); i++) {
                ItemStack slotStack = inv.getInput(i);
                if (slotStack.isEmpty()) continue;

                if (slotStack.getItem() instanceof IToolPart) {
                    partCount++; // Unforged parts still count, pulling the average down

                    if (slotStack.hasTag() && slotStack.getTag().contains(TiCKeys.QUALITY_KEY.toString())) {
                        totalQuality += slotStack.getTag().getFloat(TiCKeys.QUALITY_KEY.toString());
                    }
                }
            }

            // We don't need to apply the forged modifier if no parts have been forged
            if (totalQuality <= 0) {
                return;
            }

            // 3. Apply the data to the final ItemStack
            if (partCount > 0) {
                float averageQuality = totalQuality / partCount;

                // Wrap the ItemStack so we can use the TiC API safely
                ToolStack tool = ToolStack.from(finalStack);

                // Save our average quality
                tool.getPersistentData().putFloat(TiCKeys.QUALITY_KEY, averageQuality);

                // Append the Forged Quality Modifier without overwriting existing traits
                ModifierNBT.Builder upgrades = ModifierNBT.builder();
                upgrades.add(tool.getUpgrades());
                upgrades.add(TiCKeys.FORGED_MODIFIER, 1);

                tool.setUpgrades(upgrades.build());

                // Rebuild stats so the new modifier is immediately calculated for the GUI display
                tool.rebuildStats();

                // Note: Because ToolStack.from() wraps the existing ItemStack in memory,
                // any NBT changes we just made are automatically saved to `finalStack`.
                // We don't need to replace the return value in `cir`.
            }
        }
    }
}