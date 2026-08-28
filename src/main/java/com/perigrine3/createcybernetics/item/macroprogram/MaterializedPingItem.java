package com.perigrine3.createcybernetics.item.macroprogram;

import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramExecutionManager;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MaterializedPingItem extends BowItem {
    public static final String PING_PROJECTILE_FLAG = "CreateCyberneticsPingProjectile";

    public MaterializedPingItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, LivingEntity target) {
        super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, angle, target);

        if (!shooter.level().isClientSide) {
            projectile.getPersistentData().putBoolean(PING_PROJECTILE_FLAG, true);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        int useTicks = getUseDuration(stack, livingEntity) - timeLeft;
        float power = getPowerForTime(useTicks);

        super.releaseUsing(stack, level, livingEntity, timeLeft);

        if (level.isClientSide) return;
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (power < 0.1F) return;

        MacroprogramExecutionManager.endProgram(player, MacroprogramRegistry.PING.id(), true);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}