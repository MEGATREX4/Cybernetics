package com.perigrine3.createcybernetics.client.biomonitor;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class BiomonitorObfuscationController {

    private static final int LEVEL_2_MIN_OBFUSCATED_TICKS = 12;
    private static final int LEVEL_2_MAX_OBFUSCATED_TICKS = 32;

    private static final int LEVEL_2_MIN_CLEAR_TICKS = 80;
    private static final int LEVEL_2_MAX_CLEAR_TICKS = 240;

    private static UUID trackedPlayerId;

    private static int previousRejectionLevel = 0;
    private static int obfuscatedTicksRemaining = 0;
    private static int clearTicksRemaining = 0;

    private static boolean obfuscated = false;

    private BiomonitorObfuscationController() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (minecraft.isPaused()) {
            return;
        }

        if (player == null) {
            clear();
            return;
        }

        if (trackedPlayerId == null || !trackedPlayerId.equals(player.getUUID())) {
            clear();
            trackedPlayerId = player.getUUID();
        }

        MobEffectInstance rejection = player.getEffect(ModEffects.CYBERWARE_REJECTION);
        int rejectionLevel = rejection == null ? 0 : Mth.clamp(rejection.getAmplifier() + 1, 1, 3);

        if (rejectionLevel < 2) {
            resetEpisode();
            previousRejectionLevel = rejectionLevel;
            return;
        }

        if (rejectionLevel >= 3) {
            obfuscated = true;
            obfuscatedTicksRemaining = 0;
            clearTicksRemaining = 0;
            previousRejectionLevel = rejectionLevel;
            return;
        }

        if (previousRejectionLevel != rejectionLevel) {
            resetEpisode();
            scheduleClearPeriod(player);
            previousRejectionLevel = rejectionLevel;
        }

        tickLevelTwo(player);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void tickLevelTwo(LocalPlayer player) {
        if (obfuscated) {
            if (obfuscatedTicksRemaining > 0) {
                obfuscatedTicksRemaining--;
            }

            if (obfuscatedTicksRemaining <= 0) {
                obfuscated = false;
                scheduleClearPeriod(player);
            }

            return;
        }

        if (clearTicksRemaining > 0) {
            clearTicksRemaining--;
        }

        if (clearTicksRemaining <= 0) {
            beginObfuscatedPeriod(player);
        }
    }

    private static void beginObfuscatedPeriod(LocalPlayer player) {
        obfuscated = true;
        obfuscatedTicksRemaining = Mth.nextInt(player.getRandom(), LEVEL_2_MIN_OBFUSCATED_TICKS, LEVEL_2_MAX_OBFUSCATED_TICKS);
        clearTicksRemaining = 0;
    }

    private static void scheduleClearPeriod(LocalPlayer player) {
        obfuscated = false;
        obfuscatedTicksRemaining = 0;
        clearTicksRemaining = Mth.nextInt(player.getRandom(), LEVEL_2_MIN_CLEAR_TICKS, LEVEL_2_MAX_CLEAR_TICKS);
    }

    public static boolean shouldObfuscate(LocalPlayer player) {
        if (player == null) {
            return false;
        }

        MobEffectInstance rejection = player.getEffect(ModEffects.CYBERWARE_REJECTION);

        if (rejection == null) {
            return false;
        }

        int rejectionLevel = Mth.clamp(rejection.getAmplifier() + 1, 1, 3);

        if (rejectionLevel >= 3) {
            return true;
        }

        return rejectionLevel == 2 && obfuscated;
    }

    private static void resetEpisode() {
        obfuscated = false;
        obfuscatedTicksRemaining = 0;
        clearTicksRemaining = 0;
    }

    private static void clear() {
        trackedPlayerId = null;
        previousRejectionLevel = 0;

        resetEpisode();
    }
}