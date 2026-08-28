package com.perigrine3.createcybernetics.item.cyberware.brain;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.CyberwareAttributeHelper;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
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
public class KerenzikovItem extends Item implements ICyberwareItem {

    private static final float DODGE_CHANCE = 0.20F;
    private static final int DODGE_COOLDOWN_TICKS = 10;
    private static final float DODGE_PUSH = 0.65F;

    private static final String NBT_ACTIVE_TICK = "createcybernetics_kerenzikov_active_tick";
    private static final String NBT_LAST_DODGE_TICK = "createcybernetics_kerenzikov_last_dodge_tick";

    private static final String NBT_STIM_ACTIVE_TICK = "cc_stim_active_tick";

    private final int humanityCost;

    public KerenzikovItem(Properties props, int humanityCost) {
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
        return Set.of(ModItems.BONEUPGRADES_SANDEVISTAN.get(), ModItems.BRAINUPGRADES_BERSERK.get());
    }

    @Override
    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 10;
    }

    @Override
    public void onInstalled(LivingEntity entity) {
        CyberwareAttributeHelper.applyModifier(entity, "kerenzikov_ram");
        CyberwareAttributeHelper.applyModifier(entity, "kerenzikov_ram_regen");
    }

    @Override
    public void onRemoved(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        CyberwareAttributeHelper.removeModifier(entity, "kerenzikov_ram");
        CyberwareAttributeHelper.removeModifier(entity, "kerenzikov_ram_regen");

        CompoundTag pd = entity.getPersistentData();
        pd.remove(NBT_ACTIVE_TICK);
        pd.remove(NBT_LAST_DODGE_TICK);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        player.getPersistentData().putLong(NBT_ACTIVE_TICK, player.level().getGameTime());
    }

    public static boolean isActive(Player player) {
        if (player.level().isClientSide) {
            return false;
        }

        CompoundTag pd = player.getPersistentData();
        if (!pd.contains(NBT_ACTIVE_TICK)) {
            return false;
        }

        long now = player.level().getGameTime();
        long activeTick = pd.getLong(NBT_ACTIVE_TICK);

        return (now - activeTick) <= 1L;
    }

    private static boolean isStimActive(Player player) {
        CompoundTag pd = player.getPersistentData();
        if (!pd.contains(NBT_STIM_ACTIVE_TICK)) {
            return false;
        }

        long now = player.level().getGameTime();
        long activeTick = pd.getLong(NBT_STIM_ACTIVE_TICK);

        return (now - activeTick) <= 1L;
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

        if (!isActive(player)) {
            return;
        }

        if (!canDodge(event.getSource())) {
            return;
        }

        if (isStimActive(player)) {
            return;
        }

        CompoundTag pd = player.getPersistentData();
        long now = player.level().getGameTime();

        if (pd.contains(NBT_LAST_DODGE_TICK)) {
            long lastDodge = pd.getLong(NBT_LAST_DODGE_TICK);

            if ((now - lastDodge) < DODGE_COOLDOWN_TICKS) {
                return;
            }
        }

        if (player.getRandom().nextFloat() >= DODGE_CHANCE) {
            return;
        }

        event.setCanceled(true);
        pd.putLong(NBT_LAST_DODGE_TICK, now);

        doDodge(player, event.getSource().getEntity());
    }

    private static boolean canDodge(DamageSource source) {
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return false;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FREEZING)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }

        return true;
    }

    private static void doDodge(Player player, Entity attacker) {
        double yawRad = Math.toRadians(player.getYRot());
        double lx = -Math.sin(yawRad);

        double sx = Math.cos(yawRad);
        double sz = -lx;

        if (attacker != null) {
            double ax = attacker.getX() - player.getX();
            double az = attacker.getZ() - player.getZ();
            double len = Math.sqrt(ax * ax + az * az);

            if (len > 0.0001) {
                ax /= len;
                az /= len;

                sx = -az;
                sz = ax;

                if (player.getRandom().nextBoolean()) {
                    sx = -sx;
                    sz = -sz;
                }
            }
        }

        player.push(sx * DODGE_PUSH, 0.02, sz * DODGE_PUSH);
        player.hurtMarked = true;
        player.resetFallDistance();
    }
}