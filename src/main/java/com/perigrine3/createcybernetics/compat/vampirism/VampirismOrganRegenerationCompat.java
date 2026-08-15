package com.perigrine3.createcybernetics.compat.vampirism;

import com.perigrine3.createcybernetics.ConfigValues;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareDurabilityCategory;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.durability.CyberwareDurabilityManager;
import com.perigrine3.createcybernetics.common.surgery.DefaultOrgans;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VampirismOrganRegenerationCompat {

    private VampirismOrganRegenerationCompat() {}

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!VampirismCompat.isLoaded()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!VampirismCompat.isBloodRegenerationHeal(player, event.getAmount())) return;

        regenerateOrgans(player);
    }

    private static void regenerateOrgans(ServerPlayer player) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        Set<Item> removedItems = new HashSet<>();

        boolean changed = removeOrganReplacingCyberware(player, data, removedItems);

        if (!removedItems.isEmpty()) {
            changed |= removeDependentCyberware(player, data, removedItems);
        }

        changed |= restoreMissingDefaultOrgans(data);
        changed |= repairDefaultOrganDurability(player, data);

        if (!changed) return;

        data.recomputeHumanityBaseFromInstalled(player);
        data.clampEnergyToCapacity(player);
        data.setDirty();
    }

    private static boolean removeOrganReplacingCyberware(ServerPlayer player, PlayerCyberwareData data, Set<Item> removedItems) {
        boolean changed = false;

        for (CyberwareSlot slot : CyberwareSlot.values()) {
            InstalledCyberware[] installedCyberware = data.getAll().get(slot);
            if (installedCyberware == null) continue;

            for (int index = 0; index < installedCyberware.length; index++) {
                InstalledCyberware installed = installedCyberware[index];
                if (installed == null) continue;

                ItemStack stack = installed.getItem();
                if (stack == null || stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof ICyberwareItem cyberwareItem)) continue;

                CyberwareDurabilityCategory category = cyberwareItem.getDurabilityCategory(stack, slot);

                if (category == CyberwareDurabilityCategory.DEFAULT_ORGAN) continue;
                if (!cyberwareItem.replacesOrgan()) continue;
                if (!replacesDefaultOrgan(cyberwareItem)) continue;

                ItemStack droppedStack = stack.copy();

                data.ejectInventoryForRemovedCyberware(player, droppedStack);

                InstalledCyberware removed = data.remove(slot, index);
                if (removed == null) continue;

                Item removedItem = removed.getItem().getItem();

                if (removedItem != null) {
                    removedItems.add(removedItem);
                }

                cyberwareItem.onRemoved(player);
                player.drop(droppedStack, false);

                changed = true;
            }
        }

        return changed;
    }

    private static boolean removeDependentCyberware(ServerPlayer player, PlayerCyberwareData data, Set<Item> removedItems) {
        boolean anyRemoved = false;
        boolean changed;

        do {
            changed = false;

            for (CyberwareSlot slot : CyberwareSlot.values()) {
                InstalledCyberware[] installedCyberware = data.getAll().get(slot);
                if (installedCyberware == null) continue;

                for (int index = 0; index < installedCyberware.length; index++) {
                    InstalledCyberware installed = installedCyberware[index];
                    if (installed == null) continue;

                    ItemStack stack = installed.getItem();
                    if (stack == null || stack.isEmpty()) continue;
                    if (!(stack.getItem() instanceof ICyberwareItem cyberwareItem)) continue;

                    if (cyberwareItem.getDurabilityCategory(stack, slot) == CyberwareDurabilityCategory.DEFAULT_ORGAN) continue;

                    Set<Item> requiredItems = cyberwareItem.requiresCyberware(stack, slot);
                    Set<TagKey<Item>> requiredTags = cyberwareItem.requiresCyberwareTags(stack, slot);

                    boolean hasRequiredItems = requiredItems != null && !requiredItems.isEmpty();
                    boolean hasRequiredTags = requiredTags != null && !requiredTags.isEmpty();

                    if (!hasRequiredItems && !hasRequiredTags) continue;
                    if (!removedRequirementMatches(requiredItems, requiredTags, removedItems)) continue;

                    boolean itemsSatisfied = hasRequiredItems && hasAnyInstalledItem(data, requiredItems);
                    boolean tagsSatisfied = hasRequiredTags && hasAnyInstalledItemWithAnyTag(data, requiredTags);

                    if (itemsSatisfied || tagsSatisfied) continue;

                    ItemStack droppedStack = stack.copy();

                    data.ejectInventoryForRemovedCyberware(player, droppedStack);

                    InstalledCyberware removed = data.remove(slot, index);
                    if (removed == null) continue;

                    Item removedItem = removed.getItem().getItem();

                    if (removedItem != null) {
                        removedItems.add(removedItem);
                    }

                    cyberwareItem.onRemoved(player);
                    player.drop(droppedStack, false);

                    changed = true;
                    anyRemoved = true;
                }
            }
        } while (changed);

        return anyRemoved;
    }

    private static boolean removedRequirementMatches(Set<Item> requiredItems, Set<TagKey<Item>> requiredTags, Set<Item> removedItems) {
        if (requiredItems != null) {
            for (Item requiredItem : requiredItems) {
                if (requiredItem != null && removedItems.contains(requiredItem)) {
                    return true;
                }
            }
        }

        if (requiredTags != null) {
            for (TagKey<Item> requiredTag : requiredTags) {
                if (requiredTag != null && anyRemovedItemMatchesTag(removedItems, requiredTag)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasAnyInstalledItem(PlayerCyberwareData data, Set<Item> items) {
        if (items == null || items.isEmpty()) return true;

        for (InstalledCyberware[] installedCyberware : data.getAll().values()) {
            if (installedCyberware == null) continue;

            for (InstalledCyberware installed : installedCyberware) {
                if (installed == null) continue;

                ItemStack stack = installed.getItem();
                if (stack == null || stack.isEmpty()) continue;

                if (items.contains(stack.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasAnyInstalledItemWithAnyTag(PlayerCyberwareData data, Set<TagKey<Item>> tags) {
        if (tags == null || tags.isEmpty()) return true;

        for (InstalledCyberware[] installedCyberware : data.getAll().values()) {
            if (installedCyberware == null) continue;

            for (InstalledCyberware installed : installedCyberware) {
                if (installed == null) continue;

                ItemStack stack = installed.getItem();
                if (stack == null || stack.isEmpty()) continue;

                for (TagKey<Item> tag : tags) {
                    if (tag != null && stack.is(tag)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean anyRemovedItemMatchesTag(Set<Item> removedItems, TagKey<Item> tag) {
        if (removedItems == null || removedItems.isEmpty()) return false;
        if (tag == null) return false;

        for (Item item : removedItems) {
            if (item == null) continue;
            if (new ItemStack(item).is(tag)) return true;
        }

        return false;
    }

    private static boolean replacesDefaultOrgan(ICyberwareItem cyberwareItem) {
        for (CyberwareSlot replacedSlot : cyberwareItem.getReplacedOrgans()) {
            if (replacedSlot == null) continue;

            for (int index = 0; index < replacedSlot.size; index++) {
                ItemStack defaultOrgan = DefaultOrgans.get(replacedSlot, index);

                if (defaultOrgan != null && !defaultOrgan.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean restoreMissingDefaultOrgans(PlayerCyberwareData data) {
        boolean changed = false;

        for (CyberwareSlot slot : CyberwareSlot.values()) {
            InstalledCyberware[] installedCyberware = data.getAll().get(slot);
            if (installedCyberware == null) continue;

            for (int index = 0; index < installedCyberware.length; index++) {
                ItemStack defaultOrgan = DefaultOrgans.get(slot, index);
                if (defaultOrgan == null || defaultOrgan.isEmpty()) continue;

                InstalledCyberware current = installedCyberware[index];

                if (current != null) {
                    ItemStack currentStack = current.getItem();

                    if (currentStack != null && !currentStack.isEmpty()) {
                        continue;
                    }
                }

                ItemStack restoredStack = defaultOrgan.copy();

                int humanityCost = 0;

                if (restoredStack.getItem() instanceof ICyberwareItem cyberwareItem) {
                    humanityCost = cyberwareItem.getHumanityCost();
                }

                InstalledCyberware restored = new InstalledCyberware(restoredStack, slot, index, humanityCost);
                restored.setPowered(true);

                data.set(slot, index, restored);
                data.setEnabled(slot, index, true);

                changed = true;
            }
        }

        return changed;
    }

    private static boolean repairDefaultOrganDurability(ServerPlayer player, PlayerCyberwareData data) {
        boolean changed = false;
        int repairAmount = Math.max(0, ConfigValues.FOOD_DURABILITY_REPAIR_PER_NUTRITION);

        if (repairAmount <= 0) return false;

        for (CyberwareSlot slot : CyberwareSlot.values()) {
            InstalledCyberware[] installedCyberware = data.getAll().get(slot);
            if (installedCyberware == null) continue;

            for (InstalledCyberware installed : installedCyberware) {
                if (installed == null) continue;

                ItemStack stack = installed.getItem();
                if (stack == null || stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof ICyberwareItem cyberwareItem)) continue;
                if (cyberwareItem.getDurabilityCategory(stack, slot) != CyberwareDurabilityCategory.DEFAULT_ORGAN) continue;

                int repaired = CyberwareDurabilityManager.repair(player, data, installed, repairAmount);

                if (repaired > 0) {
                    changed = true;
                }
            }
        }

        return changed;
    }
}