//package net.astr0.astrocraft.compat.tic;
//
//import net.astr0.astrocraft.Astrocraft;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.world.item.ItemStack;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import slimeknights.tconstruct.library.events.TinkerToolEvent;
//import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
//
//@Mod.EventBusSubscriber(modid = Astrocraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//public class TiCAssemblyEvents {
//
//    @SubscribeEvent
//    public static void onToolCrafting(TinkerToolEvent event) {
//        float totalQuality = 0f;
//        int partCount = 0;
//
//        // Loop through the input item stacks (the tool parts)
//        for (int i = 0; i < event.getItemStacks().getSlots(); i++) {
//            ItemStack stack = event.getItemStacks().getStackInSlot(i);
//            if (stack.isEmpty()) continue;
//
//            CompoundTag tag = stack.getTag();
//            if (tag != null) {
//                // RULE 1: Block Unforged Parts
//                if (tag.getBoolean(HistoryStagesTiC.TAG_UNFORGED)) {
//                    event.setCanceled(true);
//                    event.setMessage(Component.translatable("error.historystages.part_unforged"));
//                    return; // Abort crafting entirely
//                }
//
//                // RULE 2: Read Quality
//                if (tag.contains(HistoryStagesTiC.QUALITY_KEY.toString())) {
//                    totalQuality += tag.getFloat(HistoryStagesTiC.QUALITY_KEY.toString());
//                    partCount++;
//                }
//            }
//        }
//
//        // RULE 3: Transfer Average Quality to the new Tool
//        if (partCount > 0) {
//            float averageQuality = totalQuality / partCount;
//            ModDataNBT persistentData = event.getTool().getPersistentData();
//
//            // Save the data to the tool
//            persistentData.putFloat(HistoryStagesTiC.QUALITY_KEY, averageQuality);
//
//            // Ensure the tool actually HAS our modifier so the stats apply
//            // (Assumes you have a way to inject the modifier, or you just rely on the hook)
//            event.getTool().getUpgrades().addEntry(HistoryStagesTiC.FORGED_MODIFIER_ID, 1);
//        }
//    }
//}