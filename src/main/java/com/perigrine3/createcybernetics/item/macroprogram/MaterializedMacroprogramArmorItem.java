package com.perigrine3.createcybernetics.item.macroprogram;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class MaterializedMacroprogramArmorItem extends ArmorItem {
    public MaterializedMacroprogramArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}