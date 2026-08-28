package com.perigrine3.createcybernetics.common.quickhack;

public enum QuickhackTargetDomain {
    SYSTEM_ALL,
    PROCESSOR,
    CYBEROPTICS,
    AUDITORY,
    MOTOR_CONTROL,
    ARMS,
    LEGS,
    INTERNAL_ORGANS,
    SKELETON,
    DERMAL,
    POWER_SYSTEM,
    CYBERDECK,
    NEURAL_SYSTEM,
    ACTIVE_CYBERWARE,
    HIGHEST_DRAW,
    RANDOM_IMPLANT;

    public static QuickhackTargetDomain fromId(String id) {
        if (id == null) return SYSTEM_ALL;

        return switch (id) {
            case "processor" -> PROCESSOR;
            case "cyberoptics" -> CYBEROPTICS;
            case "auditory" -> AUDITORY;
            case "motor_control" -> MOTOR_CONTROL;
            case "arms" -> ARMS;
            case "legs" -> LEGS;
            case "internal_organs" -> INTERNAL_ORGANS;
            case "skeleton" -> SKELETON;
            case "dermal" -> DERMAL;
            case "power_system" -> POWER_SYSTEM;
            case "cyberdeck" -> CYBERDECK;
            case "neural_system" -> NEURAL_SYSTEM;
            case "active_cyberware" -> ACTIVE_CYBERWARE;
            case "highest_draw" -> HIGHEST_DRAW;
            case "random_implant" -> RANDOM_IMPLANT;
            default -> SYSTEM_ALL;
        };
    }

    public boolean isGlobalDrainTarget() {
        return this == SYSTEM_ALL || this == POWER_SYSTEM;
    }

    public boolean affectsArms() {
        return this == ARMS || this == MOTOR_CONTROL;
    }

    public boolean affectsLegs() {
        return this == LEGS || this == MOTOR_CONTROL;
    }

    public boolean affectsEyes() {
        return this == CYBEROPTICS;
    }
}
