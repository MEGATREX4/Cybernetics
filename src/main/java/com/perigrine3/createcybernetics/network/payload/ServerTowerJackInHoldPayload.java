package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerTowerJackInHoldPayload(boolean held) implements CustomPacketPayload {
    public static final Type<ServerTowerJackInHoldPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "server_tower_jack_in_hold"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerTowerJackInHoldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ServerTowerJackInHoldPayload::held,
                    ServerTowerJackInHoldPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}