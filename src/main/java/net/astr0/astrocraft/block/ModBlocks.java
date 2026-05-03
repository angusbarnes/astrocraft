package net.astr0.astrocraft.block;

import net.astr0.astrocraft.Astrocraft;
import net.astr0.astrocraft.block.AdvancedAssembler.AdvancedAssemblerBlock;
import net.astr0.astrocraft.block.Assembler.AssemblerBlock;
import net.astr0.astrocraft.block.ChemicalMixer.ChemicalMixerBlock;
import net.astr0.astrocraft.block.CoolantBlock.CoolantBlock;
import net.astr0.astrocraft.block.EUVMachine.EUVMachineBlock;
import net.astr0.astrocraft.block.ElectrolyticSeperator.ElectrolyticSeperatorBlock;
import net.astr0.astrocraft.block.EnergyInputHatch.EnergyInputHatchBlock;
import net.astr0.astrocraft.block.FluidInputHatch.FluidInputHatchBlock;
import net.astr0.astrocraft.block.FluidOutputHatch.FluidOutputHatchBlock;
import net.astr0.astrocraft.block.GemPolisher.GemPolishingStationBlock;
import net.astr0.astrocraft.block.PyrolysisChamber.PyrolysisChamberBlock;
import net.astr0.astrocraft.block.ReactionChamber.ChemicalReactorBlock;
import net.astr0.astrocraft.item.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final BooleanProperty BLOCKSTATE_ACTIVE = BooleanProperty.create("active");
    // This is the global instance of the block registry
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Astrocraft.MODID);
    // Call this function from the entry point to allow the blocks register to link itself to the eventBus
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerControllerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerControllerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerControllerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItemWithInfo(block.get(), new Item.Properties()));
    }

    public static RegistryObject<LiquidBlock> registerFluidBlock(String fluidName, RegistryObject<FlowingFluid> source) {
        return BLOCKS.register(String.format("%s_fluid_block", fluidName),
                () -> new LiquidBlock(source, BlockBehaviour.Properties.copy(Blocks.WATER)));
    }

    public static final RegistryObject<Block> REGULATED_MACHINE_CASING = registerBlock("regulated_machine_casing", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Block> VACUUM_FREEZER_CONTROLLER = registerControllerBlock("vacuum_freezer_controller", () -> new VacuumFreezerControllerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> NIC_BLOCK = registerBlock("nic_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> LOG_PILE = registerBlock("log_pile",
            () -> new LogPile(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()));

    public static final RegistryObject<Block> BRICK_KILN_PILE = registerBlock("brick_kiln_pile",
            () -> new BrickKilnPileBlock(BlockBehaviour.Properties.copy(Blocks.BRICKS).noOcclusion()));

    public static final RegistryObject<Block> BRICK_KILN = registerBlock("brick_kiln",
            () -> new BrickKilnBlock(BlockBehaviour.Properties.copy(Blocks.BRICKS).noOcclusion()));

    public static final RegistryObject<Block> FORGE_ANVIL = registerBlock("forge_anvil",
            () -> new ForgeAnvilBlock(BlockBehaviour.Properties.copy(Blocks.ANVIL).noOcclusion()));

    public static final RegistryObject<Block> CROP_STICKS = registerBlock("crop_sticks",
            () -> new CropSticksBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE).noOcclusion()));

    public static final RegistryObject<Block> KNAPPING_STONE = registerBlock("knapping_stone",
            () -> new LogPile(BlockBehaviour.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> CLEANROOM_BLOCK = registerBlock("cleanroom_wall",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).strength(1, 100)));

    public static final RegistryObject<Block> TROPHY = registerBlock("trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST).noOcclusion()));


    public static final RegistryObject<Block> GEM_POLISHING_STATION = registerBlock("gem_polishing_station",
            () -> new GemPolishingStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> CHEMICAL_MIXER = registerBlock("chemical_mixer",
            () -> new ChemicalMixerBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> ASSEMBLER = registerBlock("assembler",
            () -> new AssemblerBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> ADVANCED_ASSEMBLER = registerBlock("advanced_assembler",
            () -> new AdvancedAssemblerBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> CHEMICAL_REACTOR = registerBlock("chemical_reactor",
            () -> new ChemicalReactorBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> ELECTROLYTIC_SEPERATOR = registerBlock("electrolytic_seperator",
            () -> new ElectrolyticSeperatorBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> PYROLYSIS_CHAMBER = registerBlock("pyrolysis_chamber",
            () -> new PyrolysisChamberBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> EUV_MACHINE = registerBlock("euv_machine",
            () -> new EUVMachineBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> COOLANT_BLOCK = registerBlock("coolant_block",
            () -> new CoolantBlock(ModBlocks.MACHINE_DEFAULT_PROPS));

    public static final RegistryObject<Block> FLUID_INPUT_HATCH_BLOCK = registerBlock("fluid_input_hatch",
            () -> new FluidInputHatchBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> FLUID_OUTPUT_HATCH_BLOCK = registerBlock("fluid_output_hatch",
            () -> new FluidOutputHatchBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> ENERGY_INPUT_HATCH_BLOCK = registerBlock("energy_input_hatch",
            () -> new EnergyInputHatchBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));


    public static final BlockBehaviour.Properties MACHINE_DEFAULT_PROPS = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .noOcclusion()
            .sound(SoundType.METAL)
            .strength(2, 12)
            .emissiveRendering((a, b, c) -> true)
            .lightLevel((state) -> state.getValue(BLOCKSTATE_ACTIVE) ? 9 : 0);

    //#anchor BLOCK_REGION
public static final RegistryObject<Block> ADAMANTIUM_BLOCK = registerBlock("adamantium_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> ADAMANTIUM_ORE = registerBlock("adamantium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_ADAMANTIUM_ORE = registerBlock("deepslate_adamantium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> AEROGEL_BLOCK = registerBlock("aerogel_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> ANTIMONY_BLOCK = registerBlock("antimony_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> ANTIMONY_ORE = registerBlock("antimony_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_ANTIMONY_ORE = registerBlock("deepslate_antimony_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> BIOLUMINESCENT_COON_JUICE_BLOCK = registerBlock("bioluminescent_coon_juice_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> BROMINE_BLOCK = registerBlock("bromine_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> BROMINE_ORE = registerBlock("bromine_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_BROMINE_ORE = registerBlock("deepslate_bromine_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> CARBONADIUM_BLOCK = registerBlock("carbonadium_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> CARBONADIUM_ORE = registerBlock("carbonadium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_CARBONADIUM_ORE = registerBlock("deepslate_carbonadium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> CHLORINE_BLOCK = registerBlock("chlorine_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> CHLORINE_ORE = registerBlock("chlorine_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_CHLORINE_ORE = registerBlock("deepslate_chlorine_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> COBALT_BLOCK = registerBlock("cobalt_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> COBALT_ORE = registerBlock("cobalt_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_COBALT_ORE = registerBlock("deepslate_cobalt_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> CYANOGEN_BLOCK = registerBlock("cyanogen_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> DILITHIUM_BLOCK = registerBlock("dilithium_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> DILITHIUM_ORE = registerBlock("dilithium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_DILITHIUM_ORE = registerBlock("deepslate_dilithium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> FENTANYL_BLOCK = registerBlock("fentanyl_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> GELID_CRYOTHEUM_BLOCK = registerBlock("gelid_cryotheum_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> GRAPHENE_BLOCK = registerBlock("graphene_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> HELIUM_BLOCK = registerBlock("helium_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> HELIUM_ORE = registerBlock("helium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_HELIUM_ORE = registerBlock("deepslate_helium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> HIGH_ENTROPY_ALLOY_BLOCK = registerBlock("high_entropy_alloy_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> IUMIUM_BLOCK = registerBlock("iumium_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> IUMIUM_ORE = registerBlock("iumium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_IUMIUM_ORE = registerBlock("deepslate_iumium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> METHYL_ETHYL_KETONE_BLOCK = registerBlock("methyl_ethyl_ketone_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> NEON_BLOCK = registerBlock("neon_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> NEON_ORE = registerBlock("neon_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_NEON_ORE = registerBlock("deepslate_neon_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> PISS_WATER_BLOCK = registerBlock("piss_water_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> RADON_BLOCK = registerBlock("radon_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> RADON_ORE = registerBlock("radon_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_RADON_ORE = registerBlock("deepslate_radon_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> STIPNICIUM_BLOCK = registerBlock("stipnicium_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> STIPNICIUM_ORE = registerBlock("stipnicium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_STIPNICIUM_ORE = registerBlock("deepslate_stipnicium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> TRICHLOROMETHANE_BLOCK = registerBlock("trichloromethane_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> XENON_BLOCK = registerBlock("xenon_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> XENON_ORE = registerBlock("xenon_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_XENON_ORE = registerBlock("deepslate_xenon_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> XYLENE_BLOCK = registerBlock("xylene_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> PHOSPHORUS_BLOCK = registerBlock("phosphorus_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> PHOSPHORUS_ORE = registerBlock("phosphorus_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_PHOSPHORUS_ORE = registerBlock("deepslate_phosphorus_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> HIGH_CARBON_STEEL_52100_BLOCK = registerBlock("high_carbon_steel_52100_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> ENGINEERED_ALLOY_BLOCK = registerBlock("engineered_alloy_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> NITROGEN_BLOCK = registerBlock("nitrogen_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> NITROGEN_ORE = registerBlock("nitrogen_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_NITROGEN_ORE = registerBlock("deepslate_nitrogen_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> SODIUM_BLOCK = registerBlock("sodium_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
public static final RegistryObject<Block> SODIUM_ORE = registerBlock("sodium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
public static final RegistryObject<Block> DEEPSLATE_SODIUM_ORE = registerBlock("deepslate_sodium_ore", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));
    //#end BLOCK_REGION
}
