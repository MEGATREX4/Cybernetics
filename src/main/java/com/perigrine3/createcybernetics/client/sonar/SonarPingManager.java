package com.perigrine3.createcybernetics.client.sonar;

import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class SonarPingManager {
    public static final int MAX_PINGS = 16;

    private static final double DUPLICATE_DISTANCE = 0.35D;
    private static final double DUPLICATE_DISTANCE_SQR = DUPLICATE_DISTANCE * DUPLICATE_DISTANCE;
    private static final long DUPLICATE_WINDOW_NANOS = 75_000_000L;

    private static final Object LOCK = new Object();
    private static final ArrayList<Ping> PINGS = new ArrayList<>(MAX_PINGS);

    public static final class Ping {
        public final Vec3 worldPos;
        public final long timeNanos;

        public Ping(Vec3 worldPos, long timeNanos) {
            this.worldPos = worldPos;
            this.timeNanos = timeNanos;
        }
    }

    private SonarPingManager() {
    }

    public static void push(Vec3 worldPos) {
        if (!isValidPosition(worldPos)) {
            return;
        }

        long now = Util.getNanos();

        synchronized (LOCK) {
            pruneInvalidEntriesLocked();

            for (int i = PINGS.size() - 1; i >= 0; i--) {
                Ping ping = PINGS.get(i);

                if (ping == null) {
                    continue;
                }

                long age = now - ping.timeNanos;

                if (age > DUPLICATE_WINDOW_NANOS) {
                    break;
                }

                if (ping.worldPos.distanceToSqr(worldPos) <= DUPLICATE_DISTANCE_SQR) {
                    return;
                }
            }

            while (PINGS.size() >= MAX_PINGS) {
                PINGS.remove(0);
            }

            PINGS.add(new Ping(worldPos, now));
        }
    }

    public static void prune(float lifeSeconds) {
        if (lifeSeconds <= 0.0F || !Float.isFinite(lifeSeconds)) {
            clear();
            return;
        }

        long now = Util.getNanos();
        long maxAgeNanos = (long) (lifeSeconds * 1_000_000_000L);

        synchronized (LOCK) {
            PINGS.removeIf(ping -> isInvalidOrExpired(ping, now, maxAgeNanos));
        }
    }

    public static List<Ping> snapshotNewestFirst() {
        synchronized (LOCK) {
            pruneInvalidEntriesLocked();

            int size = PINGS.size();
            ArrayList<Ping> out = new ArrayList<>(size);

            for (int i = size - 1; i >= 0; i--) {
                Ping ping = PINGS.get(i);

                if (isValidPing(ping)) {
                    out.add(ping);
                }
            }

            return out;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            PINGS.clear();
        }
    }

    private static void pruneInvalidEntriesLocked() {
        PINGS.removeIf(ping -> !isValidPing(ping));
    }

    private static boolean isInvalidOrExpired(Ping ping, long now, long maxAgeNanos) {
        if (!isValidPing(ping)) {
            return true;
        }

        return now - ping.timeNanos > maxAgeNanos;
    }

    private static boolean isValidPing(Ping ping) {
        return ping != null && isValidPosition(ping.worldPos);
    }

    private static boolean isValidPosition(Vec3 pos) {
        return pos != null
                && Double.isFinite(pos.x)
                && Double.isFinite(pos.y)
                && Double.isFinite(pos.z);
    }
}