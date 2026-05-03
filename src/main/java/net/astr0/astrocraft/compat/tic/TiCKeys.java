package net.astr0.astrocraft.compat.tic;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import static net.astr0.astrocraft.Astrocraft.MODID;

public class TiCKeys {

    public static final ModifierId FORGED_MODIFIER = ModifierId.tryBuild(MODID, "forged");
    public static final ResourceLocation QUALITY_KEY = ResourceLocation.fromNamespaceAndPath(MODID, "forge_quality");
    public static final String TAG_UNFORGED = MODID + ":unforged";
}
