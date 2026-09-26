package net.astr0.astrocraft.network;

/**
 * Sent from server → client on player login to sync the full CropRegistry.
 *
 * Wire format per entry:
 *   ResourceLocation item  (string)
 *   String           type
 *   String           rarity
 *   int              climateCount
 *   String...        climates
 */
//public class SyncCropRegistryPacket {
//
//    private final List<CropEntry> entries;
//
//    // Constructed server-side from the live registry
//    public SyncCropRegistryPacket(Collection<CropEntry> entries) {
//        this.entries = List.copyOf(entries);
//    }
//
//    // ── Codec ─────────────────────────────────────────────────────────────
//
//    public static SyncCropRegistryPacket decode(FriendlyByteBuf buf) {
//        int count = buf.readVarInt();
//        List<CropEntry> entries = new ArrayList<>(count);
//        for (int i = 0; i < count; i++) {
//            ResourceLocation item   = buf.readResourceLocation();
//            String           type   = buf.readUtf();
//            String           rarity = buf.readUtf();
//
//            int climateCount = buf.readVarInt();
//            Set<String> climates = new HashSet<>(climateCount);
//            for (int j = 0; j < climateCount; j++) {
//                climates.add(buf.readUtf());
//            }
//
//            entries.add(new CropEntry(item, type, rarity, Collections.unmodifiableSet(climates)));
//        }
//        return new SyncCropRegistryPacket(entries);
//    }
//
//    public void encode(FriendlyByteBuf buf) {
//        buf.writeVarInt(entries.size());
//        for (CropEntry entry : entries) {
//            buf.writeResourceLocation(entry.item());
//            buf.writeUtf(entry.type());
//            buf.writeUtf(entry.rarity());
//            buf.writeVarInt(entry.climates().size());
//            for (String climate : entry.climates()) {
//                buf.writeUtf(climate);
//            }
//        }
//    }
//
//    // ── Handler ───────────────────────────────────────────────────────────
//
//    public void handle(Supplier<NetworkEvent.Context> ctx) {
//        ctx.get().enqueueWork(() ->
//                CropRegistry.getInstance().loadFromSync(entries)
//        );
//        ctx.get().setPacketHandled(true);
//    }
//}
