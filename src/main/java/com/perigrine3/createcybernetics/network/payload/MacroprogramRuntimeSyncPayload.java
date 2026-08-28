package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MacroprogramRuntimeSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<MacroprogramRuntimeSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "macroprogram_runtime_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MacroprogramRuntimeSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entries().size());

                for (Entry entry : payload.entries()) {
                    buffer.writeUtf(entry.id());
                    buffer.writeVarInt(entry.activeRemaining() + 1);
                    buffer.writeVarInt(entry.cooldownRemaining());
                    buffer.writeVarInt(entry.usesRemaining() + 1);
                }
            },
            buffer -> {
                int size = buffer.readVarInt();
                List<Entry> entries = new ArrayList<>(size);

                for (int i = 0; i < size; i++) {
                    String id = buffer.readUtf();
                    int activeRemaining = buffer.readVarInt() - 1;
                    int cooldownRemaining = buffer.readVarInt();
                    int usesRemaining = buffer.readVarInt() - 1;
                    entries.add(new Entry(id, activeRemaining, cooldownRemaining, usesRemaining));
                }

                return new MacroprogramRuntimeSyncPayload(entries);
            }
    );

    @Override
    public Type<MacroprogramRuntimeSyncPayload> type() {
        return TYPE;
    }

    public record Entry(String id, int activeRemaining, int cooldownRemaining, int usesRemaining) {}
}