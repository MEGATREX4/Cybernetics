package com.perigrine3.createcybernetics.common.macroprogram;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MacroprogramArmorHandler {
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "macroprogram_armor_speed");
    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "macroprogram_armor_attack_damage");

    private static final double INTERMEDIATE_SPEED_BONUS = 0.10D;
    private static final double ADVANCED_SPEED_BONUS = 0.10D;
    private static final double ADMINISTRATOR_SPEED_BONUS = 0.25D;

    private static final double ADVANCED_ATTACK_DAMAGE_BONUS = 0.15D;
    private static final double ADMINISTRATOR_ATTACK_DAMAGE_BONUS = 0.35D;

    private MacroprogramArmorHandler() {}

    public static void tick(ServerPlayer player) {
        ArmorTier tier = getActiveTier(player);

        updateSpeed(player, tier);
        updateAttackDamage(player, tier);
    }

    public static float getDamageMultiplier(ServerPlayer player) {
        return switch (getActiveTier(player)) {
            case BASIC -> 0.75F;
            case INTERMEDIATE -> 0.55F;
            case ADVANCED -> 0.35F;
            case ADMINISTRATOR -> 0.15F;
            case NONE -> 1.0F;
        };
    }

    public static void clear(ServerPlayer player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_MODIFIER_ID);
    }

    private static ArmorTier getActiveTier(ServerPlayer player) {
        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.ADMINISTRATOR_ACCESS_ARMOR.id())) {
            return ArmorTier.ADMINISTRATOR;
        }

        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.ADVANCED_ARMOR.id())) {
            return ArmorTier.ADVANCED;
        }

        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.INTERMEDIATE_ARMOR.id())) {
            return ArmorTier.INTERMEDIATE;
        }

        if (MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.BASIC_ARMOR.id())) {
            return ArmorTier.BASIC;
        }

        return ArmorTier.NONE;
    }

    private static void updateSpeed(ServerPlayer player, ArmorTier tier) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;

        double bonus = switch (tier) {
            case INTERMEDIATE -> INTERMEDIATE_SPEED_BONUS;
            case ADVANCED -> ADVANCED_SPEED_BONUS;
            case ADMINISTRATOR -> ADMINISTRATOR_SPEED_BONUS;
            default -> 0.0D;
        };

        updateModifier(attribute, SPEED_MODIFIER_ID, bonus);
    }

    private static void updateAttackDamage(ServerPlayer player, ArmorTier tier) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute == null) return;

        double bonus = switch (tier) {
            case ADVANCED -> ADVANCED_ATTACK_DAMAGE_BONUS;
            case ADMINISTRATOR -> ADMINISTRATOR_ATTACK_DAMAGE_BONUS;
            default -> 0.0D;
        };

        updateModifier(attribute, ATTACK_DAMAGE_MODIFIER_ID, bonus);
    }

    private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        AttributeModifier existing = attribute.getModifier(id);

        if (amount <= 0.0D) {
            if (existing != null) {
                attribute.removeModifier(id);
            }

            return;
        }

        if (existing != null && existing.amount() == amount) {
            return;
        }

        if (existing != null) {
            attribute.removeModifier(id);
        }

        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute == null) return;

        if (attribute.hasModifier(id)) {
            attribute.removeModifier(id);
        }
    }

    private enum ArmorTier {
        NONE,
        BASIC,
        INTERMEDIATE,
        ADVANCED,
        ADMINISTRATOR
    }
}