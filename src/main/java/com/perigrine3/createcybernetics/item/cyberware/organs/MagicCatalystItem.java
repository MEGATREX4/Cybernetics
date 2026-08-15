package com.perigrine3.createcybernetics.item.cyberware.organs;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MagicCatalystItem extends Item implements ICyberwareItem {

    private final int humanityCost;

    private static final int ENERGY_PER_TICK = 100;

    public MagicCatalystItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost)
                    .withStyle(ChatFormatting.GOLD));

            tooltip.add(Component.translatable("tooltip.createcybernetics.organsupgrades_magiccatalyst.energy").withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    @Override
    public Map<Item, Integer> getAdditionalAnvilRepairMaterials(ItemStack cyberwareStack) {
        return Map.of(
                Items.NETHER_STAR, 750,
                Items.GHAST_TEAR, 300,
                Items.REDSTONE_BLOCK, 500,
                Items.REDSTONE, 200,
                Items.LAPIS_BLOCK, 500,
                Items.LAPIS_LAZULI, 200,
                ModItems.COMPONENT_WIRING.get(), 100,
                ModItems.COMPONENT_PLATING.get(), 100,
                ModItems.COMPONENT_MESH.get(), 100
        );
    }

    @Override
    public int getEnergyGeneratedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return ENERGY_PER_TICK;
    }

    @Override
    public int getEnergyCapacity(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 0;
    }

    @Override
    public boolean acceptsGeneratedEnergy(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return false;
    }

    @Override
    public boolean acceptsChargerEnergy(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return false;
    }

    @Override
    public int getChargerEnergyReceivePerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 0;
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.ORGANS, CyberwareSlot.HEART);
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
    public int maxStacksPerSlotType(ItemStack stack, CyberwareSlot slotType) {
        return 1;
    }

    @Override
    public Set<Item> incompatibleCyberware(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModItems.ORGANSUPGRADES_MAGICCATALYST.get(), ModItems.ORGANSUPGRADES_DUALISTICCONVERTER.get());
    }

    @Override
    public void onInstalled(LivingEntity entity) {}

    @Override
    public void onRemoved(LivingEntity entity) {}

    @Override
    public void onTick(LivingEntity entity) {}
}