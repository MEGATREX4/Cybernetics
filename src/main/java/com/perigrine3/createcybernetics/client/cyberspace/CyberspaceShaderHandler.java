package com.perigrine3.createcybernetics.client.cyberspace;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CyberspaceShaderHandler {

    private static boolean cyberspaceShaderActive = false;

    private CyberspaceShaderHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        boolean inCyberspace = minecraft.level != null
                && minecraft.level.dimension().equals(ModDimensions.CYBERSPACE_LEVEL);

        if (inCyberspace && !cyberspaceShaderActive) {
            loadCyberspaceShader();
            cyberspaceShaderActive = true;
        } else if (!inCyberspace && cyberspaceShaderActive) {
            unloadCyberspaceShader();
            cyberspaceShaderActive = false;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (cyberspaceShaderActive) {
            unloadCyberspaceShader();
            cyberspaceShaderActive = false;
        }
    }

    private static void loadCyberspaceShader() {
    }

    private static void unloadCyberspaceShader() {
    }
}