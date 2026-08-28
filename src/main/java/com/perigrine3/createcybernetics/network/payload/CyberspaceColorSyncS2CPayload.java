package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.CyberspaceColorConfigClient;
import com.perigrine3.createcybernetics.common.cyberspace.CyberspaceColorData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record CyberspaceColorSyncS2CPayload(UUID playerId, int color) implements CustomPacketPayload {

    public static final Type<CyberspaceColorSyncS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace_color_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, CyberspaceColorSyncS2CPayload> STREAM_CODEC =
            StreamCodec.of(CyberspaceColorSyncS2CPayload::encode, CyberspaceColorSyncS2CPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, CyberspaceColorSyncS2CPayload payload) {
        buf.writeUUID(payload.playerId());
        buf.writeInt(payload.color());
    }

    private static CyberspaceColorSyncS2CPayload decode(FriendlyByteBuf buf) {
        return new CyberspaceColorSyncS2CPayload(buf.readUUID(), buf.readInt());
    }

    public static void handle(CyberspaceColorSyncS2CPayload payload) {
        Minecraft.getInstance().execute(() ->
                CyberspaceColorConfigClient.applySyncedColor(payload.playerId(), CyberspaceColorData.sanitize(payload.color()))
        );
    }
}