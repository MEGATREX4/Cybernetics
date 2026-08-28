package com.perigrine3.createcybernetics.item.cyberware.brain;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.EntityCyberwareData;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.ModMobAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.CyberwareAttributeHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ICEProtocolItem extends Item implements ICyberwareItem {
    private static final float QUICKHACK_NEGATION_CHANCE = 0.75F;
    public static final int QUICKHACK_INTERCEPTION_ENERGY_COST = 15;
    public static final int ICE_BREAK_DURATION_TICKS = 300;

    private static final String NBT_ICE_BROKEN_UNTIL = "CreateCyberneticsICEBrokenUntil";

    private final int humanityCost;

    public ICEProtocolItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.createcybernetics.brainupgrades_iceprotocol.energy").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Map<Item, Integer> getAdditionalAnvilRepairMaterials(ItemStack cyberwareStack) {
        return Map.of(
                Items.DIAMOND, 300,
                ModItems.COMPONENT_SSD.get(), 200
        );
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.BRAIN);
    }

    @Override
    public boolean replacesOrgan() {
        return false;
    }

    @Override
    public Set<CyberwareSlot> getReplacedOrgans() {
        return Set.of(CyberwareSlot.BRAIN);
    }

    @Override
    public void onInstalled(LivingEntity entity) {
        CyberwareAttributeHelper.applyModifier(entity, "ice_defense_ram");
        CyberwareAttributeHelper.applyModifier(entity, "ice_defense_ram_regen");
    }

    @Override
    public void onRemoved(LivingEntity entity) {
        CyberwareAttributeHelper.removeModifier(entity, "ice_defense_ram");
        CyberwareAttributeHelper.removeModifier(entity, "ice_defense_ram_regen");
    }

    @Override
    public void onTick(LivingEntity entity) {
    }

    public static boolean hasICE(LivingEntity entity) {
        if (entity == null) return false;

        if (entity instanceof ServerPlayer player) {
            if (!player.hasData(ModAttachments.CYBERWARE)) return false;

            PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
            if (data == null) return false;

            return hasIceProtocol(data.getAll().get(CyberwareSlot.BRAIN));
        }

        if (!entity.hasData(ModMobAttachments.CYBERENTITY_CYBERWARE)) return false;

        EntityCyberwareData data = entity.getData(ModMobAttachments.CYBERENTITY_CYBERWARE);
        if (data == null) return false;

        return hasIceProtocol(data.getAll().get(CyberwareSlot.BRAIN));
    }

    public static boolean isICEBroken(LivingEntity entity) {
        if (entity == null) return false;

        long brokenUntil = entity.getPersistentData().getLong(NBT_ICE_BROKEN_UNTIL);

        if (brokenUntil <= entity.level().getGameTime()) {
            if (brokenUntil != 0L) {
                entity.getPersistentData().remove(NBT_ICE_BROKEN_UNTIL);
            }

            return false;
        }

        return true;
    }

    public static void breakICE(LivingEntity entity) {
        if (entity == null) return;
        if (!hasICE(entity)) return;

        entity.getPersistentData().putLong(NBT_ICE_BROKEN_UNTIL, entity.level().getGameTime() + ICE_BREAK_DURATION_TICKS);

        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal("[ICE Protocol]: ICE BREACH DETECTED").withStyle(ChatFormatting.RED), true);
        }
    }

    public static boolean negatesQuickhack(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.level().isClientSide) return false;
        if (isICEBroken(entity)) return false;

        if (entity instanceof ServerPlayer serverPlayer) {
            return negatesPlayerQuickhack(serverPlayer);
        }

        return negatesEntityQuickhack(entity);
    }

    private static boolean negatesPlayerQuickhack(ServerPlayer player) {
        if (!player.hasData(ModAttachments.CYBERWARE)) return false;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;

        InstalledCyberware[] installed = data.getAll().get(CyberwareSlot.BRAIN);
        if (!hasIceProtocol(installed)) return false;

        if (!data.tryConsumeEnergy(player, QUICKHACK_INTERCEPTION_ENERGY_COST)) {
            return false;
        }

        data.setDirty();
        ModAttachments.syncCyberware(player);

        boolean blocked = player.getRandom().nextFloat() < QUICKHACK_NEGATION_CHANCE;

        if (blocked) {
            player.displayClientMessage(Component.literal("[ICE Protocol]: Quickhack Neutralized").withStyle(ChatFormatting.RED), true);
        }

        return blocked;
    }

    private static boolean negatesEntityQuickhack(LivingEntity entity) {
        if (!entity.hasData(ModMobAttachments.CYBERENTITY_CYBERWARE)) return false;

        EntityCyberwareData data = entity.getData(ModMobAttachments.CYBERENTITY_CYBERWARE);
        if (data == null) return false;

        InstalledCyberware[] installed = data.getAll().get(CyberwareSlot.BRAIN);
        if (!hasIceProtocol(installed)) return false;

        if (!data.tryConsumeEnergy(QUICKHACK_INTERCEPTION_ENERGY_COST)) {
            return false;
        }

        data.setDirty();

        return entity.getRandom().nextFloat() < QUICKHACK_NEGATION_CHANCE;
    }

    private static boolean hasIceProtocol(InstalledCyberware[] installed) {
        if (installed == null) return false;

        for (InstalledCyberware entry : installed) {
            if (entry == null) continue;

            ItemStack stack = entry.getItem();
            if (stack == null || stack.isEmpty()) continue;

            if (stack.getItem() instanceof ICEProtocolItem) {
                return true;
            }
        }

        return false;
    }
}