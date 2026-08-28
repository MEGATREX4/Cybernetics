package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BreachProtocolPayloads {

    private BreachProtocolPayloads() {}

    public record OpenPayload(
            UUID sessionId,
            List<String> grid,
            List<List<String>> codes,
            int durationTicks
    ) implements CustomPacketPayload {

        public static final Type<OpenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "breach_protocol_open"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public OpenPayload decode(RegistryFriendlyByteBuf buffer) {
                UUID sessionId = buffer.readUUID();

                int gridSize = buffer.readVarInt();
                List<String> grid = new ArrayList<>(gridSize);

                for (int i = 0; i < gridSize; i++) {
                    grid.add(buffer.readUtf());
                }

                int codeCount = buffer.readVarInt();
                List<List<String>> codes = new ArrayList<>(codeCount);

                for (int i = 0; i < codeCount; i++) {
                    int codeLength = buffer.readVarInt();
                    List<String> code = new ArrayList<>(codeLength);

                    for (int j = 0; j < codeLength; j++) {
                        code.add(buffer.readUtf());
                    }

                    codes.add(code);
                }

                int durationTicks = buffer.readVarInt();

                return new OpenPayload(sessionId, grid, codes, durationTicks);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, OpenPayload payload) {
                buffer.writeUUID(payload.sessionId());

                buffer.writeVarInt(payload.grid().size());

                for (String token : payload.grid()) {
                    buffer.writeUtf(token);
                }

                buffer.writeVarInt(payload.codes().size());

                for (List<String> code : payload.codes()) {
                    buffer.writeVarInt(code.size());

                    for (String token : code) {
                        buffer.writeUtf(token);
                    }
                }

                buffer.writeVarInt(payload.durationTicks());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StatePayload(
            UUID sessionId,
            List<Integer> selectedIndices,
            List<String> input,
            boolean selectingRow,
            int requiredIndex
    ) implements CustomPacketPayload {

        public static final Type<StatePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "breach_protocol_state"));

        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public StatePayload decode(RegistryFriendlyByteBuf buffer) {
                UUID sessionId = buffer.readUUID();

                int selectedCount = buffer.readVarInt();
                List<Integer> selectedIndices = new ArrayList<>(selectedCount);

                for (int i = 0; i < selectedCount; i++) {
                    selectedIndices.add(buffer.readVarInt());
                }

                int inputCount = buffer.readVarInt();
                List<String> input = new ArrayList<>(inputCount);

                for (int i = 0; i < inputCount; i++) {
                    input.add(buffer.readUtf());
                }

                boolean selectingRow = buffer.readBoolean();
                int requiredIndex = buffer.readVarInt();

                return new StatePayload(sessionId, selectedIndices, input, selectingRow, requiredIndex);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, StatePayload payload) {
                buffer.writeUUID(payload.sessionId());

                buffer.writeVarInt(payload.selectedIndices().size());

                for (int index : payload.selectedIndices()) {
                    buffer.writeVarInt(index);
                }

                buffer.writeVarInt(payload.input().size());

                for (String token : payload.input()) {
                    buffer.writeUtf(token);
                }

                buffer.writeBoolean(payload.selectingRow());
                buffer.writeVarInt(payload.requiredIndex());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClosePayload(
            UUID sessionId,
            boolean success
    ) implements CustomPacketPayload {

        public static final Type<ClosePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "breach_protocol_close"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClosePayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ClosePayload decode(RegistryFriendlyByteBuf buffer) {
                return new ClosePayload(buffer.readUUID(), buffer.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ClosePayload payload) {
                buffer.writeUUID(payload.sessionId());
                buffer.writeBoolean(payload.success());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SelectPayload(
            UUID sessionId,
            int row,
            int column
    ) implements CustomPacketPayload {

        public static final Type<SelectPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "breach_protocol_select"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SelectPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SelectPayload decode(RegistryFriendlyByteBuf buffer) {
                return new SelectPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SelectPayload payload) {
                buffer.writeUUID(payload.sessionId());
                buffer.writeVarInt(payload.row());
                buffer.writeVarInt(payload.column());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CancelPayload(
            UUID sessionId
    ) implements CustomPacketPayload {

        public static final Type<CancelPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "breach_protocol_cancel"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CancelPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CancelPayload decode(RegistryFriendlyByteBuf buffer) {
                return new CancelPayload(buffer.readUUID());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CancelPayload payload) {
                buffer.writeUUID(payload.sessionId());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}