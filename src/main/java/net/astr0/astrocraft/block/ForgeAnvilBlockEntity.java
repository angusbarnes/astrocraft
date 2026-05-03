package net.astr0.astrocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ForgeAnvilBlockEntity extends BlockEntity {

    // ── Tuning ────────────────────────────────────────────────────────
    public static final int   MAX_STRIKES       = 3;
    public static final int   MAX_CYCLES        = 6;
    /** One full left→right→left oscillation in ticks. */
    public static final int   TICKS_PER_CYCLE   = 30;
    public static final int   MAX_SESSION_TICKS = TICKS_PER_CYCLE * MAX_CYCLES;

    /** Distance from bar centre (range 0–0.5) for each quality tier. */
    public static final float PERFECT_RADIUS = 0.05f;   // centre 10 % of bar
    public static final float FINE_RADIUS    = 0.15f;   // centre 30 %
    public static final float GOOD_RADIUS    = 0.30f;   // centre 60 %
    // Anything outside GOOD_RADIUS is CRUDE.

    // ── State ─────────────────────────────────────────────────────────
    private ItemStack placedItem      = ItemStack.EMPTY;
    private int       strikeCount     = 0;
    private float[]   strikeQualities = {-1f, -1f, -1f}; // -1 = unused slot
    private boolean   sessionActive   = false;
    private boolean   sessionFailed   = false;
    private int       sessionTicks    = 0;

    public ForgeAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGE_ANVIL.get(), pos, state);
    }

    // ── Queries ───────────────────────────────────────────────────────
    public boolean   hasItem()            { return !placedItem.isEmpty(); }
    public ItemStack getPlacedItem()      { return placedItem; }
    public boolean   isSessionActive()    { return sessionActive; }
    public boolean   isSessionFailed()    { return sessionFailed; }
    public int       getStrikeCount()     { return strikeCount; }
    public float[]   getStrikeQualities() { return strikeQualities; }
    public int       getSessionTicks()    { return sessionTicks; }

    // ── Actions ───────────────────────────────────────────────────────

    /** Place a forgeable item on the anvil and start a session. */
    public void placeItem(ItemStack stack) {

        CompoundTag tag = stack.getOrCreateTag();
        placedItem      = stack.copyWithCount(1);
        strikeCount     = 0;
        strikeQualities = new float[]{-1f, -1f, -1f};
        sessionActive   = !(tag.contains("failedForge") || tag.contains("forgingQuality") || tag.contains("forgeStrikes"));
        sessionFailed   = false;
        sessionTicks    = 0;
        setChanged();
        sync();
    }

    /** Player picks up the item (cancels an in-progress session). */
    public ItemStack takeItem() {
        ItemStack result = placedItem.copy();
        reset();
        return result;
    }

    /**
     * Process a hammer strike.
     *
     * @param barPosition 0.0 = left edge, 1.0 = right edge
     * @return quality tier of this strike, or NONE if session is not active
     */
    public StrikeResult strike(float barPosition) {
        if (!sessionActive || sessionFailed || strikeCount >= MAX_STRIKES)
            return StrikeResult.NONE;

        float dist = Math.abs(barPosition - 0.5f);
        StrikeResult tier;
        float        quality;

        if      (dist < PERFECT_RADIUS) { tier = StrikeResult.PERFECT; quality = 1.00f; }
        else if (dist < FINE_RADIUS)    { tier = StrikeResult.FINE;    quality = 0.75f; }
        else if (dist < GOOD_RADIUS)    { tier = StrikeResult.GOOD;    quality = 0.50f; }
        else                            { tier = StrikeResult.CRUDE;   quality = 0.00f; }

        strikeQualities[strikeCount] = quality;
        strikeCount++;

        if (tier == StrikeResult.CRUDE || strikeCount >= MAX_STRIKES)
            finalizeSession(tier == StrikeResult.CRUDE);

        setChanged();
        sync();
        return tier;
    }

    /** Server-side ticker — enforces the session time-out. */
    public static void tick(Level level, BlockPos pos,
                                  BlockState state, ForgeAnvilBlockEntity be) {
        if (level.isClientSide()) {
            be.sessionTicks++;
            return;
        }

        if (!be.sessionActive) return;
        be.sessionTicks++;
        if (be.sessionTicks >= MAX_SESSION_TICKS) {
            be.finalizeSession(false); // time ran out — score whatever strikes were made
            be.setChanged();
            be.sync();
        }
    }

    // ── Private ───────────────────────────────────────────────────────

    private void finalizeSession(boolean failed) {
        sessionActive = false;
        sessionFailed = failed;
        CompoundTag nbt = placedItem.getOrCreateTag();

        if (failed || strikeCount == 0) {
            // Item is ruined — remove it
            nbt.putBoolean("failedForge", true);
        } else {
            float total = 0f;
            for (float q : strikeQualities) if (q >= 0f) total += q;
            // Penalise unused strike slots by dividing by MAX_STRIKES, not strikeCount
            float finalQuality = total / MAX_STRIKES;


            nbt.putFloat("forgingQuality", finalQuality);
            nbt.putInt  ("forgeStrikes",   strikeCount);
        }
    }

    private void reset() {
        placedItem      = ItemStack.EMPTY;
        strikeCount     = 0;
        strikeQualities = new float[]{-1f, -1f, -1f};
        sessionActive   = false;
        sessionFailed   = false;
        sessionTicks    = 0;
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ── NBT ───────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!placedItem.isEmpty()) tag.put("PlacedItem", placedItem.serializeNBT());
        tag.putInt    ("StrikeCount",   strikeCount);
        tag.putBoolean("SessionActive", sessionActive);
        tag.putBoolean("SessionFailed", sessionFailed);
        tag.putInt    ("SessionTicks",  sessionTicks);
        for (int i = 0; i < 3; i++) tag.putFloat("StrikeQ" + i, strikeQualities[i]);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        placedItem    = tag.contains("PlacedItem")
                ? ItemStack.of(tag.getCompound("PlacedItem")) : ItemStack.EMPTY;
        strikeCount   = tag.getInt    ("StrikeCount");
        sessionActive = tag.getBoolean("SessionActive");
        sessionFailed = tag.getBoolean("SessionFailed");
        sessionTicks  = tag.getInt    ("SessionTicks");
        for (int i = 0; i < 3; i++)
            strikeQualities[i] = tag.contains("StrikeQ" + i) ? tag.getFloat("StrikeQ" + i) : -1f;
    }

    // ── Vanilla sync protocol ─────────────────────────────────────────

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }

    // ── Strike tier ───────────────────────────────────────────────────
    public enum StrikeResult { PERFECT, FINE, GOOD, CRUDE, NONE }
}
