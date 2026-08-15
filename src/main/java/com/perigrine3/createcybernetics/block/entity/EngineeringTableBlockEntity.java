package com.perigrine3.createcybernetics.block.entity;

import com.perigrine3.createcybernetics.recipe.EngineeringDeconstructRecipe;
import com.perigrine3.createcybernetics.recipe.EngineeringDeconstructRecipeInput;
import com.perigrine3.createcybernetics.recipe.ModRecipes;
import com.perigrine3.createcybernetics.screen.custom.crafting.EngineeringTableMenu;
import com.perigrine3.createcybernetics.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class EngineeringTableBlockEntity extends BlockEntity implements MenuProvider {

    public static final int GRID_SIZE = 25;

    public static final int DECONSTRUCT_INPUT_SIZE = 1;
    public static final int DECONSTRUCT_OUTPUT_SIZE = 6;

    private final ItemStackHandler crafting = new ItemStackHandler(GRID_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private boolean deconProcessing = false;

    private final ItemStackHandler deconstructInput = new ItemStackHandler(DECONSTRUCT_INPUT_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();

            if (level != null && !level.isClientSide()) {
                if (!deconProcessing && level instanceof ServerLevel serverLevel) {
                    tryInstantDeconstruct(serverLevel);
                }

                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final ItemStackHandler deconstructOutputs = new ItemStackHandler(DECONSTRUCT_OUTPUT_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();

            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public EngineeringTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENGINEERING_TABLE_BLOCKENTITY.get(), pos, state);
    }

    public ItemStackHandler getCrafting() {
        return crafting;
    }

    public ItemStackHandler getDeconstructInput() {
        return deconstructInput;
    }

    public ItemStackHandler getDeconstructOutputs() {
        return deconstructOutputs;
    }

    public boolean outputsAreEmpty() {
        for (int i = 0; i < DECONSTRUCT_OUTPUT_SIZE; i++) {
            if (!deconstructOutputs.getStackInSlot(i).isEmpty()) return false;
        }

        return true;
    }

    public boolean hasDeconstructRecipe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (level == null) return false;

        EngineeringDeconstructRecipeInput input = new EngineeringDeconstructRecipeInput(stack);

        return level.getRecipeManager().getRecipeFor(ModRecipes.ENGINEERING_DECONSTRUCT_TYPE.get(), input, level).isPresent();
    }

    private void tryInstantDeconstruct(ServerLevel level) {
        ItemStack inputStack = deconstructInput.getStackInSlot(0);

        if (inputStack.isEmpty()) return;
        if (!outputsAreEmpty()) return;

        EngineeringDeconstructRecipeInput recipeInput = new EngineeringDeconstructRecipeInput(inputStack);

        Optional<RecipeHolder<EngineeringDeconstructRecipe>> recipeHolder =
                level.getRecipeManager().getRecipeFor(ModRecipes.ENGINEERING_DECONSTRUCT_TYPE.get(), recipeInput, level);

        if (recipeHolder.isEmpty()) return;

        EngineeringDeconstructRecipe recipe = recipeHolder.get().value();

        deconProcessing = true;

        try {
            deconstructInput.setStackInSlot(0, ItemStack.EMPTY);

            for (int i = 0; i < DECONSTRUCT_OUTPUT_SIZE; i++) {
                ItemStack output = ItemStack.EMPTY;

                if (i < recipe.outputs().size()) {
                    ItemStack recipeOutput = recipe.outputs().get(i);

                    if (!recipeOutput.isEmpty()) {
                        output = recipeOutput.copy();

                        int maxCount = recipeOutput.getCount();
                        int rolledCount = maxCount <= 1 ? 1 : level.random.nextInt(maxCount) + 1;

                        output.setCount(rolledCount);
                    }
                }

                deconstructOutputs.setStackInSlot(i, output);
            }

            level.playSound(null, worldPosition, ModSounds.METAL_CRUSHING.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        } finally {
            deconProcessing = false;
        }
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(GRID_SIZE + DECONSTRUCT_INPUT_SIZE + DECONSTRUCT_OUTPUT_SIZE);

        for (int i = 0; i < GRID_SIZE; i++) {
            inv.setItem(i, crafting.getStackInSlot(i));
        }

        inv.setItem(GRID_SIZE, deconstructInput.getStackInSlot(0));

        for (int i = 0; i < DECONSTRUCT_OUTPUT_SIZE; i++) {
            inv.setItem(GRID_SIZE + 1 + i, deconstructOutputs.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("crafting", crafting.serializeNBT(registries));
        tag.put("deconstructInput", deconstructInput.serializeNBT(registries));
        tag.put("deconstructOutputs", deconstructOutputs.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crafting.deserializeNBT(registries, tag.getCompound("crafting"));
        deconstructInput.deserializeNBT(registries, tag.getCompound("deconstructInput"));
        deconstructOutputs.deserializeNBT(registries, tag.getCompound("deconstructOutputs"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.createcybernetics.engineering_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EngineeringTableMenu(id, inv, this.getBlockPos());
    }
}