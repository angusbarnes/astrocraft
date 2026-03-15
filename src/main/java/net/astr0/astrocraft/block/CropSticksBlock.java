package net.astr0.astrocraft.block;

import mekanism.common.util.VoxelShapeUtils;
import net.astr0.astrocraft.farming.CropUtils;
import net.astr0.astrocraft.farming.PlantedCrop;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.PlantType;

import java.util.ArrayList;
import java.util.List;

public class CropSticksBlock extends CropBlock implements EntityBlock, BonemealableBlock {
    public static final BooleanProperty SEEDED = BooleanProperty.create("seeded");
    private final VoxelShape STICK1 = Block.box(1.0D, 0.0D,  1.0D, 2.0D, 16.0D, 2.0D);
    private final VoxelShape STICK2 = Block.box(14.0D,0.0D, 14.0D, 15.0D, 16.0D, 15.0D);
    private final VoxelShape STICK3 = Block.box(1.0D, 0.0D, 14.0D, 2.0D, 16.0D, 15.0D);
    private final VoxelShape STICK4 = Block.box(14.0D,0.0D,  1.0D, 15.0D, 16.0D, 2.0D);
    private final  VoxelShape SHAPE = VoxelShapeUtils.combine(STICK1, STICK2, STICK3, STICK4);
    private final VoxelShape INTERACTION_SHAPE = Block.box(0,0,0,16,16,16);
    //public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public CropSticksBlock(Properties props) {
        super(Properties.copy(Blocks.WHEAT));
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(AGE, 0)
                        .setValue(SEEDED, false)
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CropSticksBlockEntity(pos, state);
    }

    // --- 1. Hybrid Ticking ---
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // Always tick so we can check if the BE needs to grow
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CropSticksBlockEntity be) {
            be.performGrowthTick(level, random);
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof FarmBlock;
    }

    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos.below()).getBlock() instanceof FarmBlock;
    }

    @Override
    public PlantType getPlantType(BlockGetter level, BlockPos pos) {
        return PlantType.CROP;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {

        if (pLevel.getBlockEntity(pPos) instanceof CropSticksBlockEntity be) {
            return be.getSeed();
        }

        return super.getCloneItemStack(pLevel, pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModBlocks.CROP_STICKS.get().asItem()) return InteractionResult.FAIL;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CropSticksBlockEntity cropBE)) return InteractionResult.PASS;

        ItemStack currentSeed = cropBE.getSeed();
        PlantedCrop heldPlant = CropUtils.getPlantedCrop(held);
        LootParams.Builder builder = getStandardLootParams(level, pos, player, player.getItemInHand(hand), cropBE);
        List<ItemStack> drops = cropBE.simulateDrops((builder));

        if (held.getItem() instanceof ShearsItem) {

            // Clear weeds
            if (cropBE.hasWeeds()) {
                cropBE.clearWeeds();
                //level.setBlock(pos, state.setValue(AGE, 0), 2);
                return InteractionResult.SUCCESS;
            }

            //Auto re-plant
            if (cropBE.readyForHarvest()) {
                doStandardDrops(drops, currentSeed, level, pos, true);
                level.setBlock(pos, state.setValue(AGE, 0).setValue(SEEDED, true), 2);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.SUCCESS;
        }

        if(cropBE.hasWeeds()) {
            return InteractionResult.PASS;
        }

        // Swap seed
        if (heldPlant != null && !currentSeed.isEmpty() && !ItemStack.isSameItemSameTags(held, currentSeed)) {

            if (state.getValue(AGE) == 7) {
                doStandardDrops(drops, currentSeed, level, pos);
            } else {
                popResource(level, pos, currentSeed);
            }

            // Plant the new seed
            cropBE.setSeed(held);
            held.shrink(1);

            // Reset age to 0 for the new plant
            //level.setBlock(pos, state.setValue(AGE, 0), 2);
            return InteractionResult.SUCCESS;
        }

        // Plant seed
        if (currentSeed.isEmpty() && heldPlant != null) {
            cropBE.setSeed(held);
            held.shrink(1);
            //level.setBlock(pos, state.setValue(AGE, 0), 2);
            return InteractionResult.SUCCESS;
        }

        // standard harvest
        if (cropBE.readyForHarvest()) {
            doStandardDrops(drops, currentSeed, level, pos);
            state.setValue(AGE, 0);
            cropBE.setSeed(ItemStack.EMPTY);
            //level.setBlock(pos, state.setValue(AGE, 0).setValue(SEEDED, false), 2);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void doStandardDrops(List<ItemStack> drops, ItemStack currentSeed, Level level, BlockPos pos) {
        doStandardDrops(drops, currentSeed, level, pos, false);
    }

    private void doStandardDrops(List<ItemStack> drops, ItemStack currentSeed, Level level, BlockPos pos, boolean consumeSeed) {

        for (ItemStack drop : drops) {
            // Filter logic: In a standard harvest, we reset AGE but keep the seed in the BE.
            // Therefore, we don't drop the "base" seed stack.
            if (consumeSeed && drop.is(currentSeed.getItem())) {
                drop.shrink(1);
                if (drop.getCount() == 0) continue;
            }
            popResource(level, pos, drop);
        }
    }

    private LootParams.Builder getStandardLootParams(Level level, BlockPos pos, Player player, ItemStack toolStack, CropSticksBlockEntity cropBE) {
        return new LootParams.Builder((ServerLevel) level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, toolStack)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, cropBE);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();

        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (be == null) {
            Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
            if (origin != null) {
                BlockPos pos = BlockPos.containing(origin);
                be = builder.getLevel().getBlockEntity(pos);
            }
        }

        if (be instanceof CropSticksBlockEntity cropBE) {
            // Pass the builder directly
            List<ItemStack> simulated = cropBE.simulateDrops(builder);

            if (simulated.isEmpty() && !cropBE.getSeed().isEmpty()) {
                drops.add(cropBE.getSeed());
            } else {
                drops.addAll(simulated);
            }
        }
        return drops;
    }

    // --- Bonemeal Support ---
    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < 7;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CropSticksBlockEntity be) {
            // Force a growth tick
            be.performGrowthTick(level, random);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(SEEDED);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
}