package com.perigrine3.createcybernetics.common.quickhack.modifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QuickhackModifiers {
    private static final Map<String, QuickhackModifier> MODIFIERS = new LinkedHashMap<>();

    static {
        register("duration_plus", (context, settings) -> settings.multiplyDuration(1.5F));
        register("duration_plus_plus", (context, settings) -> settings.multiplyDuration(2.5F));
        register("intensity_plus", (context, settings) -> settings.multiplyIntensity(1.5F));
        register("efficiency", (context, settings) -> settings.multiplyEfficiency(0.75F));
        register("delay", (context, settings) -> settings.addDelayTicks(40));
        register("repeat", (context, settings) -> settings.addRepeats(2));
        register("cascade", (context, settings) -> settings.setCascade(true));
        register("silent", (context, settings) -> settings.setSilent(true));
        register("persistent", (context, settings) -> settings.setPersistent(true));
        register("randomize", (context, settings) -> settings.setRandomize(true));
        register("contagion", (context, settings) -> settings.setContagion(true));
    }

    private QuickhackModifiers() {}

    public static QuickhackModifier get(String id) {
        if (id == null) return null;
        return MODIFIERS.get(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    private static void register(String id, QuickhackModifier modifier) {
        MODIFIERS.put(id, modifier);
    }
}