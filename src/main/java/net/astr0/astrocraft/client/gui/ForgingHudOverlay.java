package net.astr0.astrocraft.client.gui;

import net.astr0.astrocraft.ForgingClientHelper;
import net.astr0.astrocraft.ModTags;
import net.astr0.astrocraft.block.ForgeAnvilBlock;
import net.astr0.astrocraft.block.ForgeAnvilBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class ForgingHudOverlay implements IGuiOverlay {

    public static final ForgingHudOverlay INSTANCE = new ForgingHudOverlay();

    // ── Layout ────────────────────────────────────────────────────────
    /** Total width of the quality-zone bar in pixels. */
    private static final int BAR_W = 200;
    private static final int BAR_H = 14;
    /**
     * Distance the bar sits above the bottom of the screen.
     * Adjust if you have other overlays in this area.
     */
    private static final int BAR_BOTTOM_OFFSET = 90;

    // ── Zone pixel half-widths (from centre) ──────────────────────────
    // These are computed from the server-side thresholds so the
    // visual zones match the actual hit-boxes exactly.
    //   PERFECT_RADIUS=0.05  → 0.05 × 200 = 10 px
    //   FINE_RADIUS   =0.15  → 0.15 × 200 = 30 px
    //   GOOD_RADIUS   =0.30  → 0.30 × 200 = 60 px
    private static final int PERF_HALF = (int)(ForgeAnvilBlockEntity.PERFECT_RADIUS * BAR_W); // 10
    private static final int FINE_HALF = (int)(ForgeAnvilBlockEntity.FINE_RADIUS    * BAR_W); // 30
    private static final int GOOD_HALF = (int)(ForgeAnvilBlockEntity.GOOD_RADIUS    * BAR_W); // 60

    // ── Colours (ARGB) ────────────────────────────────────────────────
    private static final int COL_CRUDE   = 0xFFAA2222;
    private static final int COL_GOOD    = 0xFFCCAA00;
    private static final int COL_FINE    = 0xFF44BB44;
    private static final int COL_PERFECT = 0xFFFFD700;
    private static final int COL_BORDER  = 0xFF111111;
    private static final int COL_BG      = 0xAA000000;
    private static final int COL_TIMER   = 0xFF4488FF;
    private static final int COL_TIMER_LOW = 0xFFFF4444;
    private static final int COL_UNUSED  = 0xFF333333;

    @Override
    public void render(ForgeGui gui, GuiGraphics gfx,
                       float partialTick, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        // Only show when the player is looking at our forging anvil
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;
        if (bhr.getType() == HitResult.Type.MISS) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState bs = mc.level.getBlockState(pos);
        if (!(bs.getBlock() instanceof ForgeAnvilBlock)) return;
        if (!(mc.level.getBlockEntity(pos) instanceof ForgeAnvilBlockEntity be)) return;
        if (!be.hasItem() || !be.isSessionActive() || !mc.player.getMainHandItem().is(ModTags.HAMMERS)) return;

        drawForgingUI(gfx, sw, sh, be, partialTick);
    }

    // ─────────────────────────────────────────────────────────────────

    private void drawForgingUI(GuiGraphics gfx, int sw, int sh,
                               ForgeAnvilBlockEntity be, float partialTick) {
        int barX = sw / 2 - BAR_W / 2;
        int barY = sh - BAR_BOTTOM_OFFSET;
        int cx   = barX + BAR_W / 2;   // pixel centre of bar

        // ── Dark pill background ──────────────────────────────────────
        gfx.fill(barX - 6, barY - 26, barX + BAR_W + 6, barY + BAR_H + 28, COL_BG);

        // ── "FORGING" title ───────────────────────────────────────────
        gfx.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("gui.astrocraft.forging.title"),
                sw / 2, barY - 20, 0xFFFFDD00);

        // ── Quality zones ─────────────────────────────────────────────
        // Left crude
        gfx.fill(barX,                barY, cx - GOOD_HALF, barY + BAR_H, COL_CRUDE);
        // Left good
        gfx.fill(cx - GOOD_HALF,      barY, cx - FINE_HALF, barY + BAR_H, COL_GOOD);
        // Left fine
        gfx.fill(cx - FINE_HALF,      barY, cx - PERF_HALF, barY + BAR_H, COL_FINE);
        // Perfect centre
        gfx.fill(cx - PERF_HALF,      barY, cx + PERF_HALF, barY + BAR_H, COL_PERFECT);
        // Right fine
        gfx.fill(cx + PERF_HALF,      barY, cx + FINE_HALF, barY + BAR_H, COL_FINE);
        // Right good
        gfx.fill(cx + FINE_HALF,      barY, cx + GOOD_HALF, barY + BAR_H, COL_GOOD);
        // Right crude
        gfx.fill(cx + GOOD_HALF,      barY, barX + BAR_W,   barY + BAR_H, COL_CRUDE);

        // ── Zone labels ───────────────────────────────────────────────
        drawZoneLabel(gfx, "CRUDE",   barX + 20,             barY + BAR_H + 2);
        drawZoneLabel(gfx, "GOOD",    cx - GOOD_HALF + 15,   barY + BAR_H + 2);
        drawZoneLabel(gfx, "FINE",    cx - FINE_HALF + 10,   barY + BAR_H + 2);
        drawZoneLabel(gfx, "PERFECT", cx,                    barY + BAR_H + 2);
        drawZoneLabel(gfx, "CRUDE",   barX + BAR_W - 20,     barY + BAR_H + 2);

        // ── Bar border ────────────────────────────────────────────────
        gfx.renderOutline(barX - 1, barY - 1, BAR_W + 2, BAR_H + 2, COL_BORDER);

        // ── Session timer strip (below bar) ───────────────────────────
        float timeLeft = 1f - (be.getSessionTicks() / (float) ForgeAnvilBlockEntity.MAX_SESSION_TICKS);
        int timerFill  = Math.max(0, (int)(BAR_W * timeLeft));
        //gfx.fill(barX,           barY + BAR_H + 3, barX + BAR_W, barY + BAR_H + 6, 0xFF222222);
        gfx.fill(barX,           barY + BAR_H - 90, barX + timerFill, barY + BAR_H -84,
                timeLeft > 0.33f ? COL_TIMER : COL_TIMER_LOW);

        // ── Moving indicator arrow ────────────────────────────────────
        float barPos  = ForgingClientHelper.getBarPositionSmooth(partialTick);
        int   indPixX = barX + Math.round(barPos * BAR_W);
        int   indCol  = indicatorColor(barPos);

        // Downward-pointing triangle above bar (widest at top)
        for (int iy = 0; iy < 7; iy++) {
            int half = 6 - iy;
            gfx.fill(indPixX - half, barY - 9 + iy,
                    indPixX + half + 1, barY - 8 + iy, indCol);
        }
        // Vertical cursor line through bar
        gfx.fill(indPixX - 1, barY, indPixX + 2, barY + BAR_H, 0xCCFFFFFF);

        // ── Strike quality slots ──────────────────────────────────────
        drawStrikeSlots(gfx, be, sw, barY - 20 + BAR_H + 28 + 2);
    }

    private void drawZoneLabel(GuiGraphics gfx, String text, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        // Small scale — draw shadow only
        gfx.drawString(mc.font, text, x - mc.font.width(text) / 2, y, 0x88FFFFFF, false);
    }

    private int indicatorColor(float barPos) {
        float dist = Math.abs(barPos - 0.5f);
        if (dist < ForgeAnvilBlockEntity.PERFECT_RADIUS) return COL_PERFECT;
        if (dist < ForgeAnvilBlockEntity.FINE_RADIUS)    return COL_FINE;
        if (dist < ForgeAnvilBlockEntity.GOOD_RADIUS)    return COL_GOOD;
        return COL_CRUDE;
    }

    private void drawStrikeSlots(GuiGraphics gfx, ForgeAnvilBlockEntity be, int sw, int y) {
        float[] qualities = be.getStrikeQualities();
        int slotSize  = 16;
        int spacing   = 26;
        int totalW    = ForgeAnvilBlockEntity.MAX_STRIKES * spacing - (spacing - slotSize);
        int startX    = sw / 2 - totalW / 2;

        gfx.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("gui.astrocraft.forging.strikes"),
                sw / 2, y, 0xAAAAAA);
        y += 12;

        for (int i = 0; i < ForgeAnvilBlockEntity.MAX_STRIKES; i++) {
            int slotX = startX + i * spacing;
            float q   = qualities[i];

            int bgCol = q < 0f   ? COL_UNUSED
                    : q >= 1f  ? COL_PERFECT
                    : q >= .75f? COL_FINE
                    : q >= .5f ? COL_GOOD
                    :            COL_CRUDE;

            gfx.fill(slotX, y, slotX + slotSize, y + slotSize, bgCol);
            gfx.renderOutline(slotX - 1, y - 1, slotSize + 2, slotSize + 2, COL_BORDER);

            // Icon inside slot
            String icon = q < 0f   ? "-"
                    : q >= 1f  ? "*"    // perfect
                    : q >= .75f? "+"    // fine
                    : q >= .5f ? "o"    // good
                    :            "X";   // crude
            gfx.drawCenteredString(Minecraft.getInstance().font, icon,
                    slotX + slotSize / 2, y + 4, 0xFFFFFF);
        }
    }
}
