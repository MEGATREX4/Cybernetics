package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.recipe.EngineeringDeconstructRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EngineeringDeconstructRecipeCategory implements IRecipeCategory<RecipeHolder<EngineeringDeconstructRecipe>> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "engineering_deconstruct");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/engineering_gui.png");

    public static final RecipeType<RecipeHolder<EngineeringDeconstructRecipe>> ENGINEERING_DECONSTRUCT_RECIPE_TYPE =
            new RecipeType<>(UID, (Class<RecipeHolder<EngineeringDeconstructRecipe>>) (Class<?>) RecipeHolder.class);

    private final IDrawable background;
    private final IDrawable icon;

    public EngineeringDeconstructRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 14, 58, 40, 92);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ENGINEERING_TABLE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<EngineeringDeconstructRecipe>> getRecipeType() {
        return ENGINEERING_DECONSTRUCT_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.engineeringtable.deconstruct");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EngineeringDeconstructRecipe> holder, IFocusGroup focuses) {
        EngineeringDeconstructRecipe recipe = holder.value();

        List<ItemStack> inputStacks = new ArrayList<>();

        for (Ingredient input : recipe.inputs()) {
            for (ItemStack stack : input.getItems()) {
                inputStacks.add(stack.copy());
            }
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 14, 4)
                .addItemStacks(inputStacks);

        int startX = 5;
        int startY = 39;
        int spacing = 18;

        for (int i = 0; i < recipe.outputs().size(); i++) {
            int col = i % 2;
            int row = i / 2;

            builder.addSlot(RecipeIngredientRole.OUTPUT, startX + col * spacing, startY + row * spacing)
                    .addItemStack(recipe.outputs().get(i));
        }
    }
}