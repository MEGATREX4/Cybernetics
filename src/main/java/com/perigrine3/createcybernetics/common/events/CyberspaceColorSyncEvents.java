package com.perigrine3.createcybernetics.common.events;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.cyberspace.CyberspaceColorData;
import com.perigrine3.createcybernetics.network.payload.CyberspaceColorSyncS2CPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CyberspaceColorSyncEvents {

    private CyberspaceColorSyncEvents() {}

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;

        if (oldPlayer.getPersistentData().contains(CyberspaceColorData.NBT_COLOR)) {
            CyberspaceColorData.set(newPlayer, CyberspaceColorData.get(oldPlayer));
        }

        sendCurrentToSelfAndTrackers(newPlayer);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        sendCurrentToViewer(target, viewer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        sendCurrentToSelfAndTrackers(player);
    }

    private static void sendCurrentToSelfAndTrackers(ServerPlayer player) {
        CyberspaceColorSyncS2CPayload out = makePayload(player);

        PacketDistributor.sendToPlayer(player, out);
        PacketDistributor.sendToPlayersTrackingEntity(player, out);
    }

    private static void sendCurrentToViewer(ServerPlayer target, ServerPlayer viewer) {
        PacketDistributor.sendToPlayer(viewer, makePayload(target));
    }

    private static CyberspaceColorSyncS2CPayload makePayload(ServerPlayer player) {
        return new CyberspaceColorSyncS2CPayload(player.getUUID(), CyberspaceColorData.get(player));
    }
}