package com.perigrine3.createcybernetics.client.cyberspace;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CyberspaceFogHandler {

    private CyberspaceFogHandler() {}

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || !minecraft.level.dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) return;

        event.setRed(0.005F);
        event.setGreen(0.015F);
        event.setBlue(0.025F);
    }
}