package com.perigrine3.createcybernetics.client.render.rejection;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CyberwareRejectionClientState {

    private static final Map<UUID, RejectionVisualState> STATES = new HashMap<>();

    private CyberwareRejectionClientState() {
    }

    public static void accept(UUID playerId, int rejectionLevel, boolean fugue) {
        if (playerId == null) {
            return;
        }

        int level = Mth.clamp(rejectionLevel, 0, 3);

        if (level <= 0 && !fugue) {
            STATES.remove(playerId);
            return;
        }

        STATES.put(playerId, new RejectionVisualState(level, fugue));
    }

    public static int getRejectionLevel(AbstractClientPlayer player) {
        if (player == null) {
            return 0;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;

        if (player == localPlayer) {
            MobEffectInstance rejection = player.getEffect(ModEffects.CYBERWARE_REJECTION);
            return rejection == null ? 0 : Mth.clamp(rejection.getAmplifier() + 1, 1, 3);
        }

        RejectionVisualState state = STATES.get(player.getUUID());
        return state == null ? 0 : state.rejectionLevel();
    }

    public static boolean isInFugue(AbstractClientPlayer player) {
        if (player == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;

        if (player == localPlayer) {
            return player.hasEffect(ModEffects.CYBERPSYCHOSIS_FUGUE);
        }

        RejectionVisualState state = STATES.get(player.getUUID());
        return state != null && state.fugue();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }

    private record RejectionVisualState(int rejectionLevel, boolean fugue) {
    }
}