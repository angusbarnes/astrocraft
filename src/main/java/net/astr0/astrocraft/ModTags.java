package net.astr0.astrocraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class ModTags {

    public static ResourceLocation RL(String path) {
        return new ResourceLocation("astrocraft", path);
    }
    public static ResourceLocation FORGE_RL(String path) {
        return ResourceLocation.fromNamespaceAndPath("forge", path);
    }

    public static final TagKey<Block> BLOCK_CABLE = BlockTags.create(RL("cable_block"));
    public static TagKey<Block> MACHINE_CASING = BlockTags.create(RL("casing"));
    public static TagKey<Block> LOCKED_BLOCK = BlockTags.create(RL("locked_block"));
    public static TagKey<Block> LOCKED_BLOCK_OVERWORLD = BlockTags.create(RL("locked_block/overworld"));
    public static TagKey<Block> LOCKED_BLOCK_TREE = BlockTags.create(RL("locked_block/tree"));
    public static TagKey<Block> LOCKED_BLOCK_NETHER = BlockTags.create(RL("locked_block/nether"));
    public static TagKey<Block> LOCKED_BLOCK_END = BlockTags.create(RL("locked_block/end"));
    public static TagKey<Block> LOCKED_BLOCK_TRANSCENDENT= BlockTags.create(RL("locked_block/transcendent"));

    public static TagKey<Item>  ENDERIO_TELEPORT_ITEM = ItemTags.create(new ResourceLocation("forge", "enderio_staff"));

    public static final TagKey<Item> CHEMICAL_PROTECTION = ItemTags.create(RL("chemical_protection"));
    public static final TagKey<Item> FORGE_SEED = ItemTags.create(RL("chemical_protection"));


    public static final TagKey<Fluid> TIER_1_COOLANT = FluidTags.create(new ResourceLocation("forge", "tier_1_coolant"));
    public static final TagKey<Fluid> TIER_2_COOLANT = FluidTags.create(new ResourceLocation("forge", "tier_2_coolant"));

    public static final TagKey<Fluid> PHOTORESIST_TAG = FluidTags.create(RL("photoresist"));
    public static final TagKey<Fluid> TIER_1_PHOTORESIST = FluidTags.create(RL("photoresist/tier_1"));
    public static final TagKey<Fluid> TIER_2_PHOTORESIST = FluidTags.create(RL("photoresist/tier_2"));

    public static final TagKey<Item> FORGEABLE_PART = ItemTags.create(FORGE_RL("forgeable_part"));
    public static final TagKey<Item> HAMMERS = ItemTags.create(FORGE_RL("tools/hammers"));
}
