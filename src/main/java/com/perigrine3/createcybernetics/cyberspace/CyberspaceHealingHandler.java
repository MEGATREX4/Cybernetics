package com.perigrine3.createcybernetics.cyberspace;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CyberspaceHealingHandler {
    private static final int PASSIVE_REGEN_DELAY = 200;
    private static final int PASSIVE_REGEN_INTERVAL = 50;
    private static final float PASSIVE_REGEN_AMOUNT = 1.0F;

    private static final Map<UUID, Long> LAST_DAMAGE_TICK = new HashMap<>();
    private static final Map<UUID, Long> NEXT_REGEN_TICK = new HashMap<>();

    private static boolean allowingCyberspaceHealing = false;

    private CyberspaceHealingHandler() {}

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInCyberspace(player)) return;
        if (allowingCyberspaceHealing) return;

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInCyberspace(player)) return;
        if (event.getNewDamage() <= 0.0F) return;

        long gameTime = player.level().getGameTime();

        LAST_DAMAGE_TICK.put(player.getUUID(), gameTime);
        NEXT_REGEN_TICK.put(player.getUUID(), gameTime + PASSIVE_REGEN_DELAY);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID playerId = player.getUUID();

        if (!isInCyberspace(player)) {
            LAST_DAMAGE_TICK.remove(playerId);
            NEXT_REGEN_TICK.remove(playerId);
            return;
        }

        if (!player.isAlive()) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

        long gameTime = player.level().getGameTime();

        if (!NEXT_REGEN_TICK.containsKey(playerId)) {
            LAST_DAMAGE_TICK.put(playerId, gameTime);
            NEXT_REGEN_TICK.put(playerId, gameTime + PASSIVE_REGEN_DELAY);
            return;
        }

        long nextRegenTick = NEXT_REGEN_TICK.get(playerId);

        if (gameTime < nextRegenTick) return;

        heal(player, PASSIVE_REGEN_AMOUNT);

        NEXT_REGEN_TICK.put(playerId, gameTime + PASSIVE_REGEN_INTERVAL);
    }

    public static void heal(ServerPlayer player, float amount) {
        if (player == null) return;
        if (amount <= 0.0F) return;
        if (!isInCyberspace(player)) {
            player.heal(amount);
            return;
        }

        boolean previous = allowingCyberspaceHealing;
        allowingCyberspaceHealing = true;

        try {
            player.heal(amount);
        } finally {
            allowingCyberspaceHealing = previous;
        }
    }

    public static boolean isInCyberspace(ServerPlayer player) {
        return player.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL);
    }
}