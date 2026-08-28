package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.network.payload.IceDetectionSyncPayload;
import net.minecraft.util.Mth;

public final class IceDetectionClientState {
    private static int detection;
    private static boolean active;
    private static boolean iceTriggered;

    private IceDetectionClientState() {}

    public static void handle(IceDetectionSyncPayload payload) {
        detection = Mth.clamp(payload.detection(), 0, 100);
        active = payload.active();
        iceTriggered = payload.iceTriggered();

        if (!active) {
            detection = 0;
            iceTriggered = false;
        }
    }

    public static int getDetection() {
        return detection;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean hasTriggeredIce() {
        return iceTriggered;
    }

    public static void clear() {
        detection = 0;
        active = false;
        iceTriggered = false;
    }
}