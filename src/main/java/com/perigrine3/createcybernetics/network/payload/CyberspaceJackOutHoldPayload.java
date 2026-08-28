package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.cyberspace.CyberspaceTravelManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CyberspaceJackOutHoldPayload(boolean holding) implements CustomPacketPayload {
    public static final Type<CyberspaceJackOutHoldPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace_jack_out_hold"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CyberspaceJackOutHoldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CyberspaceJackOutHoldPayload::holding,
                    CyberspaceJackOutHoldPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CyberspaceJackOutHoldPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        CyberspaceTravelManager.handleJackOutHold(player, payload.holding());
    }
}