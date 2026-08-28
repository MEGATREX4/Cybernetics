package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.cyberspace.CyberspaceColorData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public record CyberspaceColorSyncC2SPayload(int color) implements CustomPacketPayload {

    public static final Type<CyberspaceColorSyncC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace_color_sync_c2s"));

    public static final StreamCodec<FriendlyByteBuf, CyberspaceColorSyncC2SPayload> STREAM_CODEC =
            StreamCodec.of(CyberspaceColorSyncC2SPayload::encode, CyberspaceColorSyncC2SPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, CyberspaceColorSyncC2SPayload payload) {
        buf.writeInt(payload.color());
    }

    private static CyberspaceColorSyncC2SPayload decode(FriendlyByteBuf buf) {
        return new CyberspaceColorSyncC2SPayload(buf.readInt());
    }

    public static void handle(CyberspaceColorSyncC2SPayload payload, ServerPlayer sender) {
        if (sender == null) return;

        int color = CyberspaceColorData.sanitize(payload.color());
        CyberspaceColorData.set(sender, color);

        CyberspaceColorSyncS2CPayload outgoing = new CyberspaceColorSyncS2CPayload(sender.getUUID(), color);

        PacketDistributor.sendToPlayer(sender, outgoing);
        PacketDistributor.sendToPlayersTrackingEntity(sender, outgoing);
    }
}