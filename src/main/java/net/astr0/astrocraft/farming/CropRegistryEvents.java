package net.astr0.astrocraft.farming;

import net.astr0.astrocraft.Astrocraft;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers CropRegistry as a server-side reload listener.
 *
 * This fires on:
 *   - World load (server start / single-player game open)
 *   - /reload command
 *   - Datapack changes
 *
 * The registry is therefore always in sync with the active datapack set.
 */
@Mod.EventBusSubscriber(modid = Astrocraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CropRegistryEvents {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CropRegistry.INSTANCE);
    }
}
