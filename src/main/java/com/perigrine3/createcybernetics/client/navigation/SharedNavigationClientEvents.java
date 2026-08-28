package com.perigrine3.createcybernetics.client.navigation;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.MinimapWaypointClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SharedNavigationClientEvents {

    private SharedNavigationClientEvents() {}

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        MinimapWaypointClient.clearWorldIdentityCache();
        SharedNavigationClientState.clear();
        SharedNavigationClientSync.clearQueues();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MinimapWaypointClient.clearWorldIdentityCache();
        SharedNavigationClientState.clear();
        SharedNavigationClientSync.clearQueues();
    }
}
