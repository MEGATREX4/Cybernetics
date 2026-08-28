package com.perigrine3.createcybernetics.common.quickhack;

import com.perigrine3.createcybernetics.api.CyberwareDegradationCause;
import com.perigrine3.createcybernetics.api.ICyberwareData;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.EntityCyberwareData;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.ModMobAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.durability.CriticalOrganDamageController;
import com.perigrine3.createcybernetics.common.durability.CyberwareDurabilityManager;
import com.perigrine3.createcybernetics.entity.custom.JackedInPlayerBodyEntity;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record QuickhackExecutionContext(ServerPlayer caster, LivingEntity target, LivingEntity physicalTarget, ICyberwareData targetData, ItemStack quickhack, String programName) {

    public static QuickhackExecutionContext create(ServerPlayer caster, LivingEntity target, ItemStack quickhack) {
        if (caster == null || target == null || quickhack == null || quickhack.isEmpty()) return null;

        LivingEntity executionTarget = target;

        if (target instanceof JackedInPlayerBodyEntity body) {
            ServerPlayer owner = body.getOwner();

            if (owner == null) return null;
            if (!owner.isAlive()) return null;
            if (!owner.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) return null;

            executionTarget = owner;
        }

        ICyberwareData targetData = getCyberwareData(executionTarget);
        if (targetData == null) return null;

        return new QuickhackExecutionContext(caster, executionTarget, target, targetData, quickhack, QuickhackProgramData.getName(quickhack));
    }

    public boolean isProxyTarget() {
        return physicalTarget != target;
    }

    public boolean isEnabled(QuickhackCyberwareRef ref) {
        if (ref == null) return true;

        if (targetData instanceof PlayerCyberwareData playerData) {
            return playerData.isEnabled(ref.slot(), ref.index());
        }

        if (targetData instanceof EntityCyberwareData entityData) {
            return entityData.isEnabled(ref.slot(), ref.index());
        }

        return true;
    }

    public void setEnabled(QuickhackCyberwareRef ref, boolean enabled) {
        if (ref == null) return;

        if (targetData instanceof PlayerCyberwareData playerData) {
            playerData.setEnabled(ref.slot(), ref.index(), enabled);
        } else if (targetData instanceof EntityCyberwareData entityData) {
            entityData.setEnabled(ref.slot(), ref.index(), enabled);
        }
    }

    public void setPowered(QuickhackCyberwareRef ref, boolean powered) {
        if (ref == null || ref.installed() == null) return;

        ref.installed().setPowered(powered);
        targetData.setDirty();
    }

    public int extractEnergy(int amount) {
        if (amount <= 0) return 0;

        if (targetData instanceof PlayerCyberwareData playerData && target instanceof Player player) {
            return playerData.extractEnergy(player, amount);
        }

        if (targetData instanceof EntityCyberwareData entityData) {
            return entityData.extractEnergy(amount);
        }

        return 0;
    }

    public int damageCyberware(QuickhackCyberwareRef ref, int amount) {
        if (ref == null || ref.installed() == null || amount <= 0) return 0;

        InstalledCyberware installed = ref.installed();

        if (targetData instanceof PlayerCyberwareData playerData) {
            return CyberwareDurabilityManager.damage(target, playerData, installed, CyberwareDegradationCause.GENERAL_DAMAGE, amount);
        }

        if (!CyberwareDurabilityManager.usesDurability(installed)) return 0;

        ItemStack stack = installed.getItem();
        if (stack == null || stack.isEmpty()) return 0;
        if (!(stack.getItem() instanceof ICyberwareItem cyberwareItem)) return 0;
        if (!cyberwareItem.canDegradeFrom(target, stack, ref.slot(), CyberwareDegradationCause.GENERAL_DAMAGE)) return 0;

        float multiplier = Math.max(0.0F, cyberwareItem.getDegradationMultiplier(target, stack, ref.slot(), CyberwareDegradationCause.GENERAL_DAMAGE));
        int modifiedAmount = Math.max(0, Math.round(amount * multiplier));
        modifiedAmount = cyberwareItem.modifyDegradationAmount(target, stack, ref.slot(), CyberwareDegradationCause.GENERAL_DAMAGE, modifiedAmount);
        if (modifiedAmount <= 0) return 0;

        int previousDurability = installed.getDurability();
        int damaged = installed.damageDurability(modifiedAmount);
        int currentDurability = installed.getDurability();
        if (damaged <= 0) return 0;

        cyberwareItem.onDurabilityDamaged(target, stack, ref.slot(), previousDurability, currentDurability, CyberwareDegradationCause.GENERAL_DAMAGE);
        CriticalOrganDamageController.onDurabilityDamaged(target, installed, previousDurability, currentDurability);

        if (previousDurability > 0 && currentDurability <= 0) {
            cyberwareItem.onDurabilityBroken(target, stack, ref.slot(), CyberwareDegradationCause.GENERAL_DAMAGE);
        }

        targetData.setDirty();
        return damaged;
    }

    public void sync() {
        targetData.setDirty();

        if (target instanceof ServerPlayer player) {
            player.syncData(ModAttachments.CYBERWARE);
            return;
        }

        target.syncData(ModMobAttachments.CYBERENTITY_CYBERWARE);
    }

    private static ICyberwareData getCyberwareData(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!player.hasData(ModAttachments.CYBERWARE)) return null;
            return player.getData(ModAttachments.CYBERWARE);
        }

        if (!entity.hasData(ModMobAttachments.CYBERENTITY_CYBERWARE)) return null;
        return entity.getData(ModMobAttachments.CYBERENTITY_CYBERWARE);
    }
}