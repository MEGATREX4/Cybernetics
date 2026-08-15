package com.perigrine3.createcybernetics.common.cyberware;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CyberwareVisualInterface {

    private CyberwareVisualInterface() {}

    public static boolean hasWorkingVisualInterface(PlayerCyberwareData data) {
        InstalledLoc monovision = findInstalled(data, ModItems.EYEUPGRADES_MONOVISION.get(), CyberwareSlot.EYES);

        if (monovision != null) {
            return isEnabledIfToggleable(data, monovision);
        }

        boolean hasCybereyes = findInstalled(data, ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES) != null
                || findInstalled(data, ModItems.EYEUPGRADES_MULTIOPTICS1.get(), CyberwareSlot.EYES) != null
                || findInstalled(data, ModItems.EYEUPGRADES_MULTIOPTICS2.get(), CyberwareSlot.EYES) != null
                || findInstalled(data, ModItems.EYEUPGRADES_MULTIOPTICS3.get(), CyberwareSlot.EYES) != null
                || findInstalled(data, ModItems.EYEUPGRADES_MULTIOPTICS4.get(), CyberwareSlot.EYES) != null;

        if (!hasCybereyes) return false;

        InstalledLoc hudjack = findInstalled(data, ModItems.EYEUPGRADES_HUDJACK.get(), CyberwareSlot.EYES);

        return hudjack != null && isEnabledIfToggleable(data, hudjack);
    }

    public static boolean isInstalledAndEnabled(PlayerCyberwareData data, Item item, CyberwareSlot slot) {
        InstalledLoc installed = findInstalled(data, item, slot);
        return installed != null && isEnabledIfToggleable(data, installed);
    }

    private static boolean isEnabledIfToggleable(PlayerCyberwareData data, InstalledLoc loc) {
        if (loc.stack() == null || loc.stack().isEmpty()) return false;
        if (!loc.stack().is(ModTags.Items.TOGGLEABLE_CYBERWARE)) return true;
        return data.isEnabled(loc.slot(), loc.index());
    }

    private static InstalledLoc findInstalled(PlayerCyberwareData data, Item item, CyberwareSlot slot) {
        InstalledCyberware[] installed = data.getAll().get(slot);
        if (installed == null) return null;

        for (int index = 0; index < installed.length; index++) {
            InstalledCyberware cyberware = installed[index];
            if (cyberware == null) continue;

            ItemStack stack = cyberware.getItem();
            if (stack == null || stack.isEmpty()) continue;

            if (stack.getItem() == item) {
                return new InstalledLoc(slot, index, stack);
            }
        }

        return null;
    }

    private record InstalledLoc(CyberwareSlot slot, int index, ItemStack stack) {}
}