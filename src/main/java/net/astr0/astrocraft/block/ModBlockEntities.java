package net.astr0.astrocraft.block;

import net.astr0.astrocraft.Astrocraft;
import net.astr0.astrocraft.block.AdvancedAssembler.AdvancedAssemblerBlockEntity;
import net.astr0.astrocraft.block.Assembler.AssemblerBlockEntity;
import net.astr0.astrocraft.block.ChemicalMixer.ChemicalMixerBlockEntity;
import net.astr0.astrocraft.block.CoolantBlock.CoolantBlockEntity;
import net.astr0.astrocraft.block.EUVMachine.EUVMachineBlockEntity;
import net.astr0.astrocraft.block.ElectrolyticSeperator.ElectrolyticSeperatorBlockEntity;
import net.astr0.astrocraft.block.EnergyInputHatch.EnergyInputHatchBlockEntity;
import net.astr0.astrocraft.block.FluidInputHatch.FluidInputHatchBlockEntity;
import net.astr0.astrocraft.block.FluidOutputHatch.FluidOutputHatchBlockEntity;
import net.astr0.astrocraft.block.GemPolisher.GemPolishingStationBlockEntity;
import net.astr0.astrocraft.block.PyrolysisChamber.PyrolysisChamberBlockEntity;
import net.astr0.astrocraft.block.ReactionChamber.ChemicalReactorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Astrocraft.MODID);

    public static final RegistryObject<BlockEntityType<GemPolishingStationBlockEntity>> GEM_POLISHING_BE =
            BLOCK_ENTITIES.register("gem_polishing_be", () ->
                    BlockEntityType.Builder.of(GemPolishingStationBlockEntity::new,
                            ModBlocks.GEM_POLISHING_STATION.get()).build(null));

    public static final RegistryObject<BlockEntityType<BrickKilnBlockEntity>> BRICK_KILN =
            BLOCK_ENTITIES.register("brick_kiln", () -> BlockEntityType.Builder.of(BrickKilnBlockEntity::new, ModBlocks.BRICK_KILN.get()).build(null));

    public static final RegistryObject<BlockEntityType<ForgeAnvilBlockEntity>> FORGE_ANVIL =
            BLOCK_ENTITIES.register("forge_anvil", () -> BlockEntityType.Builder.of(ForgeAnvilBlockEntity::new, ModBlocks.FORGE_ANVIL.get()).build(null));

    public static final RegistryObject<BlockEntityType<CropSticksBlockEntity>> CROP_STICKS =
            BLOCK_ENTITIES.register("crop_sticks", () -> BlockEntityType.Builder.of(CropSticksBlockEntity::new, ModBlocks.CROP_STICKS.get()).build(null));

    public static final RegistryObject<BlockEntityType<ChemicalMixerBlockEntity>> CHEMICAL_MIXER_BE =
            BLOCK_ENTITIES.register("chemical_mixer", () ->
                    BlockEntityType.Builder.of(ChemicalMixerBlockEntity::new,
                            ModBlocks.CHEMICAL_MIXER.get()).build(null));

    public static final RegistryObject<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER_BE =
            BLOCK_ENTITIES.register("assembler", () ->
                    BlockEntityType.Builder.of(AssemblerBlockEntity::new,
                            ModBlocks.ASSEMBLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<AdvancedAssemblerBlockEntity>> ADVANCED_ASSEMBLER_BE =
            BLOCK_ENTITIES.register("advanced_assembler", () ->
                    BlockEntityType.Builder.of(AdvancedAssemblerBlockEntity::new,
                            ModBlocks.ADVANCED_ASSEMBLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ChemicalReactorBlockEntity>> CHEMICAL_REACTOR_BE =
            BLOCK_ENTITIES.register("chemical_reactor", () ->
                    BlockEntityType.Builder.of(ChemicalReactorBlockEntity::new,
                            ModBlocks.CHEMICAL_REACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<ElectrolyticSeperatorBlockEntity>> ELECTROLYTIC_SEPERATOR_BE =
            BLOCK_ENTITIES.register("electrolytic_seperator", () ->
                    BlockEntityType.Builder.of(ElectrolyticSeperatorBlockEntity::new,
                            ModBlocks.ELECTROLYTIC_SEPERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<PyrolysisChamberBlockEntity>> PYROLYSIS_CHAMBER_BE =
            BLOCK_ENTITIES.register("pyrolysis_chamber", () ->
                    BlockEntityType.Builder.of(PyrolysisChamberBlockEntity::new,
                            ModBlocks.PYROLYSIS_CHAMBER.get()).build(null));

    public static final RegistryObject<BlockEntityType<EUVMachineBlockEntity>> EUV_MACHINE_BE =
            BLOCK_ENTITIES.register("euv_machine", () ->
                    BlockEntityType.Builder.of(EUVMachineBlockEntity::new,
                            ModBlocks.EUV_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<VacuumFreezerController>> VACUUM_FREEZER =
            BLOCK_ENTITIES.register("vacuum_freezer_controller", () ->
                    BlockEntityType.Builder.of(VacuumFreezerController::new,
                            ModBlocks.VACUUM_FREEZER_CONTROLLER.get()).build(null));


    public static final RegistryObject<BlockEntityType<CoolantBlockEntity>> COOLANT_BLOCK_BE =
            BLOCK_ENTITIES.register("coolant_block", () ->
                    BlockEntityType.Builder.of(CoolantBlockEntity::new,
                            ModBlocks.COOLANT_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidInputHatchBlockEntity>> FLUID_INPUT_HATCH_BE =
            BLOCK_ENTITIES.register("fluid_input_hatch", () ->
                    BlockEntityType.Builder.of(FluidInputHatchBlockEntity::new,
                            ModBlocks.FLUID_INPUT_HATCH_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidOutputHatchBlockEntity>> FLUID_OUTPUT_HATCH_BE =
            BLOCK_ENTITIES.register("fluid_output_hatch", () ->
                    BlockEntityType.Builder.of(FluidOutputHatchBlockEntity::new,
                            ModBlocks.FLUID_OUTPUT_HATCH_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<EnergyInputHatchBlockEntity>> ENERGY_INPUT_HATCH_BE =
            BLOCK_ENTITIES.register("energy_input_hatch", () ->
                    BlockEntityType.Builder.of(EnergyInputHatchBlockEntity::new,
                            ModBlocks.ENERGY_INPUT_HATCH_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
