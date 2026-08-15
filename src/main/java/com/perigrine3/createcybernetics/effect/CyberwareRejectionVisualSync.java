package com.perigrine3.createcybernetics.effect;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.network.payload.CyberwareRejectionVisualPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CyberwareRejectionVisualSync {

    private static final Map<UUID, VisualState> LAST_SENT_STATES = new HashMap<>();

    private CyberwareRejectionVisualSync() {
    }

    public static void syncIfChanged(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        VisualState currentState = getCurrentState(player);
        VisualState previousState = LAST_SENT_STATES.get(player.getUUID());

        if (currentState.equals(previousState)) {
            return;
        }

        LAST_SENT_STATES.put(player.getUUID(), currentState);
        syncToTrackingPlayers(player, currentState);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) {
            return;
        }

        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        sendToPlayer(viewer, target, getCurrentState(target));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT_STATES.remove(event.getEntity().getUUID());
    }

    private static VisualState getCurrentState(ServerPlayer player) {
        CyberpsychosisSeverity severity = CyberpsychosisSeverity.fromPlayer(player);
        boolean fugue = player.hasEffect(ModEffects.CYBERPSYCHOSIS_FUGUE);

        return new VisualState(severity.id(), fugue);
    }

    private static void syncToTrackingPlayers(ServerPlayer player, VisualState state) {
        CyberwareRejectionVisualPayload payload = new CyberwareRejectionVisualPayload(player.getUUID(), state.rejectionLevel(), state.fugue());

        PacketDistributor.sendToPlayer(player, payload);
        PacketDistributor.sendToPlayersTrackingEntity(player, payload);
    }

    private static void sendToPlayer(ServerPlayer viewer, ServerPlayer target, VisualState state) {
        PacketDistributor.sendToPlayer(viewer, new CyberwareRejectionVisualPayload(target.getUUID(), state.rejectionLevel(), state.fugue()));
    }

    private record VisualState(int rejectionLevel, boolean fugue) {
    }
}