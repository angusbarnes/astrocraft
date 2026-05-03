package net.astr0.astrocraft.network;

import net.astr0.astrocraft.Astrocraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class AsTechNetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Astrocraft.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void onCommonSetup() {
        INSTANCE.registerMessage(0, FlexiPacket.class, FlexiPacket::encode, FlexiPacket::new, FlexiPacket::handle);
        INSTANCE.registerMessage(1, ServerToClientStateSyncPacket.class, ServerToClientStateSyncPacket::encode, ServerToClientStateSyncPacket::new, ServerToClientStateSyncPacket::handle);
        INSTANCE.registerMessage(2, ClientToServerStateUpdatePacket.class, ClientToServerStateUpdatePacket::encode, ClientToServerStateUpdatePacket::new, ClientToServerStateUpdatePacket::handle);
        INSTANCE.registerMessage(3, UIFluidActionPacket.class, UIFluidActionPacket::encode, UIFluidActionPacket::new, UIFluidActionPacket::handle);
        INSTANCE.registerMessage(
                4,
                ServerboundSetToolModePacket.class,
                ServerboundSetToolModePacket::encode,
                ServerboundSetToolModePacket::decode,
                ServerboundSetToolModePacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        INSTANCE.registerMessage(5, CropSeedUpdatePacket.class,CropSeedUpdatePacket::encode, CropSeedUpdatePacket::decode, CropSeedUpdatePacket::handle);
        INSTANCE.registerMessage(6, C2SStrikePacket.class, C2SStrikePacket::encode, C2SStrikePacket::decode, C2SStrikePacket::handle);
    }

    public static <T> void SendLocalBlockEntityUpdate(BlockEntity tile, T packet) {
        INSTANCE.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> tile.getLevel().getChunkAt(tile.getBlockPos())),
                packet
        );
    }

}
