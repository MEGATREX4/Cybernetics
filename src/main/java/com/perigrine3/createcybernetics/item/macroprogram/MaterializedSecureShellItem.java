package com.perigrine3.createcybernetics.item.macroprogram;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;

public class MaterializedSecureShellItem extends PickaxeItem {

    public MaterializedSecureShellItem(Properties properties) {
        super(Tiers.NETHERITE, properties.attributes(PickaxeItem.createAttributes(Tiers.NETHERITE, 1.0F, -2.8F)));
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}