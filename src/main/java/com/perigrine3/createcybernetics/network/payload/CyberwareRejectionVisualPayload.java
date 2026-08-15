package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.render.rejection.CyberwareRejectionClientState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.UUID;

public record CyberwareRejectionVisualPayload(
        UUID playerId,
        int rejectionLevel,
        boolean fugue
) implements CustomPacketPayload {

    public static final Type<CyberwareRejectionVisualPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberware_rejection_visual"));

    public static final StreamCodec<ByteBuf, CyberwareRejectionVisualPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CyberwareRejectionVisualPayload decode(ByteBuf buffer) {
            UUID playerId = new UUID(buffer.readLong(), buffer.readLong());
            int rejectionLevel = Mth.clamp(buffer.readInt(), 0, 3);
            boolean fugue = buffer.readBoolean();

            return new CyberwareRejectionVisualPayload(playerId, rejectionLevel, fugue);
        }

        @Override
        public void encode(ByteBuf buffer, CyberwareRejectionVisualPayload payload) {
            buffer.writeLong(payload.playerId().getMostSignificantBits());
            buffer.writeLong(payload.playerId().getLeastSignificantBits());
            buffer.writeInt(Mth.clamp(payload.rejectionLevel(), 0, 3));
            buffer.writeBoolean(payload.fugue());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CyberwareRejectionVisualPayload payload) {
        CyberwareRejectionClientState.accept(payload.playerId(), payload.rejectionLevel(), payload.fugue());
    }
}