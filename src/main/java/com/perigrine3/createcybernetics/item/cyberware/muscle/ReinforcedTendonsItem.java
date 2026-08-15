package com.perigrine3.createcybernetics.item.cyberware.muscle;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ReinforcedTendonsItem extends Item implements ICyberwareItem {

    private static final float DAMAGE_MULTIPLIER = 0.90F;

    private final int humanityCost;

    public ReinforcedTendonsItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.MUSCLE);
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
    public Set<Item> requiresCyberware(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModItems.MUSCLEUPGRADES_SYNTHMUSCLE.get());
    }

    @Override
    public void onInstalled(Player player) {

    }

    @Override
    public void onRemoved(Player player) {

    }

    @Override
    public void onTick(LivingEntity entity) {

    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (!hasFunctionalReinforcedTendons(player)) {
            return;
        }

        if (isCriticalDamage(event)) {
            return;
        }

        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
    }

    private static boolean hasFunctionalReinforcedTendons(Player player) {
        if (!player.hasData(ModAttachments.CYBERWARE)) {
            return false;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        InstalledCyberware[] installed = data.getAll().get(CyberwareSlot.MUSCLE);

        if (installed == null) {
            return false;
        }

        for (int i = 0; i < installed.length; i++) {
            InstalledCyberware cyberware = installed[i];

            if (cyberware == null) {
                continue;
            }

            ItemStack stack = cyberware.getItem();

            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem() instanceof ReinforcedTendonsItem)) {
                continue;
            }

            if (!data.isFunctional(player, CyberwareSlot.MUSCLE, i)) {
                continue;
            }

            return true;
        }

        return false;
    }

    private static boolean isCriticalDamage(LivingIncomingDamageEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();

        if (!(attackerEntity instanceof Player attacker)) {
            return false;
        }

        if (directEntity != attacker) {
            return false;
        }

        if (attacker.getAttackStrengthScale(0.5F) <= 0.9F) {
            return false;
        }

        if (attacker.fallDistance <= 0.0F) {
            return false;
        }

        if (attacker.onGround()) {
            return false;
        }

        if (attacker.onClimbable()) {
            return false;
        }

        if (attacker.isInWater()) {
            return false;
        }

        if (attacker.hasEffect(MobEffects.BLINDNESS)) {
            return false;
        }

        if (attacker.isPassenger()) {
            return false;
        }

        if (attacker.isSprinting()) {
            return false;
        }

        return true;
    }
}