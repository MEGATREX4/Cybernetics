package com.perigrine3.createcybernetics.common.macroprogram;

import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.macroprogram.MaterializedMacroprogramArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MacroprogramArmorEquipmentHandler {
    private static final int NORMAL_INVENTORY_SLOT_COUNT = 36;

    private MacroprogramArmorEquipmentHandler() {}

    public static boolean prepareActivation(ServerPlayer player) {
        if (hasActiveArmorProgram(player)) return true;

        List<ItemStack> displaced = new ArrayList<>();

        collectDisplaced(player, EquipmentSlot.HEAD, displaced);
        collectDisplaced(player, EquipmentSlot.CHEST, displaced);
        collectDisplaced(player, EquipmentSlot.LEGS, displaced);
        collectDisplaced(player, EquipmentSlot.FEET, displaced);

        if (countEmptyInventorySlots(player) < displaced.size()) return false;

        for (ItemStack stack : displaced) {
            moveToEmptyInventorySlot(player, stack);
        }

        clearNonRuntimeSlot(player, EquipmentSlot.HEAD);
        clearNonRuntimeSlot(player, EquipmentSlot.CHEST);
        clearNonRuntimeSlot(player, EquipmentSlot.LEGS);
        clearNonRuntimeSlot(player, EquipmentSlot.FEET);

        return true;
    }

    public static void tick(ServerPlayer player) {
        removeLooseRuntimeArmor(player);

        ArmorSet set = getActiveArmorSet(player);

        if (set == null) {
            clearRuntimeArmor(player);
            return;
        }

        equip(player, EquipmentSlot.HEAD, set.helmet());
        equip(player, EquipmentSlot.CHEST, set.chestplate());
        equip(player, EquipmentSlot.LEGS, set.leggings());
        equip(player, EquipmentSlot.FEET, set.boots());

        player.inventoryMenu.broadcastChanges();
    }

    public static void clear(ServerPlayer player) {
        clearRuntimeArmor(player);
        removeLooseRuntimeArmor(player);

        if (player.containerMenu != null && isRuntimeArmor(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }

        player.inventoryMenu.broadcastChanges();
    }

    private static ArmorSet getActiveArmorSet(ServerPlayer player) {
        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.ADMINISTRATOR_ACCESS_ARMOR.id())) {
            return new ArmorSet(
                    ModItems.MACROPROGRAM_ADMINISTRATOR_ACCESS_ARMOR_HELMET.get(),
                    ModItems.MACROPROGRAM_ADMINISTRATOR_ACCESS_ARMOR_CHESTPLATE.get(),
                    ModItems.MACROPROGRAM_ADMINISTRATOR_ACCESS_ARMOR_LEGGINGS.get(),
                    ModItems.MACROPROGRAM_ADMINISTRATOR_ACCESS_ARMOR_BOOTS.get()
            );
        }

        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.ADVANCED_ARMOR.id())) {
            return new ArmorSet(
                    ModItems.MACROPROGRAM_ADVANCED_ARMOR_HELMET.get(),
                    ModItems.MACROPROGRAM_ADVANCED_ARMOR_CHESTPLATE.get(),
                    ModItems.MACROPROGRAM_ADVANCED_ARMOR_LEGGINGS.get(),
                    ModItems.MACROPROGRAM_ADVANCED_ARMOR_BOOTS.get()
            );
        }

        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.INTERMEDIATE_ARMOR.id())) {
            return new ArmorSet(
                    ModItems.MACROPROGRAM_INTERMEDIATE_ARMOR_HELMET.get(),
                    ModItems.MACROPROGRAM_INTERMEDIATE_ARMOR_CHESTPLATE.get(),
                    ModItems.MACROPROGRAM_INTERMEDIATE_ARMOR_LEGGINGS.get(),
                    ModItems.MACROPROGRAM_INTERMEDIATE_ARMOR_BOOTS.get()
            );
        }

        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.BASIC_ARMOR.id())) {
            return new ArmorSet(
                    ModItems.MACROPROGRAM_BASIC_ARMOR_HELMET.get(),
                    ModItems.MACROPROGRAM_BASIC_ARMOR_CHESTPLATE.get(),
                    ModItems.MACROPROGRAM_BASIC_ARMOR_LEGGINGS.get(),
                    ModItems.MACROPROGRAM_BASIC_ARMOR_BOOTS.get()
            );
        }

        return null;
    }

    private static boolean hasActiveArmorProgram(ServerPlayer player) {
        return MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.BASIC_ARMOR.id())
                || MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.INTERMEDIATE_ARMOR.id())
                || MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.ADVANCED_ARMOR.id())
                || MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.ADMINISTRATOR_ACCESS_ARMOR.id());
    }

    private static void collectDisplaced(ServerPlayer player, EquipmentSlot slot, List<ItemStack> displaced) {
        ItemStack stack = player.getItemBySlot(slot);

        if (!stack.isEmpty() && !isRuntimeArmor(stack)) {
            displaced.add(stack.copy());
        }
    }

    private static void clearNonRuntimeSlot(ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);

        if (!stack.isEmpty() && !isRuntimeArmor(stack)) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static int countEmptyInventorySlots(ServerPlayer player) {
        int count = 0;

        for (int i = 0; i < NORMAL_INVENTORY_SLOT_COUNT; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                count++;
            }
        }

        return count;
    }

    private static void moveToEmptyInventorySlot(ServerPlayer player, ItemStack stack) {
        for (int i = 0; i < NORMAL_INVENTORY_SLOT_COUNT; i++) {
            if (!player.getInventory().getItem(i).isEmpty()) continue;

            player.getInventory().setItem(i, stack);
            return;
        }
    }

    private static void removeLooseRuntimeArmor(ServerPlayer player) {
        for (int i = 0; i < NORMAL_INVENTORY_SLOT_COUNT; i++) {
            if (isRuntimeArmor(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        if (isRuntimeArmor(player.getOffhandItem())) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        if (player.containerMenu != null && isRuntimeArmor(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static void clearRuntimeArmor(ServerPlayer player) {
        clearRuntimeArmorSlot(player, EquipmentSlot.HEAD);
        clearRuntimeArmorSlot(player, EquipmentSlot.CHEST);
        clearRuntimeArmorSlot(player, EquipmentSlot.LEGS);
        clearRuntimeArmorSlot(player, EquipmentSlot.FEET);
    }

    private static void clearRuntimeArmorSlot(ServerPlayer player, EquipmentSlot slot) {
        if (isRuntimeArmor(player.getItemBySlot(slot))) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void equip(ServerPlayer player, EquipmentSlot slot, Item item) {
        ItemStack current = player.getItemBySlot(slot);

        if (current.is(item)) return;

        if (!current.isEmpty() && !isRuntimeArmor(current)) {
            if (!player.getInventory().add(current.copy())) {
                player.drop(current.copy(), false);
            }
        }

        player.setItemSlot(slot, new ItemStack(item));
    }

    private static boolean isRuntimeArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MaterializedMacroprogramArmorItem;
    }

    private record ArmorSet(Item helmet, Item chestplate, Item leggings, Item boots) {}
}