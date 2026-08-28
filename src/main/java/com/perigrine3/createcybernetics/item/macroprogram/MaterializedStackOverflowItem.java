package com.perigrine3.createcybernetics.item.macroprogram;

import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class MaterializedStackOverflowItem extends SwordItem {
    private static final String NBT_COMBO_COUNT = "CreateCyberneticsStackOverflowCombo";
    private static final String NBT_LAST_HIT = "CreateCyberneticsStackOverflowLastHit";

    private static final int REQUIRED_HITS = 15;
    private static final int COMBO_TIMEOUT_TICKS = 500;

    private static final double WAVE_RADIUS = 6.0D;
    private static final double WAVE_VERTICAL_RADIUS = 2.5D;
    private static final double KNOCKBACK_STRENGTH = 1.5D;

    public MaterializedStackOverflowItem(Properties properties) {
        super(Tiers.NETHERITE, properties.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5.0F, -2.8F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (!(attacker instanceof ServerPlayer player)) return result;
        if (!player.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) return result;

        long gameTime = player.level().getGameTime();
        long lastHit = player.getPersistentData().getLong(NBT_LAST_HIT);
        int combo = player.getPersistentData().getInt(NBT_COMBO_COUNT);

        if (lastHit <= 0L || gameTime - lastHit > COMBO_TIMEOUT_TICKS) {
            combo = 0;
        }

        combo++;
        player.getPersistentData().putLong(NBT_LAST_HIT, gameTime);

        if (combo >= REQUIRED_HITS) {
            triggerWave(player, target);
            resetCombo(player);
        } else {
            player.getPersistentData().putInt(NBT_COMBO_COUNT, combo);
        }

        return result;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    public static void resetCombo(Player player) {
        if (player == null) return;

        player.getPersistentData().remove(NBT_COMBO_COUNT);
        player.getPersistentData().remove(NBT_LAST_HIT);
    }

    private static void triggerWave(ServerPlayer player, LivingEntity struckTarget) {
        AABB area = player.getBoundingBox().inflate(WAVE_RADIUS, WAVE_VERTICAL_RADIUS, WAVE_RADIUS);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive());

        for (LivingEntity entity : targets) {
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);

            if (length < 0.001D) continue;

            double scale = KNOCKBACK_STRENGTH / length;
            entity.push(dx * scale, 0.35D, dz * scale);
            entity.hurtMarked = true;
        }

        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), struckTarget.getY() + struckTarget.getBbHeight() * 0.5D, player.getZ(), 60, WAVE_RADIUS * 0.45D, 0.4D, WAVE_RADIUS * 0.45D, 0.12D);
    }
}