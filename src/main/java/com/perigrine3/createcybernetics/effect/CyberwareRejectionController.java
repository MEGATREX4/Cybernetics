package com.perigrine3.createcybernetics.effect;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CyberwareRejectionController {

    private static final int REFRESH_EVERY_TICKS = 20;
    private static final int DURATION = 120;

    private CyberwareRejectionController() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null) {
            player.removeEffect(ModEffects.CYBERWARE_REJECTION);
            CyberpsychosisFugueController.clear(player);
            syncVisualState(player);
            return;
        }

        CyberpsychosisSeverity severity = CyberpsychosisSeverity.fromPlayer(player);

        if (severity == CyberpsychosisSeverity.NONE) {
            if (player.hasEffect(ModEffects.CYBERWARE_REJECTION)) {
                player.removeEffect(ModEffects.CYBERWARE_REJECTION);
            }

            CyberpsychosisFugueController.clear(player);
            syncVisualState(player);
            return;
        }

        if (player.tickCount % REFRESH_EVERY_TICKS == 0) {
            refreshRejectionEffect(player, severity);
        }

        if (severity == CyberpsychosisSeverity.LEVEL_3) {
            CyberpsychosisFugueController.tick(player);
        } else {
            CyberpsychosisFugueController.clear(player);
        }

        syncVisualState(player);
    }

    private static void refreshRejectionEffect(Player player, CyberpsychosisSeverity severity) {
        MobEffectInstance existing = player.getEffect(ModEffects.CYBERWARE_REJECTION);
        int amplifier = severity.id() - 1;

        if (existing == null || existing.getDuration() <= DURATION - REFRESH_EVERY_TICKS || existing.getAmplifier() != amplifier) {
            player.addEffect(new MobEffectInstance(ModEffects.CYBERWARE_REJECTION, DURATION, amplifier, false, true, true));
        }
    }

    private static void syncVisualState(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CyberwareRejectionVisualSync.syncIfChanged(serverPlayer);
        }
    }
}