package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.macroprogram.MaterializedPingItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.minecraft.world.phys.EntityHitResult;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PingProjectileHitEvent {
    private static final int PING_GLOW_DURATION_TICKS = 1200;

    private PingProjectileHitEvent() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();

        if (projectile.level().isClientSide) return;
        if (!projectile.getPersistentData().getBoolean(MaterializedPingItem.PING_PROJECTILE_FLAG)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) return;
        if (!(hitResult.getEntity() instanceof LivingEntity target)) return;

        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, PING_GLOW_DURATION_TICKS, 0, false, false, true));
    }
}