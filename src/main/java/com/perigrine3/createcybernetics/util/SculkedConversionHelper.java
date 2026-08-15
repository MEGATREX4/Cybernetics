package com.perigrine3.createcybernetics.util;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class SculkedConversionHelper {

    private SculkedConversionHelper() {}

    public static boolean hasAnyQualifyingSculkOrgan(Player player) {
        if (player == null || !player.hasData(ModAttachments.CYBERWARE)) return false;

        return hasAnyQualifyingSculkOrgan(player.getData(ModAttachments.CYBERWARE));
    }

    public static boolean hasAnyQualifyingSculkOrgan(PlayerCyberwareData data) {
        if (data == null) return false;

        return data.hasSpecificItem(ModItems.BODYPART_SCULKBRAIN.get(), CyberwareSlot.BRAIN)
                || data.hasSpecificItem(ModItems.WETWARE_SCULKHEART.get(), CyberwareSlot.HEART)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKLIVER.get(), CyberwareSlot.ORGANS)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKINTESTINES.get(), CyberwareSlot.ORGANS)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKMUSCLE.get(), CyberwareSlot.MUSCLE)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKSKIN.get(), CyberwareSlot.SKIN)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKRIGHTARM.get(), CyberwareSlot.RARM)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKLEFTARM.get(), CyberwareSlot.LARM)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKRIGHTLEG.get(), CyberwareSlot.RLEG)
                || data.hasSpecificItem(ModItems.BODYPART_SCULKLEFTLEG.get(), CyberwareSlot.LLEG);
    }

    public static boolean hasFullDefaultSculkConversion(Player player) {
        if (player == null || !player.hasData(ModAttachments.CYBERWARE)) return false;

        return hasFullDefaultSculkConversion(player.getData(ModAttachments.CYBERWARE));
    }

    public static boolean hasFullDefaultSculkConversion(PlayerCyberwareData data) {
        if (data == null) return false;

        return data.hasSpecificItem(ModItems.BODYPART_SCULKBRAIN.get(), CyberwareSlot.BRAIN)
                && data.hasSpecificItem(ModItems.WETWARE_SCULKHEART.get(), CyberwareSlot.HEART)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKLIVER.get(), CyberwareSlot.ORGANS)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKINTESTINES.get(), CyberwareSlot.ORGANS)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKMUSCLE.get(), CyberwareSlot.MUSCLE)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKSKIN.get(), CyberwareSlot.SKIN)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKRIGHTARM.get(), CyberwareSlot.RARM)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKLEFTARM.get(), CyberwareSlot.LARM)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKRIGHTLEG.get(), CyberwareSlot.RLEG)
                && data.hasSpecificItem(ModItems.BODYPART_SCULKLEFTLEG.get(), CyberwareSlot.LLEG);
    }

    public static boolean isStandingOnSculk(LivingEntity entity) {
        if (entity == null) return false;

        AABB bounds = entity.getBoundingBox();

        BlockPos min = BlockPos.containing(bounds.minX + 0.001D, bounds.minY - 0.10D, bounds.minZ + 0.001D);
        BlockPos max = BlockPos.containing(bounds.maxX - 0.001D, bounds.minY + 0.01D, bounds.maxZ - 0.001D);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isSculkBlock(entity.level().getBlockState(pos))) {
                return true;
            }
        }

        return false;
    }

    public static boolean isTouchingSculk(LivingEntity entity) {
        if (entity == null) return false;

        AABB bounds = entity.getBoundingBox().inflate(0.05D);

        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isSculkBlock(entity.level().getBlockState(pos))) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSculkBlock(BlockState state) {
        return state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_VEIN)
                || state.is(Blocks.SCULK_CATALYST)
                || state.is(Blocks.SCULK_SENSOR)
                || state.is(Blocks.CALIBRATED_SCULK_SENSOR)
                || state.is(Blocks.SCULK_SHRIEKER);
    }
}