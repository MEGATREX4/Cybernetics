package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.network.payload.BreachProtocolPayloads;
import com.perigrine3.createcybernetics.screen.custom.cyberspace.BreachProtocolScreen;
import net.minecraft.client.Minecraft;

public final class BreachProtocolClientHandler {

    private BreachProtocolClientHandler() {}

    public static void open(BreachProtocolPayloads.OpenPayload payload) {
        Minecraft.getInstance().setScreen(new BreachProtocolScreen(payload));
    }

    public static void update(BreachProtocolPayloads.StatePayload payload) {
        if (Minecraft.getInstance().screen instanceof BreachProtocolScreen screen && screen.matchesSession(payload.sessionId())) {
            screen.applyState(payload);
        }
    }

    public static void close(BreachProtocolPayloads.ClosePayload payload) {
        if (Minecraft.getInstance().screen instanceof BreachProtocolScreen screen && screen.matchesSession(payload.sessionId())) {
            screen.closeFromServer();
        }
    }
}