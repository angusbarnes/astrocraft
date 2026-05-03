package net.astr0.astrocraft;

import com.mojang.logging.LogUtils;
import net.astr0.astrocraft.block.ModBlocks;
import net.astr0.astrocraft.farming.CropGenome;
import net.astr0.astrocraft.farming.FarmingNBT;
import net.astr0.astrocraft.item.KeyItem;
import net.astr0.astrocraft.item.ModItems;
import net.astr0.astrocraft.trading.TradeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static net.astr0.astrocraft.block.BlockEntityProperties.BRICK_COUNT;
import static net.astr0.astrocraft.block.BlockEntityProperties.FACING;

public class EventHandlers {

    private static final Map<BlockPos, Integer> dryingLogs = new HashMap<>();
    private static final int CONVERT_TICKS = 20 * 60 * 5; // 5 minutes


    // Call this when a block is placed
    public static void tryStartDrying(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        if (state.is(BlockTags.LOGS)) {
            BlockState above = level.getBlockState(pos.above());
            if (above.getBlock() == Blocks.CAMPFIRE) {
                dryingLogs.put(pos.immutable(), 0);
            }
        }
    }

    public static @Nullable KeyItem getKeyFromPlayer(Player player) {
        AtomicReference<KeyItem> equippedKey = new AtomicReference<>();
        ICuriosItemHandler curiosInventory = CuriosApi.getCuriosInventory(player).resolve().get();
        curiosInventory.getStacksHandler("key").ifPresent(slotInventory -> {
            if(slotInventory.getStacks().getStackInSlot(0).getItem() instanceof KeyItem key) {
                equippedKey.set(key);
            }
        });

        return equippedKey.get();
    }

    public static InteractionResult restrictBlockEntityAccess(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getPhase() != EventPriority.HIGH) return InteractionResult.PASS;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState block = level.getBlockState(pos);

        if (block.is(ModTags.LOCKED_BLOCK)) {

            Player player = event.getEntity();
            KeyItem equippedKey = getKeyFromPlayer(player);

            if (equippedKey != null && equippedKey.Unlocks(block, player.level().dimensionTypeId())) {
                return InteractionResult.SUCCESS;
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return  InteractionResult.CONSUME;
        }

        // if block is be and has tag, then check for specific tag
        //     then check curios slot
        //     if curio present: return a success
        //     if not, cancel the event
        return InteractionResult.PASS;
    }

