package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.component.MicroprogramLibrary;
import com.perigrine3.createcybernetics.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MicroprogramDataShardItem extends DataShardItem {

    public MicroprogramDataShardItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.createcybernetics.data_shard_microprograms.desc"));
        tooltip.add(Component.translatable("item.createcybernetics.data_shard_microprograms.desc2"));

        MicroprogramLibrary library = getLibrary(stack);

        tooltip.add(Component.translatable(
                "item.createcybernetics.data_shard_microprograms.storage",
                library.size(),
                MicroprogramLibrary.CAPACITY
        ).withStyle(ChatFormatting.GREEN));
    }

    public static MicroprogramLibrary getLibrary(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return MicroprogramLibrary.EMPTY;
        }

        return stack.getOrDefault(ModDataComponents.MICROPROGRAM_LIBRARY.get(), MicroprogramLibrary.EMPTY);
    }

    public static int getStoredCount(ItemStack stack) {
        return getLibrary(stack).size();
    }

    public static int getCapacity(ItemStack stack) {
        return MicroprogramLibrary.CAPACITY;
    }

    public static boolean containsMicroprogram(ItemStack stack, ResourceLocation id) {
        return getLibrary(stack).contains(id);
    }

    public static boolean canStoreMicroprogram(ItemStack stack, ResourceLocation id) {
        return getLibrary(stack).canAdd(id);
    }

    public static boolean addMicroprogram(ItemStack stack, ResourceLocation id) {
        if (stack == null || stack.isEmpty() || id == null) {
            return false;
        }

        MicroprogramLibrary current = getLibrary(stack);

        if (!current.canAdd(id)) {
            return false;
        }

        stack.set(ModDataComponents.MICROPROGRAM_LIBRARY.get(), current.add(id));
        return true;
    }

    public static boolean removeMicroprogram(ItemStack stack, ResourceLocation id) {
        if (stack == null || stack.isEmpty() || id == null) {
            return false;
        }

        MicroprogramLibrary current = getLibrary(stack);

        if (!current.contains(id)) {
            return false;
        }

        stack.set(ModDataComponents.MICROPROGRAM_LIBRARY.get(), current.remove(id));
        return true;
    }

    public static void clearMicroprograms(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        stack.set(ModDataComponents.MICROPROGRAM_LIBRARY.get(), MicroprogramLibrary.EMPTY);
    }

    public boolean isDyeable(ItemStack stack) {
        return true;
    }
}