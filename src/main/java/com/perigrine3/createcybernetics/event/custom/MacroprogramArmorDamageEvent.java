package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramArmorHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MacroprogramArmorDamageEvent {
    private MacroprogramArmorDamageEvent() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        float multiplier = MacroprogramArmorHandler.getDamageMultiplier(player);

        if (multiplier >= 1.0F) return;

        event.setAmount(event.getAmount() * multiplier);
    }
}