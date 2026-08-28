package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.entity.ModEntities;
import com.perigrine3.createcybernetics.entity.custom.JackedInPlayerBodyEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class JackedInPlayerBodyEntityEvents {
    private JackedInPlayerBodyEntityEvents() {}

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.JACKED_IN_PLAYER_BODY.get(), JackedInPlayerBodyEntity.createAttributes().build());
    }
}