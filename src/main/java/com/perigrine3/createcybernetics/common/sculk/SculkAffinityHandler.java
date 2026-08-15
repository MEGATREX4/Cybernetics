package com.perigrine3.createcybernetics.common.sculk;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.util.SculkedConversionHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SculkAffinityHandler {
    private static final Map<UUID, Set<UUID>> PROVOKED_PLAYERS = new HashMap<>();

    private SculkAffinityHandler() {}

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Warden warden)) {
            return;
        }

        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) {
            return;
        }

        if (!SculkedConversionHelper.hasFullDefaultSculkConversion(player)) {
            return;
        }

        if (isProvoked(warden, player)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Warden warden)) {
            return;
        }

        Entity attacker = event.getSource().getEntity();

        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }

        if (!SculkedConversionHelper.hasFullDefaultSculkConversion(player)) {
            return;
        }

        PROVOKED_PLAYERS.computeIfAbsent(warden.getUUID(), uuid -> new HashSet<>()).add(player.getUUID());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Warden warden) {
            PROVOKED_PLAYERS.remove(warden.getUUID());
        }
    }

    private static boolean isProvoked(Warden warden, ServerPlayer player) {
        Set<UUID> players = PROVOKED_PLAYERS.get(warden.getUUID());

        return players != null && players.contains(player.getUUID());
    }
}