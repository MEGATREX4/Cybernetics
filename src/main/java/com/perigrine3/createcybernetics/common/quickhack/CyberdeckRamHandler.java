package com.perigrine3.createcybernetics.common.quickhack;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CyberdeckRamHandler {
    private CyberdeckRamHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        PlayerCyberwareData data = serverPlayer.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        if (data.tickCyberdeckRam(serverPlayer)) {
            ModAttachments.syncCyberware(serverPlayer);
        }
    }
}