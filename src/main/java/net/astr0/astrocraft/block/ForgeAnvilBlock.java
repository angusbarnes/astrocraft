package net.astr0.astrocraft.block;

import net.astr0.astrocraft.ForgingClientHelper;
import net.astr0.astrocraft.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class ForgeAnvilBlock extends BaseEntityBlock {

    public ForgeAnvilBlock(Properties props) { super(props); }

    // BaseEntityBlock defaults to INVISIBLE — we want the block model to render.
    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeAnvilBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // Only tick server-side
        return createTickerHelper(type, ModBlockEntities.FORGE_ANVIL.get(),
                        ForgeAnvilBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        // Only process main hand to avoid double-firing
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof ForgeAnvilBlockEntity be))
            return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();

        // ── (A) Place a forgeable item ────────────────────────────────
        if (!be.hasItem() && held.is(ModTags.FORGEABLE_PART)) {
            if (!level.isClientSide) {
                be.placeItem(held);
                if (!player.isCreative()) held.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP,
                        SoundSource.BLOCKS, 0.8f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ── (B) Strike with a hammer ──────────────────────────────────
        if (be.hasItem() && be.isSessionActive() && held.is(ModTags.HAMMERS)) {
            if (level.isClientSide) {
                // DistExecutor prevents the client-only class from being loaded on the server JVM.
                final BlockPos captured = pos;
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ForgingClientHelper.sendStrike(captured));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ── (C) Retrieve finished / abandoned item (empty hand) ───────
        if (be.hasItem() && !be.isSessionActive() && held.isEmpty()) {
            if (!level.isClientSide) {
                player.addItem(be.takeItem());
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP,
                        SoundSource.BLOCKS, 0.8f, 0.9f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }
}