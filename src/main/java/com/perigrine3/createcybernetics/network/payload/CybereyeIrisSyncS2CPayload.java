package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.skin.CybereyeOverlayHandler;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record CybereyeIrisSyncS2CPayload(
        UUID playerId,
        long[] primaryMask,
        long[] secondaryMask,
        int monovisionX, int monovisionY,
        int multioptics1X, int multioptics1Y,
        int multioptics2X, int multioptics2Y,
        int multioptics3X, int multioptics3Y,
        int multioptics4X, int multioptics4Y
) implements CustomPacketPayload {

    public static final Type<CybereyeIrisSyncS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cybereye_iris_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, CybereyeIrisSyncS2CPayload> STREAM_CODEC =
            StreamCodec.of(CybereyeIrisSyncS2CPayload::encode, CybereyeIrisSyncS2CPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static CybereyeIrisSyncS2CPayload from(UUID playerId, CybereyeVisualConfig.Layout layout) {
        if (layout == null) {
            layout = CybereyeVisualConfig.defaults();
        }

        CybereyeVisualConfig.Offset monovision = layout.getOffset(CybereyeVisualConfig.OpticsType.MONOVISION);
        CybereyeVisualConfig.Offset multioptics1 = layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_1);
        CybereyeVisualConfig.Offset multioptics2 = layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_2);
        CybereyeVisualConfig.Offset multioptics3 = layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_3);
        CybereyeVisualConfig.Offset multioptics4 = layout.getOffset(CybereyeVisualConfig.OpticsType.MULTIOPTICS_4);

        return new CybereyeIrisSyncS2CPayload(
                playerId,
                layout.primary.copyBits(),
                layout.secondary.copyBits(),
                monovision.x, monovision.y,
                multioptics1.x, multioptics1.y,
                multioptics2.x, multioptics2.y,
                multioptics3.x, multioptics3.y,
                multioptics4.x, multioptics4.y
        );
    }

    private static void encode(FriendlyByteBuf buf, CybereyeIrisSyncS2CPayload payload) {
        buf.writeUUID(payload.playerId());

        writeMask(buf, payload.primaryMask());
        writeMask(buf, payload.secondaryMask());

        buf.writeInt(payload.monovisionX());
        buf.writeInt(payload.monovisionY());

        buf.writeInt(payload.multioptics1X());
        buf.writeInt(payload.multioptics1Y());

        buf.writeInt(payload.multioptics2X());
        buf.writeInt(payload.multioptics2Y());

        buf.writeInt(payload.multioptics3X());
        buf.writeInt(payload.multioptics3Y());

        buf.writeInt(payload.multioptics4X());
        buf.writeInt(payload.multioptics4Y());
    }

    private static CybereyeIrisSyncS2CPayload decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();

        long[] primaryMask = readMask(buf);
        long[] secondaryMask = readMask(buf);

        int monovisionX = buf.readInt();
        int monovisionY = buf.readInt();

        int multioptics1X = buf.readInt();
        int multioptics1Y = buf.readInt();

        int multioptics2X = buf.readInt();
        int multioptics2Y = buf.readInt();

        int multioptics3X = buf.readInt();
        int multioptics3Y = buf.readInt();

        int multioptics4X = buf.readInt();
        int multioptics4Y = buf.readInt();

        return new CybereyeIrisSyncS2CPayload(
                playerId,
                primaryMask,
                secondaryMask,
                monovisionX, monovisionY,
                multioptics1X, multioptics1Y,
                multioptics2X, multioptics2Y,
                multioptics3X, multioptics3Y,
                multioptics4X, multioptics4Y
        );
    }

    public static void handle(CybereyeIrisSyncS2CPayload payload) {
        Minecraft.getInstance().execute(() -> {
            CybereyeVisualConfig.Layout layout = new CybereyeVisualConfig.Layout();

            copyMask(payload.primaryMask(), layout.primary);
            copyMask(payload.secondaryMask(), layout.secondary);

            layout.optics.put(CybereyeVisualConfig.OpticsType.MONOVISION,
                    new CybereyeVisualConfig.Offset(payload.monovisionX(), payload.monovisionY()));

            layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_1,
                    new CybereyeVisualConfig.Offset(payload.multioptics1X(), payload.multioptics1Y()));

            layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_2,
                    new CybereyeVisualConfig.Offset(payload.multioptics2X(), payload.multioptics2Y()));

            layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_3,
                    new CybereyeVisualConfig.Offset(payload.multioptics3X(), payload.multioptics3Y()));

            layout.optics.put(CybereyeVisualConfig.OpticsType.MULTIOPTICS_4,
                    new CybereyeVisualConfig.Offset(payload.multioptics4X(), payload.multioptics4Y()));

            CompoundTag root = CybereyeVisualConfig.writeToNbt(layout);
            CybereyeOverlayHandler.applySyncedConfig(payload.playerId(), root);
        });
    }

    private static void writeMask(FriendlyByteBuf buf, long[] mask) {
        for (int i = 0; i < CybereyeVisualConfig.MASK_LONG_COUNT; i++) {
            long value = mask != null && i < mask.length ? mask[i] : 0L;
            buf.writeLong(value);
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