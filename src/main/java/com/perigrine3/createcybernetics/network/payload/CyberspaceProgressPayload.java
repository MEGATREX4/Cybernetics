package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.CyberspaceProgressHud;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CyberspaceProgressPayload(int mode, int percent, boolean active) implements CustomPacketPayload {
    public static final int JACK_IN = 0;
    public static final int SAFE_JACK_OUT = 1;
    public static final int UNSAFE_JACK_OUT = 2;

    public static final Type<CyberspaceProgressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CyberspaceProgressPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CyberspaceProgressPayload::mode,
                    ByteBufCodecs.VAR_INT,
                    CyberspaceProgressPayload::percent,
                    ByteBufCodecs.BOOL,
                    CyberspaceProgressPayload::active,
                    CyberspaceProgressPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CyberspaceProgressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CyberspaceProgressHud.handle(payload));
    }
}