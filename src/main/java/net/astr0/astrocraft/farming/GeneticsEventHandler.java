package net.astr0.astrocraft.farming;

import net.astr0.astrocraft.Astrocraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class GeneticsEventHandler {

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        // We only care about the server syncing, ignore client-side tag updates unless needed for JEI
        Astrocraft.LOGGER.info("TAGS UPDATED EVENT");
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {

            onServerStarted(null);
        }
    }

    public static void onServerStarted(ServerStartedEvent NOT_USED) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Grab the Recipe Manager and trigger our cache rebuild!
            GeneticsCache.rebuild(server.getRecipeManager());

            Astrocraft.LOGGER.info("Astrocraft Genetics Caches Rebuilt successfully!");
        }
    }
}
