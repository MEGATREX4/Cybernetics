package com.perigrine3.createcybernetics.common.breach;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.common.cyberspace.IceDetectionManager;
import com.perigrine3.createcybernetics.network.payload.BreachProtocolPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BreachProtocolManager {
    private static final int DURATION_TICKS = 15 * 20;

    private static final int FAILURE_DETECTION = 15;

    private static final int MAX_CLUSTER_BLOCKS = 4096;
    private static final double MAX_INTERACTION_DISTANCE_SQR = 64.0D;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<GateClusterKey, UUID> BUSY_CLUSTERS = new HashMap<>();

    private BreachProtocolManager() {}

    public static void open(ServerPlayer player, BlockPos gatePos) {
        if (!player.level().getBlockState(gatePos).is(ModBlocks.DIGITAL_GATE.get())) {
            return;
        }

        Session oldSession = SESSIONS.get(player.getUUID());

        if (oldSession != null) {
            release(oldSession);
        }

        Set<BlockPos> cluster = collectGateCluster(player.level(), gatePos);

        if (cluster.isEmpty()) {
            return;
        }

        GateClusterKey clusterKey = createClusterKey(player.level().dimension(), cluster);
        UUID currentOwner = BUSY_CLUSTERS.get(clusterKey);

        if (currentOwner != null && !currentOwner.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.breach_protocol.in_progress"), true);
            return;
        }

        BreachProtocolPuzzle puzzle = BreachProtocolGenerator.generate(player.getRandom());

        Session session = new Session(
                UUID.randomUUID(),
                player.getUUID(),
                player.level().dimension(),
                gatePos.immutable(),
                clusterKey,
                puzzle
        );

        SESSIONS.put(player.getUUID(), session);
        BUSY_CLUSTERS.put(clusterKey, player.getUUID());

        PacketDistributor.sendToPlayer(
                player,
                new BreachProtocolPayloads.OpenPayload(
                        session.sessionId,
                        puzzle.grid(),
                        puzzle.codes(),
                        DURATION_TICKS
                )
        );
    }

    public static void handleSelect(ServerPlayer player, BreachProtocolPayloads.SelectPayload payload) {
        Session session = SESSIONS.get(player.getUUID());

        if (session == null) return;
        if (!session.sessionId.equals(payload.sessionId())) return;

        if (!player.level().dimension().equals(session.dimension)) {
            cancelInternal(player, session, true);
            return;
        }

        if (!player.level().getBlockState(session.gatePos).is(ModBlocks.DIGITAL_GATE.get())) {
            cancelInternal(player, session, true);
            return;
        }

        if (player.distanceToSqr(
                session.gatePos.getX() + 0.5D,
                session.gatePos.getY() + 0.5D,
                session.gatePos.getZ() + 0.5D
        ) > MAX_INTERACTION_DISTANCE_SQR) {
            cancelInternal(player, session, true);
            return;
        }

        if (session.timerStarted && player.level().getGameTime() >= session.expiresAt) {
            fail(player, session);
            return;
        }

        int row = payload.row();
        int column = payload.column();

        if (row < 0 || row >= BreachProtocolPuzzle.GRID_SIZE || column < 0 || column >= BreachProtocolPuzzle.GRID_SIZE) {
            return;
        }

        if (session.selectingRow) {
            if (row != session.requiredIndex) {
                return;
            }
        } else if (column != session.requiredIndex) {
            return;
        }

        int selectedIndex = row * BreachProtocolPuzzle.GRID_SIZE + column;

        if (session.selectedIndices.contains(selectedIndex)) {
            return;
        }

        if (!session.timerStarted) {
            session.timerStarted = true;
            session.expiresAt = player.level().getGameTime() + DURATION_TICKS;
        }

        session.selectedIndices.add(selectedIndex);

        String token = session.puzzle.get(row, column);
        session.input.add(token);

        int matchedLength = getCompletedCodeLength(session.puzzle.codes(), session.input);

        if (matchedLength > 0) {
            succeed(player, session, matchedLength);
            return;
        }

        if (!canStillMatch(session.puzzle.codes(), session.input)) {
            fail(player, session);
            return;
        }

        if (session.input.size() >= 6) {
            fail(player, session);
            return;
        }

        if (session.selectingRow) {
            session.selectingRow = false;
            session.requiredIndex = column;
        } else {
            session.selectingRow = true;
            session.requiredIndex = row;
        }

        syncState(player, session);
    }

    public static void handleCancel(ServerPlayer player, BreachProtocolPayloads.CancelPayload payload) {
        Session session = SESSIONS.get(player.getUUID());

        if (session == null) return;
        if (!session.sessionId.equals(payload.sessionId())) return;

        if (session.timerStarted) {
            fail(player, session);
        } else {
            release(session);
        }
    }

    private static void succeed(ServerPlayer player, Session session, int codeLength) {
        int detectionChange = switch (codeLength) {
            case 3 -> 10;
            case 4 -> 5;
            case 5 -> 0;
            case 6 -> -5;
            default -> 0;
        };

        IceDetectionManager.addDetection(player, detectionChange);

        destroyGateCluster(player.level(), session.gatePos);

        release(session);

        PacketDistributor.sendToPlayer(player, new BreachProtocolPayloads.ClosePayload(session.sessionId, true));
        player.displayClientMessage(Component.translatable("message.createcybernetics.breach_protocol.success"), true);
    }

    private static void fail(ServerPlayer player, Session session) {
        IceDetectionManager.addDetection(player, FAILURE_DETECTION);

        release(session);

        PacketDistributor.sendToPlayer(player, new BreachProtocolPayloads.ClosePayload(session.sessionId, false));
        player.displayClientMessage(Component.translatable("message.createcybernetics.breach_protocol.failed"), true);
    }

    private static void cancelInternal(ServerPlayer player, Session session, boolean closeScreen) {
        release(session);

        if (closeScreen) {
            PacketDistributor.sendToPlayer(player, new BreachProtocolPayloads.ClosePayload(session.sessionId, false));
        }
    }

    private static void syncState(ServerPlayer player, Session session) {
        PacketDistributor.sendToPlayer(
                player,
                new BreachProtocolPayloads.StatePayload(
                        session.sessionId,
                        List.copyOf(session.selectedIndices),
                        List.copyOf(session.input),
                        session.selectingRow,
                        session.requiredIndex
                )
        );
    }

    private static int getCompletedCodeLength(List<List<String>> codes, List<String> input) {
        for (List<String> code : codes) {
            if (code.equals(input)) {
                return code.size();
            }
        }

        return 0;
    }

    private static boolean canStillMatch(List<List<String>> codes, List<String> input) {
        for (List<String> code : codes) {
            if (startsWith(code, input)) {
                return true;
            }
        }

        return false;
    }

    private static boolean startsWith(List<String> code, List<String> input) {
        if (input.size() > code.size()) {
            return false;
        }

        for (int i = 0; i < input.size(); i++) {
            if (!code.get(i).equals(input.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static void destroyGateCluster(Level level, BlockPos start) {
        Set<BlockPos> cluster = collectGateCluster(level, start);

        for (BlockPos pos : cluster) {
            if (level.getBlockState(pos).is(ModBlocks.DIGITAL_GATE.get())) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static Set<BlockPos> collectGateCluster(Level level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();

        if (!level.getBlockState(start).is(ModBlocks.DIGITAL_GATE.get())) {
            return found;
        }

        ArrayDeque<BlockPos> pending = new ArrayDeque<>();

        BlockPos initial = start.immutable();
        pending.add(initial);
        found.add(initial);

        while (!pending.isEmpty() && found.size() < MAX_CLUSTER_BLOCKS) {
            BlockPos current = pending.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);

                if (found.contains(neighbor)) {
                    continue;
                }

                if (!level.getBlockState(neighbor).is(ModBlocks.DIGITAL_GATE.get())) {
                    continue;
                }

                BlockPos immutable = neighbor.immutable();

                found.add(immutable);
                pending.addLast(immutable);
            }
        }

        return found;
    }

    private static GateClusterKey createClusterKey(ResourceKey<Level> dimension, Set<BlockPos> cluster) {
        BlockPos anchor = null;

        for (BlockPos pos : cluster) {
            if (anchor == null || comparePositions(pos, anchor) < 0) {
                anchor = pos;
            }
        }

        if (anchor == null) {
            throw new IllegalStateException("Cannot create Digital Gate cluster key for empty cluster");
        }

        return new GateClusterKey(dimension, anchor.immutable());
    }

    private static int comparePositions(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());

        if (x != 0) return x;

        int y = Integer.compare(first.getY(), second.getY());

        if (y != 0) return y;

        return Integer.compare(first.getZ(), second.getZ());
    }

    private static void release(Session session) {
        SESSIONS.remove(session.playerId);

        UUID owner = BUSY_CLUSTERS.get(session.clusterKey);

        if (session.playerId.equals(owner)) {
            BUSY_CLUSTERS.remove(session.clusterKey);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Session session = SESSIONS.get(player.getUUID());

        if (session == null) {
            return;
        }

        if (!player.level().dimension().equals(session.dimension)) {
            cancelInternal(player, session, true);
            return;
        }

        if (!player.level().getBlockState(session.gatePos).is(ModBlocks.DIGITAL_GATE.get())) {
            cancelInternal(player, session, true);
            return;
        }

        if (session.timerStarted && player.level().getGameTime() >= session.expiresAt) {
            fail(player, session);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Session session = SESSIONS.get(player.getUUID());

        if (session != null) {
            release(session);
        }
    }

    private static final class Session {
        private final UUID sessionId;
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final BlockPos gatePos;
        private final GateClusterKey clusterKey;
        private final BreachProtocolPuzzle puzzle;

        private final List<Integer> selectedIndices = new ArrayList<>();
        private final List<String> input = new ArrayList<>();

        private boolean selectingRow = true;
        private int requiredIndex;

        private boolean timerStarted;
        private long expiresAt;

        private Session(
                UUID sessionId,
                UUID playerId,
                ResourceKey<Level> dimension,
                BlockPos gatePos,
                GateClusterKey clusterKey,
                BreachProtocolPuzzle puzzle
        ) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.dimension = dimension;
            this.gatePos = gatePos;
            this.clusterKey = clusterKey;
            this.puzzle = puzzle;
        }
    }

    private record GateClusterKey(
            ResourceKey<Level> dimension,
            BlockPos anchor
    ) {}
}