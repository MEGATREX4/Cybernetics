package com.perigrine3.createcybernetics.common.cyberspace;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.network.payload.IceDetectionSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class IceDetectionManager {
    public static final int MIN_DETECTION = 0;
    public static final int MAX_DETECTION = 100;

    private static final Map<UUID, DetectionSession> SESSIONS = new HashMap<>();

    private IceDetectionManager() {}

    public static void startSession(ServerPlayer player) {
        DetectionSession session = new DetectionSession();
        SESSIONS.put(player.getUUID(), session);
        sync(player, session);
    }

    public static void endSession(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new IceDetectionSyncPayload(0, false, false));
    }

    public static boolean hasActiveSession(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static int getDetection(ServerPlayer player) {
        DetectionSession session = SESSIONS.get(player.getUUID());
        return session == null ? 0 : session.detection;
    }

    public static boolean hasTriggeredIce(ServerPlayer player) {
        DetectionSession session = SESSIONS.get(player.getUUID());
        return session != null && session.iceTriggered;
    }

    public static int addDetection(ServerPlayer player, int amount) {
        DetectionSession session = SESSIONS.get(player.getUUID());

        if (session == null || amount == 0) {
            return session == null ? 0 : session.detection;
        }

        int previousDetection = session.detection;
        session.detection = Mth.clamp(session.detection + amount, MIN_DETECTION, MAX_DETECTION);

        if (session.detection >= MAX_DETECTION && previousDetection < MAX_DETECTION && !session.iceTriggered) {
            session.iceTriggered = true;
            triggerFinalIce(player);
        }

        sync(player, session);

        return session.detection;
    }

    public static int setDetection(ServerPlayer player, int detection) {
        DetectionSession session = SESSIONS.get(player.getUUID());

        if (session == null) {
            return 0;
        }

        int previousDetection = session.detection;
        session.detection = Mth.clamp(detection, MIN_DETECTION, MAX_DETECTION);

        if (session.detection >= MAX_DETECTION && previousDetection < MAX_DETECTION && !session.iceTriggered) {
            session.iceTriggered = true;
            triggerFinalIce(player);
        }

        sync(player, session);

        return session.detection;
    }

    private static void triggerFinalIce(ServerPlayer player) {
    }

    private static void sync(ServerPlayer player, DetectionSession session) {
        PacketDistributor.sendToPlayer(player, new IceDetectionSyncPayload(session.detection, true, session.iceTriggered));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SESSIONS.remove(player.getUUID());
        }
    }

    private static final class DetectionSession {
        private int detection;
        private boolean iceTriggered;
    }
}