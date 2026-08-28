package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramExecutionManager;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramRegistry;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class FirewallDamageEvent {
    private static final String REFLECTED_DAMAGE_FLAG = "CreateCyberneticsFirewallReflecting";

    private FirewallDamageEvent() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        if (!(target instanceof ServerPlayer player)) return;
        if (!MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.FIREWALL.id())) return;
        if (!player.isUsingItem()) return;
        if (!player.getUseItem().is(ModItems.MATERIALIZED_FIREWALL.get())) return;
        if (player.getPersistentData().getBoolean(REFLECTED_DAMAGE_FLAG)) return;

        Entity sourceEntity = event.getSource().getEntity();

        if (!(sourceEntity instanceof LivingEntity attacker)) return;
        if (attacker == player) return;

        float reflectedDamage = event.getAmount();

        event.setCanceled(true);

        player.getPersistentData().putBoolean(REFLECTED_DAMAGE_FLAG, true);

        try {
            DamageSource reflectedSource = player.damageSources().playerAttack(player);
            attacker.hurt(reflectedSource, reflectedDamage);
        } finally {
            player.getPersistentData().remove(REFLECTED_DAMAGE_FLAG);
        }
    }
}