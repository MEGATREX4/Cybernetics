package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BanHammerDamageEvent {
    private BanHammerDamageEvent() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        if (!(attacker instanceof Player player)) return;
        if (!player.getMainHandItem().is(ModItems.MATERIALIZED_BANHAMMER.get())) return;

        event.setAmount(target.getHealth() * 0.5F);
    }
}