package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.quickhack.QuickhackClientState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuickhackClientStatePayload(boolean invertMovement, boolean invertVision, boolean motorLatency, boolean opticsMalfunction, boolean powerMalfunction) implements CustomPacketPayload {
    public static final Type<QuickhackClientStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "quickhack_client_state"));
    public static final StreamCodec<ByteBuf, QuickhackClientStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.invertMovement());
                buffer.writeBoolean(payload.invertVision());
                buffer.writeBoolean(payload.motorLatency());
                buffer.writeBoolean(payload.opticsMalfunction());
                buffer.writeBoolean(payload.powerMalfunction());
            },
            buffer -> new QuickhackClientStatePayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean())
    );

    public static void handle(QuickhackClientStatePayload payload) {
        QuickhackClientState.set(payload.invertMovement(), payload.invertVision(), payload.motorLatency(), payload.opticsMalfunction(), payload.powerMalfunction());
    }

    @Override
    public Type<QuickhackClientStatePayload> type() {
        return TYPE;
    }
}