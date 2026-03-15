package net.astr0.astrocraft.network;

import net.astr0.astrocraft.block.CropSticksBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CropSeedUpdatePacket {
    /** Registry name of this packet channel. */
    public static final ResourceLocation ID =
            new ResourceLocation("cropsystem", "seedupdate");

    private final ItemStack seedStack;
    private final BlockPos pos;

    public CropSeedUpdatePacket(BlockPos pos, ItemStack stack) {
        this.seedStack = stack;
        this.pos = pos;
    }

    // -------------------------------------------------------------------------
    // Codec
    // -------------------------------------------------------------------------

    public static CropSeedUpdatePacket decode(FriendlyByteBuf buf) {
        return new CropSeedUpdatePacket(buf.readBlockPos(), buf.readItem());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(seedStack);
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemStack getStack() {
        return seedStack;
    }

    // -------------------------------------------------------------------------
    // Handler (runs on server thread)
    // -------------------------------------------------------------------------

    public static void handle(CropSeedUpdatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;

            if (player != null) {
                var level = player.getCommandSenderWorld();
                var tile = level.getBlockEntity(msg.getPos());

                // Not sure if this is the most suitable method for determining chunk coord
                if(level.hasChunk(player.chunkPosition().x, player.chunkPosition().z)) {

                    // NOTE: reduce nesting
                    if(tile instanceof CropSticksBlockEntity cropBE) {
                        cropBE.setSeed(msg.getStack());
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
