package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.quickhack.MicroprogramDefinition;
import com.perigrine3.createcybernetics.common.quickhack.MicroprogramRegistry;
import com.perigrine3.createcybernetics.component.MicroprogramLibrary;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public class MicroprogramCreativeShardItem extends DataShardItem {

    public MicroprogramCreativeShardItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.createcybernetics.data_shard_microprograms.desc"));
        tooltip.add(Component.translatable("item.createcybernetics.data_shard_microprograms.desc2"));

        tooltip.add(Component.translatable(
                "item.createcybernetics.data_shard_microprograms_creative.storage",
                getStoredCount(stack)
        ).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static MicroprogramLibrary createFullLibrary() {
        List<ResourceLocation> microprograms = new ArrayList<>();

        for (MicroprogramDefinition definition : MicroprogramRegistry.getAll()) {
            microprograms.add(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, definition.id()));
        }

        return new MicroprogramLibrary(microprograms);
    }

    public static MicroprogramLibrary getLibrary(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return MicroprogramLibrary.EMPTY;
        }

        return createFullLibrary();
    }

    public static int getStoredCount(ItemStack stack) {
        return getLibrary(stack).size();
    }

    public static int getCapacity(ItemStack stack) {
        return MicroprogramRegistry.getAll().size();
    }

    public static boolean containsMicroprogram(ItemStack stack, ResourceLocation id) {
        return id != null && getLibrary(stack).contains(id);
    }

    public static boolean canStoreMicroprogram(ItemStack stack, ResourceLocation id) {
        return false;
    }

    public static boolean addMicroprogram(ItemStack stack, ResourceLocation id) {
        return false;
    }

    public static boolean removeMicroprogram(ItemStack stack, ResourceLocation id) {
        return false;
    }

    public static void clearMicroprograms(ItemStack stack) {
    }

    public boolean isDyeable(ItemStack stack) {
        return true;
    }
}