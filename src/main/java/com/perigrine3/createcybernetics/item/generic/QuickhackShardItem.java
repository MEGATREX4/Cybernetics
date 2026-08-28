package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.common.quickhack.QuickhackProgramData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class QuickhackShardItem extends Item {

    public QuickhackShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!QuickhackProgramData.hasProgram(stack)) {
            tooltip.add(Component.translatable("item.createcybernetics.quickhack_shard.blank"));

            return;
        }

        String name = QuickhackProgramData.getName(stack);
        if (name.isBlank()) {name = "Unnamed_Quickhack";}

        tooltip.add(Component.translatable("item.createcybernetics.quickhack_shard.desc", name));

        tooltip.add(Component.literal(QuickhackProgramData.getProgramDisplay(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));

        tooltip.add(Component.translatable("item.createcybernetics.quickhack_shard.ram",
                QuickhackProgramData.getRamCost(stack)).withStyle(ChatFormatting.AQUA));
    }
}