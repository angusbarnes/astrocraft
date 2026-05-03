package net.astr0.astrocraft.compat.tic;

import net.astr0.astrocraft.Astrocraft;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

import static net.astr0.astrocraft.Astrocraft.LOGGER;

public class TiCRegistration {

    private static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Astrocraft.MODID);

    public static final StaticModifier<ForgedQualityModifier> FORGED =
            MODIFIERS.register("forged", ForgedQualityModifier::new);

    public static void register(IEventBus bus) {
        MODIFIERS.register(bus);
        LOGGER.info("[ASTROCRAFT] TiC integration loaded");
    }
}
