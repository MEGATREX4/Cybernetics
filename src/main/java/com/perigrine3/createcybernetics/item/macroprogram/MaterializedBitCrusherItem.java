package com.perigrine3.createcybernetics.item.macroprogram;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class MaterializedBitCrusherItem extends SwordItem {
    private static final int STUN_TICKS = 20;

    public MaterializedBitCrusherItem(Properties properties) {
        super(Tiers.NETHERITE, properties.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6.0F, -2.4F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, STUN_TICKS, 4, false, false, true));
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}