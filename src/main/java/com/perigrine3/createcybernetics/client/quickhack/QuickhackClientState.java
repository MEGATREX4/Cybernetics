package com.perigrine3.createcybernetics.client.quickhack;

import java.util.concurrent.ThreadLocalRandom;

public final class QuickhackClientState {
    private static boolean invertMovement;
    private static boolean invertVision;
    private static boolean motorLatency;
    private static boolean opticsMalfunction;
    private static boolean powerMalfunction;

    private static int falseEnergy = -1;
    private static long nextFalseEnergyTick = Long.MIN_VALUE;

    private QuickhackClientState() {}

    public static boolean invertMovement() {
        return invertMovement;
    }

    public static boolean invertVision() {
        return invertVision;
    }

    public static boolean motorLatency() {
        return motorLatency;
    }

    public static boolean opticsMalfunction() {
        return opticsMalfunction;
    }

    public static boolean powerMalfunction() {
        return powerMalfunction;
    }

    public static float opticsMalfunctionShaderIntensity() {
        return opticsMalfunction ? 0.85F : 0.0F;
    }

    public static int resolveDisplayedEnergy(int actualEnergy, int capacity, long gameTime) {
        actualEnergy = Math.max(0, actualEnergy);
        capacity = Math.max(0, capacity);

        if (!powerMalfunction || capacity <= 0) {
            resetFalseEnergy();
            return actualEnergy;
        }

        if (falseEnergy < 0 || gameTime >= nextFalseEnergyTick) {
            ThreadLocalRandom random = ThreadLocalRandom.current();

            int minimumOffset = Math.max(1, Math.round(capacity * 0.15F));
            int maximumOffset = Math.max(minimumOffset, Math.round(capacity * 0.55F));
            int offset = random.nextInt(minimumOffset, maximumOffset + 1);

            if (random.nextBoolean()) {
                falseEnergy = Math.min(capacity, actualEnergy + offset);
            } else {
                falseEnergy = Math.max(0, actualEnergy - offset);
            }

            if (falseEnergy == actualEnergy) {
                falseEnergy = actualEnergy >= capacity / 2 ? Math.max(0, actualEnergy - minimumOffset) : Math.min(capacity, actualEnergy + minimumOffset);
            }

            nextFalseEnergyTick = gameTime + random.nextInt(10, 31);
        }

        return falseEnergy;
    }

    public static void set(boolean invertMovement, boolean invertVision, boolean motorLatency, boolean opticsMalfunction, boolean powerMalfunction) {
        QuickhackClientState.invertMovement = invertMovement;
        QuickhackClientState.invertVision = invertVision;
        QuickhackClientState.motorLatency = motorLatency;
        QuickhackClientState.opticsMalfunction = opticsMalfunction;

        if (QuickhackClientState.powerMalfunction != powerMalfunction) {
            resetFalseEnergy();
        }

        QuickhackClientState.powerMalfunction = powerMalfunction;
    }

    public static void clear() {
        invertMovement = false;
        invertVision = false;
        motorLatency = false;
        opticsMalfunction = false;
        powerMalfunction = false;
        resetFalseEnergy();
    }

    private static void resetFalseEnergy() {
        falseEnergy = -1;
        nextFalseEnergyTick = Long.MIN_VALUE;
    }
}