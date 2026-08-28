package com.perigrine3.createcybernetics.screen.custom.computer;

import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.block.entity.ComputerBlockEntity;
import com.perigrine3.createcybernetics.block.entity.ComputerTowerBlockEntity;
import com.perigrine3.createcybernetics.common.multiblock.ServerTowerMultiblock;
import com.perigrine3.createcybernetics.screen.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class ComputerMenu extends AbstractContainerMenu {
    private static final int TOWER_SLOT_COUNT = 24;

    private static final int HIDDEN_SLOT_X = -10_000;
    private static final int HIDDEN_SLOT_Y = -10_000;

    private final ComputerBlockEntity computerBlockEntity;
    private final BlockPos computerPos;
    private final boolean connected;
    private final boolean serverTowerConnected;

    public ComputerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getComputerBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public ComputerMenu(int containerId, Inventory playerInventory, ComputerBlockEntity computerBlockEntity) {
        super(ModMenuTypes.COMPUTER_MENU.get(), containerId);

        this.computerBlockEntity = computerBlockEntity;
        this.computerPos = computerBlockEntity.getBlockPos();

        Level level = playerInventory.player.level();
        BlockEntity blockEntityBelow = level.getBlockEntity(computerPos.below());

        this.serverTowerConnected = ServerTowerMultiblock.isComputerPosition(level, computerPos);

        Container towerContainer;

        if (blockEntityBelow instanceof ComputerTowerBlockEntity tower) {
            this.connected = true;

            tower.forceGenerateLootFromBlockNbt(playerInventory.player);
            towerContainer = tower;
        } else if (serverTowerConnected) {
            this.connected = true;
            towerContainer = new SimpleContainer(TOWER_SLOT_COUNT);
        } else {
            this.connected = false;
            towerContainer = new SimpleContainer(TOWER_SLOT_COUNT);
        }

        addHiddenTowerSlots(towerContainer);
    }

    private static ComputerBlockEntity getComputerBlockEntity(Inventory playerInventory, BlockPos computerPos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(computerPos);

        if (blockEntity instanceof ComputerBlockEntity computer) {
            return computer;
        }

        throw new IllegalStateException("Computer block entity was missing at " + computerPos);
    }

    private void addHiddenTowerSlots(Container towerContainer) {
        for (int slotIndex = 0; slotIndex < TOWER_SLOT_COUNT; slotIndex++) {
            addSlot(new Slot(towerContainer, slotIndex, HIDDEN_SLOT_X, HIDDEN_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isServerTowerConnected() {
        return serverTowerConnected;
    }

    public List<ItemStack> getTowerShards() {
        List<ItemStack> shards = new ArrayList<>();

        for (int slotIndex = 0; slotIndex < TOWER_SLOT_COUNT; slotIndex++) {
            Slot slot = slots.get(slotIndex);
            ItemStack stack = slot.getItem();

            if (!stack.isEmpty()) {
                shards.add(stack.copy());
            }
        }

        return shards;
    }

    public List<TowerShardEntry> getTowerShardEntries() {
        List<TowerShardEntry> entries = new ArrayList<>();

        for (int slotIndex = 0; slotIndex < TOWER_SLOT_COUNT; slotIndex++) {
            ItemStack stack = slots.get(slotIndex).getItem();

            if (!stack.isEmpty()) {
                entries.add(new TowerShardEntry(slotIndex, stack.copy()));
            }
        }

        return entries;
    }

    public ComputerBlockEntity getComputerBlockEntity() {
        return computerBlockEntity;
    }

    public String getComputerCode() {
        return computerBlockEntity.getComputerCode();
    }

    public BlockPos getComputerPos() {
        return computerPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (computerBlockEntity.getLevel() == null) {
            return false;
        }

        return stillValid(ContainerLevelAccess.create(computerBlockEntity.getLevel(), computerPos), player, ModBlocks.COMPUTER.get());
    }

    public record TowerShardEntry(int slot, ItemStack stack) {}
}