package net.astr0.astrocraft.farming;

import net.astr0.astrocraft.Astrocraft;
import net.astr0.astrocraft.recipe.CrossbreedingRecipe;
import net.astr0.astrocraft.recipe.ModRecipes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeneticsCache {

    // Specific Recipes Cache
    private static final Map<ItemPair, List<CrossbreedingRecipe>> SPECIFIC_CACHE = new ConcurrentHashMap<>();

    // Group Recipes Cache
    private static final Map<GroupPair, List<CrossbreedingRecipe>> GROUP_MATRIX_CACHE = new ConcurrentHashMap<>();

    public static void rebuild(RecipeManager recipeManager) {
        SPECIFIC_CACHE.clear();
        GROUP_MATRIX_CACHE.clear();

        List<CrossbreedingRecipe> allRecipes = recipeManager.getAllRecipesFor(ModRecipes.CROSSBREADING_RECIPE_TYPE.get());

        for (CrossbreedingRecipe recipe : allRecipes) {
            if (recipe.isSpecific()) {
                // THE GOTCHA FIX: Unpack the ingredients into exact Item combinations
                ItemStack[] itemsA = recipe.getInputA().getItems();
                ItemStack[] itemsB = recipe.getInputB().getItems();

                for (ItemStack stackA : itemsA) {
                    for (ItemStack stackB : itemsB) {
                        ItemPair pair = new ItemPair(stackA.getItem(), stackB.getItem());
                        SPECIFIC_CACHE.computeIfAbsent(pair, k -> new ArrayList<>()).add(recipe);
                    }
                }
            } else {
                // Group matrix is much simpler
                GroupPair pair = new GroupPair(recipe.getGroupA(), recipe.getGroupB());
                GROUP_MATRIX_CACHE.computeIfAbsent(pair, k -> new ArrayList<>()).add(recipe);
            }
        }
        //TODO: pretty sure the priority system is unneccessary since we only ever pick the top priority from either group
        // Instead we should probably just map group + group = group or item + item = item without caching the full recipe lists
        // since thats the only thing we will ever actually be looking at. Just throw an error if a duplicate recipe is detected
        // Sort all the lists by priority descending
        Comparator<CrossbreedingRecipe> prioritySorter = Comparator.comparingInt(CrossbreedingRecipe::getPriority).reversed();
        SPECIFIC_CACHE.values().forEach(list -> list.sort(prioritySorter));
        GROUP_MATRIX_CACHE.values().forEach(list -> list.sort(prioritySorter));
    }

    // --- QUERY METHODS ---
    public static List<CrossbreedingRecipe> getSpecificRecipes(ItemStack itemStackA, ItemStack itemStackB) {
        return getSpecificRecipes(itemStackA.getItem(), itemStackB.getItem());
    }

    public static List<CrossbreedingRecipe> getSpecificRecipes(Item itemA, Item itemB) {
        ItemPair key = new ItemPair(itemA, itemB);
        Astrocraft.LOGGER.info("Getting specific crossbreeding recipe for " + key.toString());
        return SPECIFIC_CACHE.getOrDefault(new ItemPair(itemA, itemB), Collections.emptyList());
    }

    public static List<CrossbreedingRecipe> getGroupRecipes(String groupA, String groupB) {
        return GROUP_MATRIX_CACHE.getOrDefault(new GroupPair(groupA, groupB), Collections.emptyList());
    }
}
