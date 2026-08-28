package com.perigrine3.createcybernetics.common.quickhack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MicroprogramRegistry {
    private static final Map<String, MicroprogramDefinition> DEFINITIONS = new LinkedHashMap<>();

    public static final MicroprogramDefinition SYSTEM_ALL = register("system_all", MicroprogramCategory.TARGET, "System All", 4);
    public static final MicroprogramDefinition PROCESSOR = register("processor", MicroprogramCategory.TARGET, "Processor", 2);
    public static final MicroprogramDefinition CYBEROPTICS = register("cyberoptics", MicroprogramCategory.TARGET, "Cyberoptics", 2);
    public static final MicroprogramDefinition AUDITORY = register("auditory", MicroprogramCategory.TARGET, "Auditory", 2);
    public static final MicroprogramDefinition MOTOR_CONTROL = register("motor_control", MicroprogramCategory.TARGET, "Motor Control", 3);
    public static final MicroprogramDefinition ARMS = register("arms", MicroprogramCategory.TARGET, "Arms", 2);
    public static final MicroprogramDefinition LEGS = register("legs", MicroprogramCategory.TARGET, "Legs", 2);
    public static final MicroprogramDefinition INTERNAL_ORGANS = register("internal_organs", MicroprogramCategory.TARGET, "Internal Organs", 2);
    public static final MicroprogramDefinition SKELETON = register("skeleton", MicroprogramCategory.TARGET, "Skeleton", 2);
    public static final MicroprogramDefinition DERMAL = register("dermal", MicroprogramCategory.TARGET, "Dermal", 2);
    public static final MicroprogramDefinition POWER_SYSTEM = register("power_system", MicroprogramCategory.TARGET, "Power System", 2);
    public static final MicroprogramDefinition CYBERDECK = register("cyberdeck", MicroprogramCategory.TARGET, "Cyberdeck", 3);
    public static final MicroprogramDefinition NEURAL_SYSTEM = register("neural_system", MicroprogramCategory.TARGET, "Neural System", 3);
    public static final MicroprogramDefinition ACTIVE_CYBERWARE = register("active_cyberware", MicroprogramCategory.TARGET, "Active Cyberware", 3);
    public static final MicroprogramDefinition HIGHEST_DRAW = register("highest_draw", MicroprogramCategory.TARGET, "Highest Draw", 3);
    public static final MicroprogramDefinition RANDOM_IMPLANT = register("random_implant", MicroprogramCategory.TARGET, "Random Implant", 1);

    public static final MicroprogramDefinition REBOOT = register("reboot", MicroprogramCategory.FUNCTION, "Reboot", 3);
    public static final MicroprogramDefinition OVERCLOCK = register("overclock", MicroprogramCategory.FUNCTION, "Overclock", 3);
    public static final MicroprogramDefinition DRAIN = register("drain", MicroprogramCategory.FUNCTION, "Drain", 3);
    public static final MicroprogramDefinition SPIKE = register("spike", MicroprogramCategory.FUNCTION, "Spike", 4);
    public static final MicroprogramDefinition BURNOUT = register("burnout", MicroprogramCategory.FUNCTION, "Burnout", 5);
    public static final MicroprogramDefinition TOGGLE_OFF = register("toggle_off", MicroprogramCategory.FUNCTION, "Toggle Off", 2);
    public static final MicroprogramDefinition TOGGLE_ON = register("toggle_on", MicroprogramCategory.FUNCTION, "Toggle On", 2);
    public static final MicroprogramDefinition MALFUNCTION = register("malfunction", MicroprogramCategory.FUNCTION, "Malfunction", 3);
    public static final MicroprogramDefinition LOOP = register("loop", MicroprogramCategory.FUNCTION, "Loop", 4);
    public static final MicroprogramDefinition PING = register("ping", MicroprogramCategory.FUNCTION, "Ping", 1);
    public static final MicroprogramDefinition TRACE = register("trace", MicroprogramCategory.FUNCTION, "Trace", 2);
    public static final MicroprogramDefinition CORRUPT = register("corrupt", MicroprogramCategory.FUNCTION, "Corrupt", 5);
    public static final MicroprogramDefinition PURGE = register("purge", MicroprogramCategory.FUNCTION, "Purge", 3);
    public static final MicroprogramDefinition DESYNC = register("desync", MicroprogramCategory.FUNCTION, "Desync", 4);

    public static final MicroprogramDefinition DURATION_PLUS = register("duration_plus", MicroprogramCategory.MODIFIER, "Duration+", 2);
    public static final MicroprogramDefinition DURATION_PLUS_PLUS = register("duration_plus_plus", MicroprogramCategory.MODIFIER, "Duration++", 4);
    public static final MicroprogramDefinition INTENSITY_PLUS = register("intensity_plus", MicroprogramCategory.MODIFIER, "Intensity+", 3);
    public static final MicroprogramDefinition EFFICIENCY = register("efficiency", MicroprogramCategory.MODIFIER, "Efficiency", -2);
    public static final MicroprogramDefinition DELAY = register("delay", MicroprogramCategory.MODIFIER, "Delay", 1);
    public static final MicroprogramDefinition REPEAT = register("repeat", MicroprogramCategory.MODIFIER, "Repeat", 3);
    public static final MicroprogramDefinition CASCADE = register("cascade", MicroprogramCategory.MODIFIER, "Cascade", 4);
    public static final MicroprogramDefinition SILENT = register("silent", MicroprogramCategory.MODIFIER, "Silent", 2);
    public static final MicroprogramDefinition PERSISTENT = register("persistent", MicroprogramCategory.MODIFIER, "Persistent", 4);
    public static final MicroprogramDefinition RANDOMIZE = register("randomize", MicroprogramCategory.MODIFIER, "Randomize", -1);
    public static final MicroprogramDefinition CONTAGION = register("contagion", MicroprogramCategory.MODIFIER, "Contagion", 4);

    private MicroprogramRegistry() {}

    private static MicroprogramDefinition register(String id, MicroprogramCategory category, String displayName, int ramCost) {
        MicroprogramDefinition definition = new MicroprogramDefinition(id, category, displayName, ramCost);
        DEFINITIONS.put(id, definition);
        return definition;
    }

    public static MicroprogramDefinition get(String id) {
        if (id == null) {
            return null;
        }

        return DEFINITIONS.get(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static List<MicroprogramDefinition> getAll() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static List<MicroprogramDefinition> getByCategory(MicroprogramCategory category) {
        List<MicroprogramDefinition> definitions = new ArrayList<>();

        for (MicroprogramDefinition definition : DEFINITIONS.values()) {
            if (definition.category() == category) {
                definitions.add(definition);
            }
        }

        return List.copyOf(definitions);
    }
}