package com.perigrine3.createcybernetics.screen.custom.cyberdeck;

import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.screen.ModMenuTypes;
import com.perigrine3.createcybernetics.util.ModTags;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CyberdeckMenu extends AbstractContainerMenu {
    public static final int QUICKHACK_SLOT_COUNT = PlayerCyberwareData.CYBERDECK_QUICKHACK_SLOT_COUNT;
    public static final int MICROPROGRAM_CHIP_SLOT = PlayerCyberwareData.CYBERDECK_MICROPROGRAM_CHIP_SLOT;
    public static final int MACROPROGRAM_SLOT_START = PlayerCyberwareData.CYBERDECK_MACROPROGRAM_SLOT_START;
    public static final int MACROPROGRAM_SLOT_COUNT = PlayerCyberwareData.CYBERDECK_MACROPROGRAM_SLOT_COUNT;
    public static final int CYBERDECK_SLOT_COUNT = PlayerCyberwareData.CYBERDECK_SLOT_COUNT;

    private static final int SLOT_SIZE = 18;

    private static final int QUICKHACK_Y = 49;
    private static final int QUICKHACK_X_0 = 20;

    private static final int MICROPROGRAM_CHIP_X = 134;
    private static final int MICROPROGRAM_CHIP_Y = 56;

    private static final int MACROPROGRAM_X_0 = 17;
    private static final int MACROPROGRAM_Y_0 = 79;
    private static final int MACROPROGRAM_COLUMNS = 5;

    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 120;

    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 178;

    private static final int PLAYER_INV_START = CYBERDECK_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final IItemHandler cyberdeckInventory;
    private final Player player;

    public CyberdeckMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new ItemStackHandler(PlayerCyberwareData.CYBERDECK_SLOT_COUNT));
    }

    public CyberdeckMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(PlayerCyberwareData.CYBERDECK_SLOT_COUNT));
    }

    public CyberdeckMenu(int containerId, Inventory playerInventory, IItemHandler cyberdeckInventory) {
        super(ModMenuTypes.CYBERDECK_MENU.get(), containerId);
        this.cyberdeckInventory = cyberdeckInventory;
        this.player = playerInventory.player;

        addQuickhackSlots();
        addMicroprogramChipSlot();
        addMacroprogramSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addQuickhackSlots() {
        for (int i = 0; i < QUICKHACK_SLOT_COUNT; i++) {
            int x = QUICKHACK_X_0 + i * SLOT_SIZE;
            addSlot(new QuickhackSlot(cyberdeckInventory, i, x, QUICKHACK_Y, player));
        }
    }

    private void addMicroprogramChipSlot() {
        addSlot(new MicroprogramChipSlot(cyberdeckInventory, MICROPROGRAM_CHIP_SLOT, MICROPROGRAM_CHIP_X, MICROPROGRAM_CHIP_Y, player));
    }

    private void addMacroprogramSlots() {
        for (int i = 0; i < MACROPROGRAM_SLOT_COUNT; i++) {
            int column = i % MACROPROGRAM_COLUMNS;
            int row = i / MACROPROGRAM_COLUMNS;

            int x = MACROPROGRAM_X_0 + column * SLOT_SIZE;
            int y = MACROPROGRAM_Y_0 + row * SLOT_SIZE;

            addSlot(new MacroprogramSlot(cyberdeckInventory, MACROPROGRAM_SLOT_START + i, x, y));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = column + row * 9 + 9;
                int x = PLAYER_INV_X + column * SLOT_SIZE;
                int y = PLAYER_INV_Y + row * SLOT_SIZE;
                addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            int x = HOTBAR_X + i * SLOT_SIZE;
            addSlot(new Slot(playerInventory, i, x, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copiedStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        boolean inCyberspace = isInCyberspace(player);

        if (inCyberspace && index >= 0 && index < QUICKHACK_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        if (inCyberspace && index == MICROPROGRAM_CHIP_SLOT) {
            return ItemStack.EMPTY;
        }

        if (!inCyberspace && index >= MACROPROGRAM_SLOT_START && index < MACROPROGRAM_SLOT_START + MACROPROGRAM_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        copiedStack = stackInSlot.copy();

        if (index < CYBERDECK_SLOT_COUNT) {
            if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stackInSlot.is(ModTags.Items.MICROPROGRAM_SHARDS)) {
                if (inCyberspace) return ItemStack.EMPTY;

                if (!moveItemStackTo(stackInSlot, MICROPROGRAM_CHIP_SLOT, MICROPROGRAM_CHIP_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stackInSlot.is(ModTags.Items.QUICKHACK_SHARDS)) {
                if (inCyberspace) return ItemStack.EMPTY;

                if (!moveItemStackTo(stackInSlot, 0, QUICKHACK_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stackInSlot.is(ModTags.Items.MACROPROGRAMS)) {
                if (!moveItemStackTo(stackInSlot, MACROPROGRAM_SLOT_START, MACROPROGRAM_SLOT_START + MACROPROGRAM_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == copiedStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return copiedStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static boolean isInCyberspace(Player player) {
        return player.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL);
    }

    private static class QuickhackSlot extends SlotItemHandler {
        private final Player player;

        public QuickhackSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, Player player) {
            super(itemHandler, index, xPosition, yPosition);
            this.player = player;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isInCyberspace(player) && stack.is(ModTags.Items.QUICKHACK_SHARDS);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isInCyberspace(player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class MicroprogramChipSlot extends SlotItemHandler {
        private final Player player;

        public MicroprogramChipSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, Player player) {
            super(itemHandler, index, xPosition, yPosition);
            this.player = player;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isInCyberspace(player) && stack.is(ModTags.Items.MICROPROGRAM_SHARDS);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isInCyberspace(player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class MacroprogramSlot extends SlotItemHandler {
        public MacroprogramSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModTags.Items.MACROPROGRAMS);
        }

        @Override
        public boolean mayPickup(Player player) {
            return isInCyberspace(player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}