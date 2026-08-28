package com.perigrine3.createcybernetics.item.macroprogram;

import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MacroprogramItem extends Item {
    private final MacroprogramDefinition definition;

    public MacroprogramItem(Properties properties, MacroprogramDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    public MacroprogramDefinition getDefinition() {
        return definition;
    }

    @Override
    public Component getName(ItemStack stack) {
        return definition.getDisplayName();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("macroprogram.createcybernetics.category." + definition.category().name().toLowerCase()).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(definition.getDescription().copy().withStyle(ChatFormatting.GRAY));
    }
}