package net.astr0.astrocraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.astr0.astrocraft.block.ForgeAnvilBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ForgeAnvilBER implements BlockEntityRenderer<ForgeAnvilBlockEntity> {

    public ForgeAnvilBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ForgeAnvilBlockEntity be, float partialTick,
                       PoseStack ps, MultiBufferSource buf, int light, int overlay) {

        ItemStack item = be.getPlacedItem();
        if (item.isEmpty()) return;

        Level level = be.getLevel();
        long gameTime = level != null ? level.getGameTime() : 0L;

        ps.pushPose();

        // Gentle bob while session is active, static when done
        float bob = be.isSessionActive()
                ? (float) Math.sin((gameTime + partialTick) * 0.12f) * 0.025f
                : 0f;

        // Place item flat on the anvil top face (anvil is 12/16 tall → 0.75 blocks)
        ps.translate(0.5, 1 + bob, 0.5);

        // Rotate the item so it lies flat (XP = tilt to horizontal)
        ps.mulPose(Axis.XP.rotationDegrees(90f));

        // Rotate 45° to avoid a perfectly axis-aligned look
        ps.mulPose(Axis.ZP.rotationDegrees(45f));

        // Slow spin while session is active — satisfying visual
        if (be.isSessionActive()) {
            float spin = (gameTime + partialTick) * 0.5f;
            ps.mulPose(Axis.ZP.rotationDegrees(spin));
        }

        // Scale down to sit nicely on the anvil
        ps.scale(1f, 1f, 1f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                item, ItemDisplayContext.FIXED,
                light, overlay, ps, buf, level, 0
        );

        ps.popPose();
    }

    /** Keep rendering even when the camera is outside the block's AABB. */
    @Override
    public boolean shouldRenderOffScreen(ForgeAnvilBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 64; }
}