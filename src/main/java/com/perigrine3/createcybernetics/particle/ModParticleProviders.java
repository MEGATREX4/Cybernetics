package com.perigrine3.createcybernetics.particle;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModParticleProviders {

    private ModParticleProviders() {}

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPARKING.get(), SparkingParticle.Provider::new);
    }
}