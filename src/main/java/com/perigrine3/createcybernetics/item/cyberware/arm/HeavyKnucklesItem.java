package com.perigrine3.createcybernetics.item.cyberware.arm;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.util.CyberwareAttributeHelper;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HeavyKnucklesItem extends Item implements ICyberwareItem {

    public static final float MIN_SMASH_FALL_DISTANCE = 1.5F;

    private static final float SMASH_LOW_DISTANCE = 3.0F;
    private static final float SMASH_MID_DISTANCE = 8.0F;

    private static final float SMASH_DAMAGE_PER_BLOCK_LOW = 4.0F;
    private static final float SMASH_DAMAGE_PER_BLOCK_MID = 2.0F;
    private static final float SMASH_DAMAGE_PER_BLOCK_HIGH = 1.0F;

    private static final float ONE_KNUCKLE_DAMAGE_MULTIPLIER = 0.50F;
    private static final float TWO_KNUCKLE_DAMAGE_MULTIPLIER = 1.25F;

    private static final double SMASH_KNOCKBACK_RADIUS = 3.5D;
    private static final double SMASH_KNOCKBACK_STRENGTH = 0.70D;
    private static final double SMASH_KNOCKBACK_Y = 0.35D;

    private static final String ATTACK_SPEED_MODIFIER = "heavyknuckles_attackspeed";
    private static final String ATTACK_DAMAGE_MODIFIER = "heavyknuckles_attackdamage";
    private static final String KNOCKBACK_MODIFIER = "heavyknuckles_knockback";

    private final int humanityCost;

    public HeavyKnucklesItem(Properties props, int humanityCost) {
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
        return Set.of(CyberwareSlot.RARM, CyberwareSlot.LARM);
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
        return switch (slot) {
            case RARM -> Set.of(ModTags.Items.RIGHTARM_REPLACEMENTS);
            case LARM -> Set.of(ModTags.Items.LEFTARM_REPLACEMENTS);
            default -> Set.of();
        };
    }

    @Override
    public void onInstalled(Player player) {

    }

    @Override
    public void onRemoved(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!canUseHeavyKnuckles(player)) {
            removeModifiers(player);
        }
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (canUseHeavyKnuckles(player)) {
            applyModifiers(player);
        } else {
            removeModifiers(player);
        }
    }

    public static boolean canUseHeavyKnuckles(Player player) {
        if (player == null) {
            return false;
        }

        if (!player.getMainHandItem().isEmpty()) {
            return false;
        }

        return countFunctionalHeavyKnuckles(player) > 0;
    }

    public static boolean canSmash(Player player) {
        if (!canUseHeavyKnuckles(player)) {
            return false;
        }

        if (player.onGround()) {
            return false;
        }

        if (player.isFallFlying()) {
            return false;
        }

        if (player.fallDistance <= MIN_SMASH_FALL_DISTANCE) {
            return false;
        }

        return player.getDeltaMovement().y < 0.0D;
    }

    public static boolean hasFunctionalHeavyKnuckles(Player player) {
        return countFunctionalHeavyKnuckles(player) > 0;
    }

    public static int countFunctionalHeavyKnuckles(Player player) {
        if (player == null || !player.hasData(ModAttachments.CYBERWARE)) {
            return 0;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        int count = 0;

        if (hasFunctionalHeavyKnucklesInSlot(player, data, CyberwareSlot.RARM)) {
            count++;
        }

        if (hasFunctionalHeavyKnucklesInSlot(player, data, CyberwareSlot.LARM)) {
            count++;
        }

        return count;
    }

    private static boolean hasFunctionalHeavyKnucklesInSlot(Player player, PlayerCyberwareData data, CyberwareSlot slot) {
        InstalledCyberware[] installed = data.getAll().get(slot);
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

            if (!(stack.getItem() instanceof HeavyKnucklesItem)) {
                continue;
            }

            if (!data.isFunctional(player, slot, i)) {
                continue;
            }

            return true;
        }

        return false;
    }

    private static float getSmashDamageMultiplier(Player player) {
        int knuckleCount = countFunctionalHeavyKnuckles(player);

        if (knuckleCount >= 2) {
            return TWO_KNUCKLE_DAMAGE_MULTIPLIER;
        }

        if (knuckleCount == 1) {
            return ONE_KNUCKLE_DAMAGE_MULTIPLIER;
        }

        return 0.0F;
    }

    private static void applyModifiers(Player player) {
        if (!CyberwareAttributeHelper.hasModifier(player, ATTACK_SPEED_MODIFIER)) {
            CyberwareAttributeHelper.applyModifier(player, ATTACK_SPEED_MODIFIER);
        }

        if (!CyberwareAttributeHelper.hasModifier(player, ATTACK_DAMAGE_MODIFIER)) {
            CyberwareAttributeHelper.applyModifier(player, ATTACK_DAMAGE_MODIFIER);
        }

        if (!CyberwareAttributeHelper.hasModifier(player, KNOCKBACK_MODIFIER)) {
            CyberwareAttributeHelper.applyModifier(player, KNOCKBACK_MODIFIER);
        }
    }

    private static void removeModifiers(Player player) {
        if (CyberwareAttributeHelper.hasModifier(player, ATTACK_SPEED_MODIFIER)) {
            CyberwareAttributeHelper.removeModifier(player, ATTACK_SPEED_MODIFIER);
        }

        if (CyberwareAttributeHelper.hasModifier(player, ATTACK_DAMAGE_MODIFIER)) {
            CyberwareAttributeHelper.removeModifier(player, ATTACK_DAMAGE_MODIFIER);
        }

        if (CyberwareAttributeHelper.hasModifier(player, KNOCKBACK_MODIFIER)) {
            CyberwareAttributeHelper.removeModifier(player, KNOCKBACK_MODIFIER);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        if (target.level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();

        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        if (source.getDirectEntity() != attacker) {
            return;
        }

        if (!canSmash(attacker)) {
            return;
        }

        float fallDistance = attacker.fallDistance;
        float damageMultiplier = getSmashDamageMultiplier(attacker);

        if (damageMultiplier <= 0.0F) {
            return;
        }

        float maceSmashDamage = calculateMaceSmashDamage(fallDistance);
        float bonusDamage = maceSmashDamage * damageMultiplier;

        if (bonusDamage <= 0.0F) {
            return;
        }

        event.setAmount(event.getAmount() + bonusDamage);

        if (attacker.level() instanceof ServerLevel serverLevel) {
            playSmashEffects(serverLevel, target, fallDistance);
            applySmashKnockback(serverLevel, attacker, target, fallDistance);
        }

        attacker.resetFallDistance();
    }

    private static float calculateMaceSmashDamage(float fallDistance) {
        if (fallDistance <= MIN_SMASH_FALL_DISTANCE) {
            return 0.0F;
        }

        if (fallDistance <= SMASH_LOW_DISTANCE) {
            return fallDistance * SMASH_DAMAGE_PER_BLOCK_LOW;
        }

        float damage = SMASH_LOW_DISTANCE * SMASH_DAMAGE_PER_BLOCK_LOW;

        if (fallDistance <= SMASH_MID_DISTANCE) {
            return damage + ((fallDistance - SMASH_LOW_DISTANCE) * SMASH_DAMAGE_PER_BLOCK_MID);
        }

        damage += (SMASH_MID_DISTANCE - SMASH_LOW_DISTANCE) * SMASH_DAMAGE_PER_BLOCK_MID;
        damage += (fallDistance - SMASH_MID_DISTANCE) * SMASH_DAMAGE_PER_BLOCK_HIGH;

        return damage;
    }

    private static void playSmashEffects(ServerLevel level, LivingEntity target, float fallDistance) {
        BlockPos groundPos = findImpactGround(level, target);
        BlockState groundState = level.getBlockState(groundPos);

        if (!groundState.isAir()) {
            BlockParticleOption blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, groundState);

            float strength = Math.min(fallDistance / 8.0F, 1.5F);

            int particleCount = 45 + Math.round(strength * 35.0F);
            double horizontalSpread = 0.75D + (strength * 0.45D);
            double verticalSpread = 0.12D + (strength * 0.08D);
            double speed = 0.20D + (strength * 0.10D);

            level.sendParticles(
                    blockParticle,
                    target.getX(),
                    groundPos.getY() + 1.05D,
                    target.getZ(),
                    particleCount,
                    horizontalSpread,
                    verticalSpread,
                    horizontalSpread,
                    speed
            );
        }

        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                target.onGround() ? SoundEvents.MACE_SMASH_GROUND : SoundEvents.MACE_SMASH_AIR,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static BlockPos findImpactGround(ServerLevel level, LivingEntity target) {
        BlockPos start = BlockPos.containing(
                target.getX(),
                target.getBoundingBox().minY - 0.05D,
                target.getZ()
        );

        for (int i = 0; i < 4; i++) {
            BlockPos check = start.below(i);
            BlockState state = level.getBlockState(check);

            if (!state.isAir() && !state.getCollisionShape(level, check).isEmpty()) {
                return check;
            }
        }

        return target.getOnPos();
    }

    private static void applySmashKnockback(ServerLevel level, Player attacker, LivingEntity target, float fallDistance) {
        AABB area = target.getBoundingBox().inflate(SMASH_KNOCKBACK_RADIUS);

        double fallStrength = Math.min(fallDistance / 10.0D, 1.5D);
        double strength = SMASH_KNOCKBACK_STRENGTH * (1.0D + fallStrength);

        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (nearby == attacker || nearby == target) {
                continue;
            }

            if (!nearby.isAlive()) {
                continue;
            }

            Vec3 direction = nearby.position().subtract(target.position());
            direction = new Vec3(direction.x, 0.0D, direction.z);

            double distance = direction.length();

            if (distance <= 0.0001D || distance > SMASH_KNOCKBACK_RADIUS) {
                continue;
            }

            direction = direction.normalize();

            double distanceScale = 1.0D - (distance / SMASH_KNOCKBACK_RADIUS);
            double finalStrength = strength * distanceScale;

            nearby.push(
                    direction.x * finalStrength,
                    SMASH_KNOCKBACK_Y * distanceScale,
                    direction.z * finalStrength
            );

            nearby.hurtMarked = true;
        }
    }
}