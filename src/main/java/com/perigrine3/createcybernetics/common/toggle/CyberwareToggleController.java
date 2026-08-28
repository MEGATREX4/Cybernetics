package com.perigrine3.createcybernetics.common.toggle;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class CyberwareToggleController {
    private CyberwareToggleController() {
    }

    private static final Set<ResourceLocation> FUGUE_FORCE_TOGGLE_EXCLUDED = Set.of(
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "eyeupgrades_zoom")
    );

    private static String key(ResourceLocation itemId) {
        return "cc_toggle_" + itemId.getNamespace() + "_" + itemId.getPath();
    }

    public static boolean isActive(ServerPlayer player, ResourceLocation itemId) {
        CompoundTag p = player.getPersistentData();
        String k = key(itemId);
        return !p.contains(k) || p.getBoolean(k);
    }

    public static boolean setActive(ServerPlayer player, ResourceLocation itemId, boolean active) {
        CompoundTag p = player.getPersistentData();
        String k = key(itemId);
        boolean before = !p.contains(k) || p.getBoolean(k);

        if (before == active) {
            return before;
        }

        p.putBoolean(k, active);
        return before;
    }

    public static boolean toggle(ServerPlayer player, ResourceLocation itemId) {
        boolean now = !isActive(player, itemId);
        setActive(player, itemId, now);
        return now;
    }

    public static boolean hasToggleableInstalled(ServerPlayer player, ResourceLocation itemId) {
        if (!player.hasData(ModAttachments.CYBERWARE)) {
            return false;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) {
            return false;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return false;
        }

        for (var entry : data.getAll().entrySet()) {
            var arr = entry.getValue();
            if (arr == null) {
                continue;
            }

            for (var cw : arr) {
                if (cw == null) {
                    continue;
                }

                ItemStack stack = cw.getItem();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                if (stack.getItem() == item && stack.is(ModTags.Items.TOGGLEABLE_CYBERWARE)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Map<ResourceLocation, ItemStack> collectToggleables(ServerPlayer player) {
        Map<ResourceLocation, ItemStack> out = new LinkedHashMap<>();

        if (!player.hasData(ModAttachments.CYBERWARE)) {
            return out;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) {
            return out;
        }

        for (var entry : data.getAll().entrySet()) {
            var arr = entry.getValue();
            if (arr == null) {
                continue;
            }

            for (var cw : arr) {
                if (cw == null) {
                    continue;
                }

                ItemStack stack = cw.getItem();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                if (!stack.is(ModTags.Items.TOGGLEABLE_CYBERWARE)) {
                    continue;
                }

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id == null) {
                    continue;
                }

                out.putIfAbsent(id, stack.copy());
            }
        }

        return out;
    }

    public static void forceAllToggleablesInactiveForCyberspace(ServerPlayer player) {
        if (player == null || !player.hasData(ModAttachments.CYBERWARE)) {
            return;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) {
            return;
        }

        boolean changed = false;

        for (Map.Entry<CyberwareSlot, InstalledCyberware[]> entry : data.getAll().entrySet()) {
            CyberwareSlot slot = entry.getKey();
            InstalledCyberware[] arr = entry.getValue();

            if (slot == null || arr == null) {
                continue;
            }

            for (int i = 0; i < arr.length; i++) {
                InstalledCyberware cw = arr[i];
                if (cw == null) {
                    continue;
                }

                ItemStack stack = cw.getItem();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                if (!stack.is(ModTags.Items.TOGGLEABLE_CYBERWARE)) {
                    continue;
                }

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id == null) {
                    continue;
                }

                if (data.isEnabled(slot, i)) {
                    data.setEnabled(slot, i, false);
                    changed = true;
                }

                if (isActive(player, id)) {
                    setActive(player, id, false);
                    changed = true;
                }
            }
        }

        if (changed) {
            data.setDirty();
            player.syncData(ModAttachments.CYBERWARE);
        }
    }

    public static boolean isExcludedFromFugueForceToggle(ResourceLocation id) {
        return id != null && FUGUE_FORCE_TOGGLE_EXCLUDED.contains(id);
    }

    public static void forceAllToggleablesActiveForFugue(ServerPlayer player) {
        if (player == null || !player.hasData(ModAttachments.CYBERWARE)) {
            return;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) {
            return;
        }

        boolean changed = false;

        for (Map.Entry<CyberwareSlot, InstalledCyberware[]> entry : data.getAll().entrySet()) {
            CyberwareSlot slot = entry.getKey();
            InstalledCyberware[] arr = entry.getValue();

            if (slot == null || arr == null) {
                continue;
            }

            for (int i = 0; i < arr.length; i++) {
                InstalledCyberware cw = arr[i];
                if (cw == null) {
                    continue;
                }

                ItemStack stack = cw.getItem();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                if (!stack.is(ModTags.Items.TOGGLEABLE_CYBERWARE)) {
                    continue;
                }

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id == null) {
                    continue;
                }

                if (isExcludedFromFugueForceToggle(id)) {
                    continue;
                }

                if (!data.isEnabled(slot, i)) {
                    data.setEnabled(slot, i, true);
                    changed = true;
                }

                if (!isActive(player, id)) {
                    setActive(player, id, true);
                    changed = true;
                }
            }
        }

        if (changed) {
            data.setDirty();
        }
    }
}