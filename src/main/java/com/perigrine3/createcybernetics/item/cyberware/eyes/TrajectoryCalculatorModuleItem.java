package com.perigrine3.createcybernetics.item.cyberware.eyes;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrajectoryCalculatorModuleItem extends Item implements ICyberwareItem {
    private final int humanityCost;

    public TrajectoryCalculatorModuleItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost)
                    .withStyle(ChatFormatting.GOLD));

            tooltip.add(Component.translatable("tooltip.createcybernetics.eyeupgrades_trajectorycalculator.energy").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Map<Item, Integer> getAdditionalAnvilRepairMaterials(ItemStack cyberwareStack) {
        return Map.of(
                ModItems.COMPONENT_WIRING.get(), 250,
                ModItems.COMPONENT_SSD.get(), 500,
                ModItems.COMPONENT_GRAPHICSCARD.get(), 350
        );
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public boolean meetsCyberwareRequirements(PlayerCyberwareData data, ItemStack installedStack, CyberwareSlot slot) {
        boolean hasMonovision = data.hasSpecificItem(ModItems.EYEUPGRADES_MONOVISION.get(), CyberwareSlot.EYES);
        boolean hasCybereyes = data.hasSpecificItem(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS1.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS2.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS3.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS4.get(), CyberwareSlot.EYES);
        boolean hasHudjack = data.hasSpecificItem(ModItems.EYEUPGRADES_HUDJACK.get(), CyberwareSlot.EYES);

        return hasMonovision || (hasCybereyes && hasHudjack);
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.EYES);
    }

    @Override
    public boolean replacesOrgan() {
        return false;
    }

    @Override
    public Set<CyberwareSlot> getReplacedOrgans() {
        return Set.of();
    }

    @Override
    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 3;
    }

    @Override
    public boolean requiresEnergyToFunction(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return true;
    }

    @Override
    public void onInstalled(LivingEntity entity) { }

    @Override
    public void onRemoved(LivingEntity entity) {
    }

    @Override
    public void onTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot, int index) {
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}