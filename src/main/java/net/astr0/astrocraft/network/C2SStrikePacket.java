package net.astr0.astrocraft.network;

import net.astr0.astrocraft.block.ForgeAnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from the client the instant the player right-clicks with a hammer.
 * Carries the bar position so the server can evaluate strike quality.
 */
public class C2SStrikePacket {

    private final BlockPos pos;
    private final float    barPosition; // 0.0 = left, 1.0 = right

    public C2SStrikePacket(BlockPos pos, float barPosition) {
        this.pos         = pos;
        this.barPosition = barPosition;
    }

    public static void encode(C2SStrikePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeFloat(pkt.barPosition);
    }

    public static C2SStrikePacket decode(FriendlyByteBuf buf) {
        return new C2SStrikePacket(buf.readBlockPos(), buf.readFloat());
    }

    public static void handle(C2SStrikePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();

            // Guard: block entity must exist and player must be close enough
            if (!(level.getBlockEntity(pkt.pos) instanceof ForgeAnvilBlockEntity be)) return;
            double distSq = player.distanceToSqr(
                    pkt.pos.getX() + 0.5, pkt.pos.getY() + 0.5, pkt.pos.getZ() + 0.5);
            if (distSq > 36.0) return; // > 6 blocks away — reject

            ForgeAnvilBlockEntity.StrikeResult result = be.strike(pkt.barPosition);

            double cx = pkt.pos.getX() + 0.5;
            double cy = pkt.pos.getY() + 1.1;
            double cz = pkt.pos.getZ() + 0.5;

            switch (result) {
                case PERFECT -> {
                    level.playSound(null, pkt.pos, SoundEvents.ANVIL_USE,
                            SoundSource.BLOCKS, 1f, 1.6f);
                    level.sendParticles(ParticleTypes.CRIT,          cx, cy, cz, 25, 0.2, 0.1, 0.2, 0.15);
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, cx, cy, cz, 15, 0.15, 0.1, 0.15, 0.08);
                }
                case FINE -> {
                    level.playSound(null, pkt.pos, SoundEvents.ANVIL_USE,
                            SoundSource.BLOCKS, 1f, 1.3f);
                    level.sendParticles(ParticleTypes.CRIT,  cx, cy, cz, 14, 0.2, 0.1, 0.2, 0.10);
                }
                case GOOD -> {
                    level.playSound(null, pkt.pos, SoundEvents.ANVIL_USE,
                            SoundSource.BLOCKS, 0.9f, 1.0f);
                    level.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 10, 0.2, 0.1, 0.2, 0.05);
                }
                case CRUDE -> {
                    level.playSound(null, pkt.pos, SoundEvents.ANVIL_DESTROY,
                            SoundSource.BLOCKS, 1f, 0.8f);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 20, 0.3, 0.2, 0.3, 0.05);
                    level.sendParticles(ParticleTypes.FLAME,       cx, cy, cz, 12, 0.2, 0.1, 0.2, 0.12);
                }
                case NONE -> { /* Session wasn't active — silently ignore */ }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