    public static float modifyBreakSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed) {
        // We only care about preventing tree chopping
        if (!state.is(BlockTags.LOGS)) return speed;

        ItemStack tool = player.getMainHandItem();

        // This might be a bit of a hack but we will generally assume that all tools with durability are fine
        //TODO: Fix this potential bug which will allow wood access to early, perhaps pivot to curios based thing
        if (tool.isDamageableItem()) {
            return speed;
        }

        return 0;
    }

    public static void forgeQualityToolTip(ItemTooltipEvent tooltip) {
        ItemStack stack = tooltip.getItemStack();
        if (!stack.is(ModTags.FORGEABLE_PART)) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("forgingQuality")) {
            float quality = tag.getFloat("forgingQuality");

            if (quality >= 0.3 && quality <= 0.6) {
                tooltip.getToolTip().add(
                        Component.literal("Part Quality: ⭐").withStyle(ChatFormatting.AQUA)
                );
            } else if (0.6 < quality && quality <= 0.85) {
                tooltip.getToolTip().add(Component.literal("Part Quality: ⭐⭐").withStyle(ChatFormatting.GREEN));
            } else if (quality > 0.85) {
                tooltip.getToolTip().add(Component.literal("Part Quality: ⭐⭐⭐").withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.getToolTip().add(Component.literal("Part Quality: Poor").withStyle(ChatFormatting.RED));
            }
        } else {
            tooltip.getToolTip().add(Component.literal("Part Quality: Crude").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static void addSeedTooltips(ItemTooltipEvent tooltip) {
        ItemStack stack = tooltip.getItemStack();
        if (!stack.is(Tags.Items.SEEDS)) return;

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(FarmingNBT.CROP_GENOME_NBT)) {
            CropGenome genome = CropGenome.fromStack(stack);
            tooltip.getToolTip().add(
                    Component.literal(genome.genome())
            );

            if (tag.contains(FarmingNBT.CROP_AGE_NBT)) {
                tooltip.getToolTip().add(
                        Component.literal("Generation: " + tag.getInt(FarmingNBT.CROP_AGE_NBT))
                );
            }
        } else {
            tooltip.getToolTip().add(Component.literal("Wild quality"));
        }
    }

    public static void PreventTreePunching(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(modifyBreakSpeed(event.getEntity(), event.getState(), event.getPosition().orElse(null), event.getNewSpeed()));
    }

    public static void DoCampfireConversion(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || !event.level.dimensionTypeId().location().getPath().equals("overworld"))
            return;

        Level level = event.level;

        Iterator<Map.Entry<BlockPos, Integer>> it = dryingLogs.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            BlockPos pos = entry.getKey();
            int ticks = entry.getValue();


            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockState above = level.getBlockState(pos.above());
            if (above.getBlock() != Blocks.CAMPFIRE) {
                it.remove();
                continue;
            }


            if (ticks >= CONVERT_TICKS) {
                level.setBlockAndUpdate(pos, Blocks.COAL_BLOCK.defaultBlockState());
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
                it.remove();
            } else {
                entry.setValue(ticks + 1);
            }
        }
    }

    public static void BlockPlaceListener(BlockEvent.EntityPlaceEvent event) {
        BlockState state = event.getPlacedBlock();
        tryStartDrying((Level) event.getLevel(), event.getPos(), state);
    }

    public static InteractionResult HandleBrickPlacement(PlayerInteractEvent.RightClickBlock event) {

        if (event.isCanceled() || event.getPhase() != EventPriority.LOW) return InteractionResult.PASS;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState block = level.getBlockState(pos);
        InteractionHand hand = event.getHand();
        ItemStack itemUsed = event.getItemStack();
        BlockHitResult blockHitResult = event.getHitVec();
        Player player = event.getEntity();

        if (block.is(ModBlocks.BRICK_KILN_PILE.get()) && itemUsed.is(Items.BRICK)) {
            int brick_count = block.getValue(BRICK_COUNT);
            Direction facing = block.getValue(FACING);

            if (brick_count < 18) {
                BlockState brick_kiln_pile = block.setValue(BRICK_COUNT, brick_count + 1);
                itemUsed.shrink(1);
                level.setBlock(pos, brick_kiln_pile, Block.UPDATE_ALL); // clear flag

                playSoundForBlock(brick_kiln_pile, level, pos, player);
                return InteractionResult.SUCCESS;
            } else {
                BlockState brick_kiln = ModBlocks.BRICK_KILN.get().defaultBlockState();
                brick_kiln = brick_kiln.setValue(FACING, facing);

                itemUsed.shrink(1);
                level.setBlock(pos, brick_kiln, Block.UPDATE_ALL); // clear flag

                playSoundForBlock(brick_kiln, level, pos, player);
            }

        } else if (event.getFace() == Direction.UP && level.getBlockState(pos.above()).is(Blocks.AIR) && itemUsed.is(Items.BRICK)) {
            BlockState brick_kiln_pile = ModBlocks.BRICK_KILN_PILE.get().getStateForPlacement(new BlockPlaceContext(player, hand, itemUsed, blockHitResult));
            itemUsed.shrink(1);
            level.setBlock(pos.above(), brick_kiln_pile, Block.UPDATE_ALL); // clear flag

            playSoundForBlock(brick_kiln_pile, level, pos, player);
            return InteractionResult.SUCCESS;
        } else if (itemUsed.is(Items.STICK)  && block.is(Blocks.CAMPFIRE)) {
            ItemStack torches = new ItemStack(Items.TORCH);
            torches.setCount(itemUsed.getCount());
            player.setItemInHand(hand, torches);
        }

        return null;
    }

    private static void playSoundForBlock(BlockState state, Level level, BlockPos blockPos, Player player) {
        SoundType soundtype = state.getSoundType(level, blockPos, player);
        level.playSound(player, blockPos,
                state.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                (soundtype.getVolume() + 1.0F) / 2.0F,
                soundtype.getPitch() * 0.8F
        );
    }

    public static void handleSurvivalSystems(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        CompoundTag data = player.getPersistentData();
        FoodData food = player.getFoodData();

        //TODO: Can only have one equipped at a time so can optimise this by only checking the slot
        boolean stabilizer = data.getBoolean("astrocraft_has_stabilizer");
        boolean xpConverter = data.getBoolean("astrocraft_has_xp_converter");
        boolean override = data.getBoolean("astrocraft_has_exhaustion_override");

        if (override) {
            handleOverrideMode(player, food);
            handleColdDamage(player); // still vulnerable to cold
            return;
        }

        float exhaustionToAdd = computeEnvironmentalExhaustion(player);

        if (stabilizer) {
            reduceExhaustion(food, 0.01f);
        } else {
            food.addExhaustion(exhaustionToAdd);
        }

        if (xpConverter) {
            convertXpToSaturation(player, food);
        }

        handleColdDamage(player);
    }

    private static float computeEnvironmentalExhaustion(ServerPlayer player) {
        float base = TradeConfig.EXHAUSTION_RATE.get().floatValue(); // increased base globally

        var level = player.level();

        // Nether multiplier
        if (level.dimension() == Level.NETHER) {
            base *= 3.5f;
        }

        // Altitude scaling
        // TODO: Review
        int y = player.blockPosition().getY();
        if (y > 100) {
            base += ((y - 100) / 10f) * 0.0007f;
        }

        // Temperature scaling (ignored in Nether)
        if (level.dimension() != Level.NETHER) {
            float temp = level.getBiome(player.blockPosition())
                    .value()
                    .getBaseTemperature();

            base += Math.abs(temp - 0.8f) * 0.004f;
        }

        return base;
    }

    private static void reduceExhaustion(FoodData food, float amount) {
        float current = food.getExhaustionLevel();
        if (current > 0f) {
            food.setExhaustion(Math.max(0f, current - amount));
        }
    }

    private static void convertXpToSaturation(ServerPlayer player, FoodData food) {

        if (food.getSaturationLevel() < 2) return;

        int drain = 5; // per tick
        LogUtils.getLogger().info("XP CURIO: Level {}", player.experienceLevel);

        if (player.experienceLevel < 10) return;

        // Proper XP removal
        player.giveExperiencePoints(-drain);
        LogUtils.getLogger().info("]]]]] XP CURIO: {}, {}, {}", player.totalExperience, drain, food.getSaturationLevel());

        food.setSaturation(10f);
    }

    private static void handleOverrideMode(ServerPlayer player, FoodData food) {

        // Disable exhaustion entirely
        food.setExhaustion(0f);

        // Custom regen
        if (player.tickCount % 40 != 0) return; // every 2 seconds

        if (player.getHealth() >= player.getMaxHealth()) return;

        if (food.getSaturationLevel() > 1f) {
            food.setSaturation(food.getSaturationLevel() - 1f);
            player.heal(1f);
        } else if (food.getFoodLevel() > 1) {
            food.setFoodLevel(food.getFoodLevel() - 1);
            player.heal(1f);
        }
    }

    private static void updateCurioFlags(ServerPlayer player) {

        CompoundTag data = player.getPersistentData();

        boolean stabilizer = hasCurio(player, ModItems.OVERWORLD_KEY.get());
        boolean xpConverter = hasCurio(player, ModItems.NETHER_KEY.get());
        boolean override = hasCurio(player, ModItems.END_KEY.get());

        data.putBoolean("astrocraft_has_stabilizer", stabilizer);
        data.putBoolean("astrocraft_has_xp_converter", xpConverter);
        data.putBoolean("astrocraft_has_exhaustion_override", override);
    }

    private static void handleColdDamage(ServerPlayer player) {

        if (player.isCreative() || player.isSpectator()) return;

        FoodData food = player.getFoodData();

        if (food.getFoodLevel() >= 10) return;

        var level = player.level();
        var biome = level.getBiome(player.blockPosition()).value();

        //TODO: Review this cold classification logic
        boolean coldBiome =
                biome.getBaseTemperature() < 0.15f ||
                        level.getBiome(player.blockPosition()).is(BiomeTags.SPAWNS_SNOW_FOXES);

        if (!coldBiome) return;

        if (player.tickCount % 40 == 0) {
            player.hurt(
                    level.damageSources().freeze(),
                    1.0f
            );
        }
    }

    public static boolean hasCurio(Player player, Item curioItem) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(stack ->
                        stack.is(curioItem)
                ).isPresent())
                .orElse(false);
    }

    public static void handleHungerMechanics(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        handleSurvivalSystems(player);
    }

    public static void onCurioChange(CurioChangeEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        updateCurioFlags(player);
    }
}
