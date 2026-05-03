package net.astr0.astrocraft;

import net.astr0.astrocraft.block.ForgeAnvilBlockEntity;
import net.astr0.astrocraft.network.AsTechNetworkHandler;
import net.astr0.astrocraft.network.C2SStrikePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ForgingClientHelper {

    private ForgingClientHelper() {}

    /**
     * Bar position 0.0→1.0 using the exact same formula as the HUD,
     * capturing the value at the instant the player clicks.
     */
    public static float getBarPosition() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0.5f;
        return computeBarPosition(mc.level.getGameTime(), 0f);
    }

    /**
     * Smooth version with partial-tick interpolation for the HUD overlay.
     */
    public static float getBarPositionSmooth(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0.5f;
        return computeBarPosition(mc.level.getGameTime(), partialTick);
    }

    /**
     * -cos gives a clean left → right → left oscillation starting at 0 (left).
     */
    private static float computeBarPosition(long gameTick, float partialTick) {
        float cycleProgress = ((gameTick % ForgeAnvilBlockEntity.TICKS_PER_CYCLE) + partialTick)
                / (float) ForgeAnvilBlockEntity.TICKS_PER_CYCLE;
        return (float)((-Math.cos(cycleProgress * Math.PI * 2.0) + 1.0) / 2.0);
    }

    /** Called by ForgeAnvilBlock.use() on the client side via DistExecutor. */
    public static void sendStrike(BlockPos pos) {
        AsTechNetworkHandler.INSTANCE.sendToServer(new C2SStrikePacket(pos, getBarPosition()));
    }
}
