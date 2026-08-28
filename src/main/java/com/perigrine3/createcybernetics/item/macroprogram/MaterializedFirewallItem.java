package com.perigrine3.createcybernetics.item.macroprogram;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public class MaterializedFirewallItem extends ShieldItem {
    public MaterializedFirewallItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}