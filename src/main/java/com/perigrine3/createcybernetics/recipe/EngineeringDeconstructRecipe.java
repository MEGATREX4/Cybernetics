package com.perigrine3.createcybernetics.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public record EngineeringDeconstructRecipe(
        List<Ingredient> inputs,
        List<ItemStack> outputs
) implements Recipe<EngineeringDeconstructRecipeInput> {

    private static final int MAX_INPUTS = 2;
    private static final int MAX_OUTPUTS = 6;

    public EngineeringDeconstructRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    @Override
    public boolean matches(EngineeringDeconstructRecipeInput input, net.minecraft.world.level.Level level) {
        if (input.stack().isEmpty()) return false;

        for (Ingredient ingredient : inputs) {
            if (ingredient.test(input.stack())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ItemStack assemble(EngineeringDeconstructRecipeInput input, HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ENGINEERING_DECONSTRUCT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ENGINEERING_DECONSTRUCT_TYPE.get();
    }

    public List<ItemStack> copyOutputs() {
        return outputs.stream().map(ItemStack::copy).toList();
    }

    public static final class Serializer implements RecipeSerializer<EngineeringDeconstructRecipe> {

        private static final Codec<List<Ingredient>> INPUTS_CODEC =
                Codec.list(Ingredient.CODEC_NONEMPTY).flatXmap(Serializer::validateInputs, Serializer::validateInputs);

        private static final Codec<List<ItemStack>> OUTPUTS_CODEC =
                Codec.list(ItemStack.CODEC).flatXmap(Serializer::validateOutputs, Serializer::validateOutputs);

        private static DataResult<List<Ingredient>> validateInputs(List<Ingredient> inputs) {
            if (inputs.isEmpty()) {
                return DataResult.error(() -> "Engineering deconstruct recipes require at least 1 input.");
            }

            if (inputs.size() > MAX_INPUTS) {
                return DataResult.error(() -> "Engineering deconstruct recipes may have at most " + MAX_INPUTS + " inputs.");
            }

            return DataResult.success(inputs);
        }

        private static DataResult<List<ItemStack>> validateOutputs(List<ItemStack> outputs) {
            if (outputs.isEmpty()) {
                return DataResult.error(() -> "Engineering deconstruct recipes require at least 1 output.");
            }

            if (outputs.size() > MAX_OUTPUTS) {
                return DataResult.error(() -> "Engineering deconstruct recipes may have at most " + MAX_OUTPUTS + " outputs.");
            }

            for (ItemStack stack : outputs) {
                if (stack.isEmpty()) {
                    return DataResult.error(() -> "Engineering deconstruct recipe outputs may not contain empty ItemStacks.");
                }
            }

            return DataResult.success(outputs);
        }

        public static final MapCodec<EngineeringDeconstructRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                INPUTS_CODEC.fieldOf("inputs").forGetter(EngineeringDeconstructRecipe::inputs),
                OUTPUTS_CODEC.fieldOf("outputs").forGetter(EngineeringDeconstructRecipe::outputs)
        ).apply(instance, EngineeringDeconstructRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, EngineeringDeconstructRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public EngineeringDeconstructRecipe decode(RegistryFriendlyByteBuf buf) {
                        int inputCount = buf.readVarInt();

                        if (inputCount < 1 || inputCount > MAX_INPUTS) {
                            throw new IllegalArgumentException("Engineering deconstruct input count must be between 1 and " + MAX_INPUTS + ". Got: " + inputCount);
                        }

                        List<Ingredient> inputs = new ArrayList<>(inputCount);

                        for (int i = 0; i < inputCount; i++) {
                            inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                        }

                        int outputCount = buf.readVarInt();

                        if (outputCount < 1 || outputCount > MAX_OUTPUTS) {
                            throw new IllegalArgumentException("Engineering deconstruct output count must be between 1 and " + MAX_OUTPUTS + ". Got: " + outputCount);
                        }

                        List<ItemStack> outputs = new ArrayList<>(outputCount);

                        for (int i = 0; i < outputCount; i++) {
                            outputs.add(ItemStack.STREAM_CODEC.decode(buf));
                        }

                        return new EngineeringDeconstructRecipe(inputs, outputs);
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, EngineeringDeconstructRecipe recipe) {
                        buf.writeVarInt(recipe.inputs().size());

                        for (Ingredient input : recipe.inputs()) {
                            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, input);
                        }

                        buf.writeVarInt(recipe.outputs().size());

                        for (ItemStack output : recipe.outputs()) {
                            ItemStack.STREAM_CODEC.encode(buf, output);
                        }
                    }
                };

        @Override
        public MapCodec<EngineeringDeconstructRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EngineeringDeconstructRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}