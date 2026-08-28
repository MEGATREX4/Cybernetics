package com.perigrine3.createcybernetics.item.cyberware.brain;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.CyberwareAttributeHelper;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
public class BerserkItem extends Item implements ICyberwareItem {

    private static final float ACTIVATION_HEALTH_PERCENT = 0.10F;

    private static final int ACTIVE_DURATION_TICKS = 20 * 30;
    private static final int COOLDOWN_DURATION_TICKS = 20 * 60 * 4;

    private static final int ACTIVE_ENERGY_COST = 25;

    private static final int STRENGTH_AMPLIFIER = 1;
    private static final int SPEED_AMPLIFIER = 1;

    private static final String NBT_ACTIVE_UNTIL = "createcybernetics_berserk_active_until";
    private static final String NBT_COOLDOWN_UNTIL = "createcybernetics_berserk_cooldown_until";

    private final int humanityCost;

    public BerserkItem(Properties props, int humanityCost) {
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
        return Set.of(CyberwareSlot.BRAIN);
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
    public Set<TagKey<Item>> requiresCyberwareTags(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModTags.Items.BRAIN_ITEMS);
    }

    @Override
    public Set<Item> incompatibleCyberware(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModItems.BONEUPGRADES_SANDEVISTAN.get(), ModItems.BRAINUPGRADES_KERENZIKOV.get());
    }

    @Override
    public void onInstalled(LivingEntity entity) {
        CyberwareAttributeHelper.applyModifier(entity, "berserk_ram");
        CyberwareAttributeHelper.applyModifier(entity, "berserk_ram_regen");
    }

    @Override
    public void onRemoved(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        CyberwareAttributeHelper.removeModifier(entity, "berserk_ram");
        CyberwareAttributeHelper.removeModifier(entity, "berserk_ram_regen");

        CompoundTag pd = entity.getPersistentData();
        pd.remove(NBT_ACTIVE_UNTIL);
        pd.remove(NBT_COOLDOWN_UNTIL);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (isActive(player)) {
            PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
            data.extractEnergy(player, ACTIVE_ENERGY_COST);
            return;
        }

        if (!isEnabled(player)) {
            return;
        }

        if (isOnCooldown(player)) {
            return;
        }

        float activationHealth = player.getMaxHealth() * ACTIVATION_HEALTH_PERCENT;
        if (player.getHealth() <= activationHealth) {
            activate(player);
        }
    }

    public static boolean isActive(Player player) {
        return player.getPersistentData().getLong(NBT_ACTIVE_UNTIL) > player.level().getGameTime();
    }

    public static boolean isOnCooldown(Player player) {
        return player.getPersistentData().getLong(NBT_COOLDOWN_UNTIL) > player.level().getGameTime();
    }

    public static int getRemainingActiveTicks(Player player) {
        long remaining = player.getPersistentData().getLong(NBT_ACTIVE_UNTIL) - player.level().getGameTime();
        return (int) Math.max(0L, remaining);
    }

    public static int getRemainingCooldownTicks(Player player) {
        long remaining = player.getPersistentData().getLong(NBT_COOLDOWN_UNTIL) - player.level().getGameTime();
        return (int) Math.max(0L, remaining);
    }

    private static boolean isEnabled(Player player) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        for (int i = 0; i < CyberwareSlot.BRAIN.size; i++) {
            InstalledCyberware installed = data.get(CyberwareSlot.BRAIN, i);
            if (installed == null) {
                continue;
            }

            ItemStack stack = installed.getItem();
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (!stack.is(ModItems.BRAINUPGRADES_BERSERK.get())) {
                continue;
            }

            if (!data.isFunctional(player, CyberwareSlot.BRAIN, i)) {
                continue;
            }

            return data.isEnabled(CyberwareSlot.BRAIN, i);
        }

        return false;
    }

    private static void activate(Player player) {
        long now = player.level().getGameTime();
        CompoundTag pd = player.getPersistentData();

        pd.putLong(NBT_ACTIVE_UNTIL, now + ACTIVE_DURATION_TICKS);
        pd.putLong(NBT_COOLDOWN_UNTIL, now + ACTIVE_DURATION_TICKS + COOLDOWN_DURATION_TICKS);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ACTIVE_DURATION_TICKS, STRENGTH_AMPLIFIER, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ACTIVE_DURATION_TICKS, SPEED_AMPLIFIER, false, true, true));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (isActive(player)) {
            event.setCanceled(true);
            return;
        }

        if (!isEnabled(player)) {
            return;
        }

        if (isOnCooldown(player)) {
            return;
        }

        float activationHealth = player.getMaxHealth() * ACTIVATION_HEALTH_PERCENT;
        float projectedHealth = player.getHealth() - event.getAmount();

        if (projectedHealth > activationHealth) {
            return;
        }

        activate(player);
        event.setCanceled(true);
    }
}