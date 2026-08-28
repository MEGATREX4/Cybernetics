package com.perigrine3.createcybernetics.item.macroprogram;

import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramExecutionManager;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MaterializedDDoSItem extends BowItem {
    public static final String DDOS_PROJECTILE_FLAG = "CreateCyberneticsDDoSProjectile";
    private static final String DDOS_SHOT_FLAG = "CreateCyberneticsDDoSShot";

    public MaterializedDDoSItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, LivingEntity target) {
        super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, angle, target);

        if (shooter.level().isClientSide) return;

        if (projectile instanceof AbstractArrow arrow) {
            arrow.getPersistentData().putBoolean(DDOS_PROJECTILE_FLAG, true);
        }

        if (shooter instanceof ServerPlayer player) {
            player.getPersistentData().putBoolean(DDOS_SHOT_FLAG, true);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            player.getPersistentData().remove(DDOS_SHOT_FLAG);
        }

        super.releaseUsing(stack, level, livingEntity, timeLeft);

        if (level.isClientSide) return;
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (!player.getPersistentData().getBoolean(DDOS_SHOT_FLAG)) return;

        player.getPersistentData().remove(DDOS_SHOT_FLAG);
        MacroprogramExecutionManager.endProgram(player, MacroprogramRegistry.DDOS.id(), true);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}