package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record IceDetectionSyncPayload(
        int detection,
        boolean active,
        boolean iceTriggered
) implements CustomPacketPayload {
    public static final Type<IceDetectionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "ice_detection_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IceDetectionSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    IceDetectionSyncPayload::detection,
                    ByteBufCodecs.BOOL,
                    IceDetectionSyncPayload::active,
                    ByteBufCodecs.BOOL,
                    IceDetectionSyncPayload::iceTriggered,
                    IceDetectionSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}