package net.astr0.astrocraft;

import com.mojang.logging.LogUtils;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import net.astr0.astrocraft.Fluid.ModFluids;
import net.astr0.astrocraft.block.AdvancedAssembler.AdvancedAssemblerScreen;
import net.astr0.astrocraft.block.Assembler.AssemblerScreen;
import net.astr0.astrocraft.block.BrickKilnRenderer;
import net.astr0.astrocraft.block.ChemicalMixer.ChemicalMixerScreen;
import net.astr0.astrocraft.block.CoolantBlock.CoolantBlockScreen;
import net.astr0.astrocraft.block.EUVMachine.EUVMachineScreen;
import net.astr0.astrocraft.block.ElectrolyticSeperator.ElectrolyticSeperatorScreen;
import net.astr0.astrocraft.block.GemPolisher.GemPolishingStationScreen;
import net.astr0.astrocraft.block.ModBlockEntities;
import net.astr0.astrocraft.block.ModBlocks;
import net.astr0.astrocraft.block.PyrolysisChamber.PyrolysisChamberScreen;
import net.astr0.astrocraft.block.ReactionChamber.ChemicalReactorScreen;
import net.astr0.astrocraft.client.gui.ModMenuTypes;
import net.astr0.astrocraft.client.input.RadialMenuKeyHandler;
import net.astr0.astrocraft.compat.CompatManager;
import net.astr0.astrocraft.compat.mek.AstrocraftSlurries;
import net.astr0.astrocraft.compat.tic.ModifierRecipeProvider;
import net.astr0.astrocraft.compat.tic.TiCRegistration;
import net.astr0.astrocraft.farming.GeneticsEventHandler;
import net.astr0.astrocraft.item.*;
import net.astr0.astrocraft.network.AsTechNetworkHandler;
import net.astr0.astrocraft.recipe.ModRecipes;
import net.astr0.astrocraft.trading.TradeConfig;
import net.astr0.astrocraft.trading.TradeEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Astrocraft.MODID)
public class Astrocraft
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "astrocraft";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public Astrocraft()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TradeConfig.SPEC);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        CompatManager.init();

        // Register the commonSetup method for mod loading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::endSetup);
        modEventBus.addListener(this::registerKeys);
        modEventBus.addListener(this::gatherData);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);


        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModFluids.register(modEventBus);
        ModCreativeModTab.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        SoundRegistry.register(modEventBus);

        if(CompatManager.isMekanismLoaded) {
            AstrocraftSlurries.SLURRIES.register(modEventBus);
        }

        if(CompatManager.isTiCLoaded) {
            TiCRegistration.register(modEventBus);
            LOGGER.info("[ASTROCRAFT] TiC integration loaded");
        }

        MinecraftForge.EVENT_BUS.register(this);
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.addListener(EventPriority.LOWEST, TradeEventHandler::onVillagerTrades);
        forgeEventBus.addListener(EventPriority.LOWEST, TradeEventHandler::onWanderingTrades);


        forgeEventBus.addListener(Astrocraft::addDangerToolTips);
        forgeEventBus.addListener(EventPriority.LOW, (PlayerInteractEvent.RightClickBlock event) -> {
            InteractionResult result = EventHandlers.HandleBrickPlacement(event);
            if (result != null) {
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        });
        forgeEventBus.addListener(EventPriority.LOW, EventHandlers::PreventTreePunching);
        forgeEventBus.addListener(EventPriority.LOW, EventHandlers::DoCampfireConversion);
        forgeEventBus.addListener(EventPriority.LOW, EventHandlers::BlockPlaceListener);
        forgeEventBus.addListener(EventPriority.HIGH, EventHandlers::restrictBlockEntityAccess);
        forgeEventBus.addListener(EventPriority.LOWEST, EventHandlers::handleHungerMechanics);
        forgeEventBus.addListener(EventPriority.HIGH, EventHandlers::onCurioChange);
        forgeEventBus.addListener(EventPriority.HIGH, TabletEventHandler::onTabletModeToggle);
        forgeEventBus.addListener(EventPriority.LOWEST, GeneticsEventHandler::onTagsUpdated);
        forgeEventBus.addListener(EventPriority.LOWEST, GeneticsEventHandler::onServerStarted);
    }

    @SubscribeEvent
    void gatherData(final GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new ModifierRecipeProvider(packOutput));
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        AsTechNetworkHandler.onCommonSetup();
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        RadialMenuKeyHandler.registerKeys(event);
    }

    private void endSetup(final FMLLoadCompleteEvent event) {
        //CompatManager.init();
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.DEEZ_NUTS_ITEM);
        }
    }

    public static void addDangerToolTips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();

        if(item instanceof AsTechBucketItem || item instanceof FluidCellItem) {
            if(stack.getTag() != null && stack.getTag().contains("danger_ttl")) {
                MutableComponent timer = MutableComponent.create(ComponentContents.EMPTY);
                timer.append(String.format("§c%d...", stack.getTag().getInt("danger_ttl")));
                event.getToolTip().add(timer);
            }
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            MenuScreens.register(ModMenuTypes.GEM_POLISHING_MENU.get(), GemPolishingStationScreen::new);
            MenuScreens.register(ModMenuTypes.CHEMICAL_MIXER_MENU.get(), ChemicalMixerScreen::new);
            MenuScreens.register(ModMenuTypes.ASSEMBLER_MENU.get(), AssemblerScreen::new);
            MenuScreens.register(ModMenuTypes.ADVANCED_ASSEMBLER_MENU.get(), AdvancedAssemblerScreen::new);
            MenuScreens.register(ModMenuTypes.CHEMICAL_REACTOR_MENU.get(), ChemicalReactorScreen::new);
            MenuScreens.register(ModMenuTypes.ELECTROLYTIC_SEPERATOR_MENU.get(), ElectrolyticSeperatorScreen::new);
            MenuScreens.register(ModMenuTypes.PYROLYSIS_CHAMBER_MENU.get(), PyrolysisChamberScreen::new);
            MenuScreens.register(ModMenuTypes.EUV_MACHINE_MENU.get(), EUVMachineScreen::new);
            MenuScreens.register(ModMenuTypes.COOLANT_BLOCK_MENU.get(), CoolantBlockScreen::new);
            MenuScreens.register(ModMenuTypes.CABLE_TOOL_MENU.get(), CableToolScreen::new);

            VisualizerRegistry.setVisualizer(ModBlockEntities.CROP_STICKS.get(), new CropSticksVisualizer());

            MinecraftForge.EVENT_BUS.register(new RadialMenuKeyHandler());
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, EventHandlers::addSeedTooltips);

        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.BRICK_KILN.get(), BrickKilnRenderer::new);
            //event.registerBlockEntityRenderer(ModBlockEntities.CROP_STICKS.get(), CropSticksRenderer::new);
        }



    }
}
