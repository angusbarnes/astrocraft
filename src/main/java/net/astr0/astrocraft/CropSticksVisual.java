package net.astr0.astrocraft;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.astr0.astrocraft.block.CropSticksBlock;
import net.astr0.astrocraft.block.CropSticksBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Consumer;


// Implement Visual.
// Optional: Implement TickableVisual if you need per-tick updates (e.g., smoothing).
// Optional: Implement DynamicVisual if the crop sways in the wind.
public class CropSticksVisual extends AbstractBlockEntityVisual<CropSticksBlockEntity> {

    private TransformedInstance transformedInstance;

    // The actual object on the GPU.
    // TransformedInstance is a standard type that supports position/rotation/scaling.
    private TransformedInstance cropInstance;

    // State tracking to know when to rebuild
    private ItemStack lastSeed = ItemStack.EMPTY;
    private int lastAge = -1;

    // This will be reconstructed on every block state change so we alledegly do not have to track changes outselves
    public CropSticksVisual(VisualizationContext context, CropSticksBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        Instancer<TransformedInstance> cropModelInstancer = instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                Models.block(blockEntity.getSimulatedPlantState())
        );
        transformedInstance = cropModelInstancer.createInstance();
        lastSeed = blockEntity.getSeed();
        lastAge = blockEntity.getBlockState().getValue(BlockStateProperties.AGE_7);

        setupVisual(partialTick);
        Astrocraft.LOGGER.info("Creating CropSticksVisual {}", blockEntity.getLevel().isClientSide() ? "client" : "server");
    }

    @Override
    protected void _delete() {
        transformedInstance.delete();
    }

    @Override
    public void update(float partialTick) {

        ItemStack currentSeed = blockEntity.getSeed();
        int currentAge = blockEntity.getBlockState().getValue(CropSticksBlock.AGE);

        boolean seedChanged = !ItemStack.isSameItemSameTags(currentSeed, lastSeed);
        boolean ageChanged = currentAge != lastAge;

        if (seedChanged) {
            // Delete old instance and rebuild with the new model
            transformedInstance.delete();

            transformedInstance = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED,
                            Models.block(blockEntity.getSimulatedPlantState()))
                    .createInstance();

            setupVisual(partialTick);
            relight(transformedInstance);

            lastSeed = currentSeed.copy();
            lastAge = currentAge;
        }

        Astrocraft.LOGGER.info("[VISUAL] update() called - SEEDED: {}, seed: {}, cachedPlant: {}",
                currentAge,
                currentSeed,
                blockEntity.getSimulatedPlantState());
    }

    // We might not need this
    private void setupVisual(float partialTicks) {
        transformedInstance.setIdentityTransform().translate(getVisualPosition()).setChanged();
        Astrocraft.LOGGER.info("Setting up CropSticksVisual {}", blockEntity.getLevel().isClientSide() ? "client" : "server");
    }

    @Override
    public void updateLight(float partialTick) {
        relight(transformedInstance);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(transformedInstance);
    }
}