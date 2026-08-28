package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CyberspaceUnsafeJackOutHoldPayload(boolean held) implements CustomPacketPayload {

    public static final Type<CyberspaceUnsafeJackOutHoldPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace_unsafe_jack_out_hold"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CyberspaceUnsafeJackOutHoldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CyberspaceUnsafeJackOutHoldPayload::held,
                    CyberspaceUnsafeJackOutHoldPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}