package com.perigrine3.createcybernetics.particle;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE, CreateCybernetics.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARKING =
            PARTICLE_TYPES.register("sparking", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}