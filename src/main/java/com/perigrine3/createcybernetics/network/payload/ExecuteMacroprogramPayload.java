package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ExecuteMacroprogramPayload(int slot) implements CustomPacketPayload {
    public static final Type<ExecuteMacroprogramPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "execute_macroprogram"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExecuteMacroprogramPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.slot()), buffer -> new ExecuteMacroprogramPayload(buffer.readVarInt()));

    @Override
    public Type<ExecuteMacroprogramPayload> type() {
        return TYPE;
    }
}