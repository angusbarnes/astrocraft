package net.astr0.astrocraft.compat.tic;

import net.astr0.astrocraft.Astrocraft;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import slimeknights.mantle.recipe.data.IRecipeHelper;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipeBuilder;
import slimeknights.tconstruct.library.tools.SlotType;

import java.util.function.Consumer;

public class ModifierRecipeProvider extends RecipeProvider implements IConditionBuilder, IRecipeHelper {

    public ModifierRecipeProvider(PackOutput generator) {
        super(generator);
    }

    /*@Override
    public String getName() {
        return "Tinkers' Levelling Addon Modifier Recipes";
    }*/

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        String abilityFolder = "tools/modifiers/upgrade/";
        String abilitySalvage = "tools/modifiers/salvage/upgrade/";

        ModifierId improvableId = TiCKeys.FORGED_MODIFIER;
        assert TiCKeys.FORGED_MODIFIER != null;
        ModifierRecipeBuilder.modifier(TiCKeys.FORGED_MODIFIER)
//                .addInput(Items.EXPERIENCE_BOTTLE)
//                .addInput(Items.NETHER_STAR)
//                .addInput(Items.DIAMOND)
//                .addInput(Items.EXPERIENCE_BOTTLE)
//                .addInput(Items.EXPERIENCE_BOTTLE)
                .setSlots(SlotType.UPGRADE, 0)
                .setMaxLevel(1)
                .saveSalvage(consumer, prefix(improvableId, abilitySalvage))
                .save(consumer, prefix(improvableId, abilityFolder));
    }

    @Override
    public String getModId() {
        return Astrocraft.MODID;
    }
}
