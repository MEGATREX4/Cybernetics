package com.perigrine3.createcybernetics.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record EngineeringDeconstructRecipeInput(ItemStack stack) implements RecipeInput {

    @Override
    public ItemStack getItem(int i) {
        return i == 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}