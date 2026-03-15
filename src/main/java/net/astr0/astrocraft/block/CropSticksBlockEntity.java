package net.astr0.astrocraft.block;

import com.mojang.logging.LogUtils;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.astr0.astrocraft.Astrocraft;
import net.astr0.astrocraft.farming.*;
import net.astr0.astrocraft.recipe.CrossbreedingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CropSticksBlockEntity extends BlockEntity {

    // 1. Single Source of Truth. This Stack MUST contain the NBT tags.
    private ItemStack seedStack = ItemStack.EMPTY;

    // Cache the helper to avoid resolving IPlantable every tick
    private @Nullable PlantedCrop cachedPlant;

    // Tracker for how long the sticks have been empty
    private int emptyTicks = 0;
    private static final int WEED_THRESHOLD = 5; // Random ticks before weeds grow

    public CropSticksBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CROP_STICKS.get(), pos, state);
    }

    public void setSeed(ItemStack stack) {
        this.seedStack = stack.copy();
        this.seedStack.setCount(1);
        this.cachedPlant = CropUtils.getPlantedCrop(this.seedStack);

        if (!level.isClientSide()) {
            this.setChanged();
            boolean isSeeded = !this.seedStack.isEmpty();
            // Always produces a real state change on seed transitions,
            // guaranteed to be different from the prior state
            level.setBlock(worldPosition,
                    getBlockState()
                            .setValue(CropSticksBlock.AGE, 0)
                            .setValue(CropSticksBlock.SEEDED, isSeeded),
                    3); // flag 3 = update + notify clients
        }
    }

    public ItemStack getSeed() {
        return seedStack.isEmpty() ? ItemStack.EMPTY : seedStack.copy();
    }

    public @Nullable CropGenome getGenes() {
        if (cachedPlant == null) return null;
        return cachedPlant.genetics();
    }

    // --- Core Logic: The "Hybrid" Tick ---
    public void performGrowthTick(ServerLevel level, RandomSource random) {
        // If the sticks are empty, attempt to crossbreed or grow weeds
        if (cachedPlant == null || seedStack.isEmpty()) {
            handleCrossbreeding(level, random);
            return;
        }

        CropGenome stats = cachedPlant.genetics();

        // 1. Calculate Growth Chance based on Stats
        float chance = 1f; //(float) (0.10f + (stats.getGrowth() * 0.05f));

        // 2. Check Light/Water logic here
        if (level.getRawBrightness(this.worldPosition.above(), 0) >= 9) {
            if (random.nextFloat() < chance) {
                int currentAge = this.getBlockState().getValue(CropSticksBlock.AGE);
                if (currentAge < 7) {
                    level.setBlock(this.worldPosition, this.getBlockState().setValue(CropSticksBlock.AGE, currentAge + 1), 2);
                }
            }
        }
    }

    // --- Crossbreeding & Weeds Logic ---
    private void handleCrossbreeding(ServerLevel level, RandomSource random) {
        // Clockwise directions (no diagonals)
        BlockPos[] neighborsPos = {
                this.worldPosition.north(),
                this.worldPosition.east(),
                this.worldPosition.south(),
                this.worldPosition.west()
        };

        List<CropSticksBlockEntity> validNeighbors = new ArrayList<>();

        for (BlockPos pos : neighborsPos) {
            if (level.getBlockEntity(pos) instanceof CropSticksBlockEntity be) {
                // Check if they have a seed and valid genetics
                // You might also want to check if AGE == 7 here so only mature crops breed!
                if (!be.getSeed().isEmpty() && be.getGenes() != null) {
                    validNeighbors.add(be);
                }
            }
        }
        boolean recipeFound = false;

        // We need at least 2 adjacent crops to crossbreed
        if (validNeighbors.size() >= 2) {
            // Randomly pick two distinct neighbors
            int index1 = random.nextInt(validNeighbors.size());
            int index2 = random.nextInt(validNeighbors.size() - 1);
            if (index2 >= index1) index2++; // Shift index to guarantee they are distinct

            CropSticksBlockEntity parent1 = validNeighbors.get(index1);
            CropSticksBlockEntity parent2 = validNeighbors.get(index2);

            ItemStack seed1 = parent1.getSeed();
            ItemStack seed2 = parent2.getSeed();
            Random rand = new Random(random.nextLong());

            if (seed1.is(seed2.getItem())) {
                // 1. Cross the parents
                CropGenome childGenome = parent1.getGenes().cross(parent2.getGenes(), rand);

                // 2. Perform standard mutation step
                childGenome = childGenome.mutate(rand);

                // 3. Rare double mutation (5% chance)
                if (rand.nextDouble() < 0.05) {
                    childGenome = childGenome.mutate(rand);
                }

                // Create the new stack and apply the genetics
                ItemStack newSeed = seed1.copy();
                childGenome.applyToStack(newSeed);
                int age = getOldestAge(seed1, seed2);
                newSeed.getOrCreateTag().putInt(FarmingNBT.CROP_AGE_NBT, age + 1);

                // Plant the new hybrid!
                setSeed(newSeed);
                recipeFound = true;
                this.emptyTicks = 0; // Reset weed timer

                return;
            }

            if (isValidRecipe(seed1, seed2)) {
                PlantedCrop resultCrop = getCraftedCrop(seed1, seed2);

                if (resultCrop != null) {

                    // 1. Cross the parents
                    CropGenome childGenome = parent1.getGenes().cross(parent2.getGenes(), rand);

                    // 2. Perform standard mutation step
                    childGenome = childGenome.mutate(rand);

                    // 3. Rare double mutation (5% chance)
                    if (rand.nextDouble() < 0.05) {
                        childGenome = childGenome.mutate(rand);
                    }

                    // Create the new stack and apply the genetics
                    ItemStack newSeed = resultCrop.seed().copy();
                    childGenome.applyToStack(newSeed);
                    int age = getOldestAge(seed1, seed2);
                    newSeed.getOrCreateTag().putInt(FarmingNBT.CROP_AGE_NBT, age + 1);

                    // Plant the new hybrid!
                    setSeed(newSeed);
                    recipeFound = true;
                    this.emptyTicks = 0; // Reset weed timer
                }
            }
        }

        // If no breeding happened, increment weed counter
        if (!recipeFound) {
            this.emptyTicks++;
            if (this.emptyTicks >= WEED_THRESHOLD) {
                // Grow weeds! We use short grass as the base item
                setSeed(new ItemStack(Items.GRASS));
                this.emptyTicks = 0;
            }
        }
    }

    private int getOldestAge(ItemStack seed1, ItemStack seed2) {
        int age1 = 0;
        int age2 = 0;

        CompoundTag tag1 = seed1.getOrCreateTag();
        CompoundTag tag2 = seed2.getOrCreateTag();
        if (tag1.contains(FarmingNBT.CROP_AGE_NBT)) {
            age1 = tag1.getInt(FarmingNBT.CROP_AGE_NBT);
        }

        if (tag2.contains(FarmingNBT.CROP_AGE_NBT)) {
            age2 = tag2.getInt(FarmingNBT.CROP_AGE_NBT);
        }

        return Math.max(age1, age2);
    }

    public void clearWeeds() {
        if (this.seedStack.is(Items.GRASS)) {
            setSeed(ItemStack.EMPTY);
            emptyTicks = 0;
        }
    }

    public boolean readyForHarvest() {
        return !seedStack.isEmpty() && getBlockState().getValue(BlockStateProperties.AGE_7) == 7;
    }

    public boolean hasWeeds() {
        return this.seedStack.is(Items.GRASS);
    }

    // --- Compatibility: Simulation ---
    public BlockState getSimulatedPlantState() {
        if (cachedPlant == null) return Blocks.AIR.defaultBlockState();

        BlockState internalState = cachedPlant.Plant().getPlant(this.level, this.worldPosition);

        if (internalState.hasProperty(BlockStateProperties.AGE_7)) {
            return internalState.setValue(BlockStateProperties.AGE_7, this.getBlockState().getValue(CropSticksBlock.AGE));
        } else if (internalState.hasProperty(BlockStateProperties.AGE_3)) {
            int age = (this.getBlockState().getValue(CropSticksBlock.AGE) * 3) / 7;
            return internalState.setValue(BlockStateProperties.AGE_3, age);
        }
        return internalState;
    }

    public List<ItemStack> simulateDrops(LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        if (cachedPlant == null) return drops;

        BlockState simulatedState = getSimulatedPlantState();
        builder.withParameter(LootContextParams.BLOCK_STATE, simulatedState);
        List<ItemStack> rawDrops = simulatedState.getDrops(builder);

        if (rawDrops.isEmpty()) {
            LogUtils.getLogger().warn("CROP STICKS: Loot table for {} returned NOTHING! Age: {}",
                    simulatedState.getBlock(),
                    this.getBlockState().getValue(CropSticksBlock.AGE));
        }
        for (ItemStack drop : rawDrops) {
            if (ItemStack.isSameItem(drop, seedStack)) {
                cachedPlant.genetics().applyToStack(drop);
                drop.getOrCreateTag().putInt(
                        FarmingNBT.CROP_AGE_NBT,
                        seedStack.getOrCreateTag().getInt(FarmingNBT.CROP_AGE_NBT)
                );
            }
            drops.add(drop);
        }
        return drops;
    }

    private CrossbreedingRecipe _cachedRecipe = null;
    private boolean isValidRecipe(ItemStack seed1, ItemStack seed2) {
        List<CrossbreedingRecipe> specificRecipe = GeneticsCache.getSpecificRecipes(seed1, seed2);

        for (CrossbreedingRecipe recipe : specificRecipe) {
            _cachedRecipe = recipe;
            return true;
        }

        List<CrossbreedingRecipe> groupRecipe = GeneticsCache.getGroupRecipes(
                GeneticsCache.getGroupForItem(seed1.getItem()),
                GeneticsCache.getGroupForItem(seed2.getItem())
        );

        for (CrossbreedingRecipe recipe : groupRecipe) {
            _cachedRecipe = recipe;
            return true;
        }

        _cachedRecipe = null;
        return false;
    }

    private PlantedCrop getCraftedCrop(ItemStack seed1, ItemStack seed2) {
        if (_cachedRecipe.isSpecific()) {
            return CropUtils.getPlantedCrop(_cachedRecipe.getResultItem());
        } else {
            Item seed = GeneticsCache.getRandomSeedFromGroup(_cachedRecipe.getResultGroup(), RandomSource.create());
            return CropUtils.getPlantedCrop(new ItemStack(seed));
        }
    }

    // --- NBT Save/Load ---
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Seed")) {
            this.seedStack = ItemStack.of(tag.getCompound("Seed"));
        } else {
            this.seedStack = ItemStack.EMPTY;
        }
        this.cachedPlant = CropUtils.getPlantedCrop(this.seedStack);
        this.emptyTicks = tag.getInt("EmptyTicks");

        Astrocraft.LOGGER.info("[CLIENT] load() called at {} - seed resolved to: {}, cachedPlant: {}",
                worldPosition,
                seedStack,
                cachedPlant != null ? cachedPlant.toString() : "NULL");

        if (level != null && level.isClientSide()) {
            VisualizationHelper.queueUpdate(this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if(!seedStack.isEmpty()) {
            tag.put("Seed", seedStack.save(new CompoundTag()));
        }
        // Save the weed tracker
        tag.putInt("EmptyTicks", this.emptyTicks);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        Astrocraft.LOGGER.info("[SERVER] getUpdateTag called at {} - seed: {}, seeded: {}",
                worldPosition,
                tag.contains("Seed") ? tag.getCompound("Seed") : "EMPTY",
                getBlockState().getValue(CropSticksBlock.SEEDED));
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        Astrocraft.LOGGER.info("[SERVER] getUpdatePacket called at {} - seed: {}",
                worldPosition, seedStack);
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}