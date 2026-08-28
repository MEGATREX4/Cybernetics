package com.perigrine3.createcybernetics.item.macroprogram;

import com.perigrine3.createcybernetics.item.cyberware.brain.ICEProtocolItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MaterializedICEBreakerItem extends Item {
    private static final float ICE_BREAK_CHANCE = 0.25F;

    public MaterializedICEBreakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide) return true;
        if (!ICEProtocolItem.hasICE(target)) return true;
        if (ICEProtocolItem.isICEBroken(target)) return true;

        if (attacker.getRandom().nextFloat() < ICE_BREAK_CHANCE) {
            ICEProtocolItem.breakICE(target);

            if (attacker instanceof ServerPlayer player) {
                player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 45, 0.45D, 0.65D, 0.45D, 0.15D);
            }
        }

        return true;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}