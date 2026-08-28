package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.network.payload.MacroprogramRuntimeSyncPayload;

import java.util.HashMap;
import java.util.Map;

public final class MacroprogramClientRuntimeState {
    private static final Map<String, State> STATES = new HashMap<>();

    private MacroprogramClientRuntimeState() {}

    public static void handle(MacroprogramRuntimeSyncPayload payload) {
        STATES.clear();

        for (MacroprogramRuntimeSyncPayload.Entry entry : payload.entries()) {
            STATES.put(entry.id(), new State(entry.activeRemaining(), entry.cooldownRemaining(), entry.usesRemaining()));
        }
    }

    public static State get(String id) {
        return STATES.getOrDefault(id, State.EMPTY);
    }

    public static void clear() {
        STATES.clear();
    }

    public record State(int activeRemaining, int cooldownRemaining, int usesRemaining) {
        private static final State EMPTY = new State(0, 0, -1);

        public boolean active() {
            return activeRemaining != 0;
        }

        public boolean untimedActive() {
            return activeRemaining < 0;
        }

        public boolean coolingDown() {
            return cooldownRemaining > 0;
        }
    }
}