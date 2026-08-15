package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.skin.CybereyeOverlayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CybereyeIrisClientEvents {

    private CybereyeIrisClientEvents() {}

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        CybereyeIrisConfigClient.reload(player.getUUID());
        CybereyeIrisConfigClient.applyCachedConfigToPlayer(player);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        CybereyeIrisConfigClient.invalidate(player.getUUID());
        CybereyeOverlayHandler.clearAll();
    }
}