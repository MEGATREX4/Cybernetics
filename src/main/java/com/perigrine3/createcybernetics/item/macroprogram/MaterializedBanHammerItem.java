package com.perigrine3.createcybernetics.item.macroprogram;

import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramExecutionManager;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

public class MaterializedBanHammerItem extends Item {
    public MaterializedBanHammerItem(Properties properties) {
        super(properties.attributes(MaceItem.createAttributes()));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker instanceof ServerPlayer player) {
            MacroprogramExecutionManager.endProgram(player, MacroprogramRegistry.BANHAMMER.id(), true);
        }

        return true;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}