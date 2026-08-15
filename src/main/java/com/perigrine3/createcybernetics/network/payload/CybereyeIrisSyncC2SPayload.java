package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public record CybereyeIrisSyncC2SPayload(
        long[] primaryMask,
        long[] secondaryMask,
        int monovisionX, int monovisionY,
        int multioptics1X, int multioptics1Y,
        int multioptics2X, int multioptics2Y,
        int multioptics3X, int multioptics3Y,
        int multioptics4X, int multioptics4Y
) implements CustomPacketPayload {

    public static final Type<CybereyeIrisSyncC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cybereye_iris_sync_c2s"));

    public static final StreamCodec<FriendlyByteBuf, CybereyeIrisSyncC2SPayload> STREAM_CODEC =
            StreamCodec.of(CybereyeIrisSyncC2SPayload::encode, CybereyeIrisSyncC2SPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static CybereyeIrisSyncC2SPayload from(CybereyeVisualConfig.Layout layout) {
        return new CybereyeIrisSyncC2SPayload(
                layout.primary.copyBits(),
                layout.secondary.copyBits(),
                layout.getOffset(CybereyeVisualConfig.OpticsType.MONOVISION).x,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MONOVISION).y,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_1).x,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_1).y,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_2).x,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_2).y,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_3).x,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_3).y,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_4).x,
                layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_4).y
        );
    }

    private static void encode(FriendlyByteBuf buf, CybereyeIrisSyncC2SPayload payload) {
        writeMask(buf, payload.primaryMask);
        writeMask(buf, payload.secondaryMask);

        buf.writeInt(payload.monovisionX);
        buf.writeInt(payload.monovisionY);
        buf.writeInt(payload.multioptics1X);
        buf.writeInt(payload.multioptics1Y);
        buf.writeInt(payload.multioptics2X);
        buf.writeInt(payload.multioptics2Y);
        buf.writeInt(payload.multioptics3X);
        buf.writeInt(payload.multioptics3Y);
        buf.writeInt(payload.multioptics4X);
        buf.writeInt(payload.multioptics4Y);
    }

    private static CybereyeIrisSyncC2SPayload decode(FriendlyByteBuf buf) {
        return new CybereyeIrisSyncC2SPayload(
                readMask(buf),
                readMask(buf),
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt()
        );
    }

    public static void handle(CybereyeIrisSyncC2SPayload payload, ServerPlayer sender) {
        if (sender == null) return;

        CybereyeVisualConfig.Layout layout = new CybereyeVisualConfig.Layout();

        copyMask(payload.primaryMask, layout.primary);
        copyMask(payload.secondaryMask, layout.secondary);

        layout.optics.put(CybereyeVisualConfig.OpticsType.MONOVISION,
                new CybereyeVisualConfig.Offset(payload.monovisionX, payload.monovisionY));
        layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_1,
                new CybereyeVisualConfig.Offset(payload.multioptics1X, payload.multioptics1Y));
        layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_2,
                new CybereyeVisualConfig.Offset(payload.multioptics2X, payload.multioptics2Y));
        layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_3,
                new CybereyeVisualConfig.Offset(payload.multioptics3X, payload.multioptics3Y));
        layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_4,
                new CybereyeVisualConfig.Offset(payload.multioptics4X, payload.multioptics4Y));

        CompoundTag root = CybereyeVisualConfig.writeToNbt(layout);
        sender.getPersistentData().put(CybereyeVisualConfig.NBT_ROOT, root);

        CybereyeIrisSyncS2CPayload outgoing = CybereyeIrisSyncS2CPayload.from(sender.getUUID(), layout);

        PacketDistributor.sendToPlayer(sender, outgoing);
        PacketDistributor.sendToPlayersTrackingEntity(sender, outgoing);
    }

    private static void writeMask(FriendlyByteBuf buf, long[] mask) {
        for (int i = 0; i < CybereyeVisualConfig.MASK_LONG_COUNT; i++) {
            buf.writeLong(mask != null && i < mask.length ? mask[i] : 0L);
        }
    }

    private static long[] readMask(FriendlyByteBuf buf) {
        long[] mask = new long[CybereyeVisualConfig.MASK_LONG_COUNT];

        for (int i = 0; i < mask.length; i++) {
            mask[i] = buf.readLong();
        }

        return mask;
    }

    private static void copyMask(long[] source, CybereyeVisualConfig.PixelMask target) {
        CybereyeVisualConfig.PixelMask sourceMask = new CybereyeVisualConfig.PixelMask(source);

        for (int v = 0; v < CybereyeVisualConfig.HEAD_UV_HEIGHT; v++) {
            for (int u = 0; u < CybereyeVisualConfig.HEAD_UV_WIDTH; u++) {
                target.set(u, v, sourceMask.get(u, v));
            }
        }
    }
}