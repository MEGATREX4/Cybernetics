package com.perigrine3.createcybernetics.item.macroprogram;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

public class MaterializedDataMinerItem extends PickaxeItem {

    public MaterializedDataMinerItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1.0F, -2.8F)));
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}