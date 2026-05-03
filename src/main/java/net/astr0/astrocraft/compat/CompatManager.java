package net.astr0.astrocraft.compat;

import net.minecraftforge.fml.ModList;

public class CompatManager {
    public static boolean isCreateLoaded;
    public static boolean isMekanismLoaded;
    public static boolean isGregTechLoaded;
    public static boolean isEnderIOLoaded;
    public static boolean isTiCLoaded;

    public static void init() {
        isCreateLoaded = ModList.get().isLoaded("create");
        isMekanismLoaded = ModList.get().isLoaded("mekanism");
        isGregTechLoaded = ModList.get().isLoaded("gtceu");
        isEnderIOLoaded = ModList.get().isLoaded("enderio");
        isTiCLoaded = ModList.get().isLoaded("tconstruct");
    }
}
