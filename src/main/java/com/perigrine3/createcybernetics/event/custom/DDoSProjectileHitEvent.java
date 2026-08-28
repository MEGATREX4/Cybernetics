package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.macroprogram.MaterializedDDoSItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DDoSProjectileHitEvent {
    private static final int DDOS_DURATION_TICKS = 900;
    private static final int MAX_AMPLIFIER = 2;

    private DDoSProjectileHitEvent() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();

        if (projectile.level().isClientSide) return;
        if (!projectile.getPersistentData().getBoolean(MaterializedDDoSItem.DDOS_PROJECTILE_FLAG)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) return;
        if (!(hitResult.getEntity() instanceof LivingEntity target)) return;

        MobEffectInstance existing = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        int amplifier = 0;

        if (existing != null) {
            amplifier = Math.min(MAX_AMPLIFIER, existing.getAmplifier() + 1);
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DDOS_DURATION_TICKS, amplifier, false, true, true));
    }
}