package com.perigrine3.createcybernetics.client.sonar;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SonarEntityTracker {
    public static final int REVEAL_TICKS = 140;

    private static final Map<Integer, TrackedEntity> TRACKED = new HashMap<>();

    private SonarEntityTracker() {
    }

    public static void mark(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        if (entity == mc.player || entity.isRemoved() || !entity.isAlive()) {
            return;
        }

        long expiresAt = mc.level.getGameTime() + REVEAL_TICKS;

        TrackedEntity tracked = TRACKED.get(entity.getId());

        if (tracked != null) {
            tracked.expiresAt = expiresAt;
            return;
        }

        TRACKED.put(entity.getId(), new TrackedEntity(entity, expiresAt));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            clear();
            return;
        }

        long now = mc.level.getGameTime();

        Iterator<Map.Entry<Integer, TrackedEntity>> iterator = TRACKED.entrySet().iterator();

        while (iterator.hasNext()) {
            TrackedEntity tracked = iterator.next().getValue();
            LivingEntity entity = tracked.entity;

            if (entity == null || entity.isRemoved() || !entity.isAlive() || now >= tracked.expiresAt) {
                iterator.remove();
            }
        }
    }

    public static List<TrackedSnapshot> snapshot() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            return List.of();
        }

        long now = mc.level.getGameTime();

        ArrayList<TrackedSnapshot> entities = new ArrayList<>(TRACKED.size());

        for (TrackedEntity tracked : TRACKED.values()) {
            LivingEntity entity = tracked.entity;

            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                continue;
            }

            long remainingTicks = tracked.expiresAt - now;

            if (remainingTicks <= 0L) {
                continue;
            }

            float strength = Math.min(1.0F, remainingTicks / (float) REVEAL_TICKS);

            entities.add(new TrackedSnapshot(entity, strength));
        }

        return entities;
    }

    public static void clear() {
        TRACKED.clear();
    }

    public record TrackedSnapshot(LivingEntity entity, float strength) {
    }

    private static final class TrackedEntity {
        private final LivingEntity entity;
        private long expiresAt;

        private TrackedEntity(LivingEntity entity, long expiresAt) {
            this.entity = entity;
            this.expiresAt = expiresAt;
        }
    }
}