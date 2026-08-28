package com.perigrine3.createcybernetics.network.handler;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutionContext;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutionResult;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutor;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackProgramData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.cyberware.brain.CyberdeckItem;
import com.perigrine3.createcybernetics.item.cyberware.brain.ICEProtocolItem;
import com.perigrine3.createcybernetics.network.payload.CastCyberdeckQuickhackPayload;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class CastCyberdeckQuickhackHandler {
    private static final int CAST_COOLDOWN_TICKS = 200;

    private CastCyberdeckQuickhackHandler() {}

    public static void handle(CastCyberdeckQuickhackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer caster)) return;
            if (!caster.hasData(ModAttachments.CYBERWARE)) return;

            PlayerCyberwareData casterData = caster.getData(ModAttachments.CYBERWARE);
            if (casterData == null) return;

            Item cyberdeckItem = ModItems.BRAINUPGRADES_CYBERDECK.get();

            if (!casterData.hasSpecificItem(cyberdeckItem, CyberwareSlot.BRAIN)) return;
            if (caster.getCooldowns().isOnCooldown(cyberdeckItem)) return;

            int slot = payload.cyberdeckSlot();

            if (slot < 0 || slot >= PlayerCyberwareData.CYBERDECK_QUICKHACK_SLOT_COUNT) return;

            ItemStack quickhack = casterData.getCyberdeckStack(slot);

            if (quickhack.isEmpty()) return;
            if (!quickhack.is(ModTags.Items.QUICKHACK_SHARDS)) return;
            if (!QuickhackProgramData.hasProgram(quickhack)) return;

            Entity entity = caster.level().getEntity(payload.targetEntityId());

            if (!(entity instanceof LivingEntity physicalTarget)) return;
            if (!physicalTarget.isAlive()) return;
            if (physicalTarget == caster) return;

            String programName = QuickhackProgramData.getName(quickhack);
            Component quickhackName = programName.isBlank() ? Component.literal("Quickhack") : Component.literal(programName + ".exe");

            QuickhackExecutionContext executionContext = QuickhackExecutionContext.create(caster, physicalTarget, quickhack);

            if (executionContext == null) {
                sendCasterFailureMessage(caster, quickhackName, Component.translatable("message.createcybernetics.quickhack.execution.no_cyberware_interface"));
                return;
            }

            LivingEntity target = executionContext.target();

            if (target == caster) return;

            int energyCost = CyberdeckItem.QUICKHACK_ENERGY_COST;

            if (casterData.getEnergyStored() < energyCost) {
                caster.displayClientMessage(Component.translatable("message.createcybernetics.quickhack.insufficient_energy").withStyle(ChatFormatting.RED), true);
                return;
            }

            int ramCost = QuickhackProgramData.getRamCost(quickhack);
            int oldRam = casterData.getCyberdeckRam();

            if (!casterData.tryConsumeCyberdeckRam(caster, ramCost)) {
                if (casterData.getCyberdeckRam() != oldRam) {
                    ModAttachments.syncCyberware(caster);
                }

                caster.displayClientMessage(Component.translatable("message.createcybernetics.quickhack.insufficient_ram", ramCost).withStyle(ChatFormatting.RED), true);
                return;
            }

            if (!casterData.tryConsumeEnergy(caster, energyCost)) {
                return;
            }

            ModAttachments.syncCyberware(caster);

            caster.getCooldowns().addCooldown(cyberdeckItem, CAST_COOLDOWN_TICKS);

            if (ICEProtocolItem.negatesQuickhack(target)) {
                sendCasterIceMessage(caster, quickhackName);
                sendTargetIceMessage(target, quickhackName);
                return;
            }

            QuickhackExecutionResult result = QuickhackExecutor.execute(executionContext);

            if (!result.success()) {
                sendCasterFailureMessage(caster, quickhackName, result.failureMessage());
                return;
            }

            for (Component output : result.casterOutput()) {
                caster.displayClientMessage(output, false);
            }

            sendCasterSuccessMessage(caster, quickhackName);

            if (result.notifyTarget()) {
                sendTargetSuccessMessage(target, quickhackName);
            }
        });
    }

    private static void sendCasterSuccessMessage(ServerPlayer caster, Component quickhackName) {
        caster.displayClientMessage(Component.translatable("message.createcybernetics.quickhack.caster.success", quickhackName).withStyle(ChatFormatting.DARK_GREEN), true);
    }

    private static void sendCasterFailureMessage(ServerPlayer caster, Component quickhackName, Component reason) {
        caster.displayClientMessage(Component.translatable("message.createcybernetics.quickhack.caster.failure", quickhackName).withStyle(ChatFormatting.DARK_RED), true);

        if (reason != null) {
            caster.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), false);
        }
    }

    private static void sendCasterIceMessage(ServerPlayer caster, Component quickhackName) {
        caster.displayClientMessage(Component.translatable("message.createcybernetics.quickhack.caster.ice_blocked", quickhackName).withStyle(ChatFormatting.DARK_RED), true);
    }

    private static void sendTargetSuccessMessage(LivingEntity target, Component quickhackName) {
        if (!(target instanceof ServerPlayer targetPlayer)) return;

        targetPlayer.displayClientMessage(Component.translatable("message.createcybernetics.quickhack.target.success", quickhackName).withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    private static void sendTargetIceMessage(LivingEntity target, Component quickhackName) {
        if (!(target instanceof ServerPlayer targetPlayer)) return;

        targetPlayer.displayClientMessage(Component.translatable("message.createcybernetics.quickhack.target.ice_blocked", quickhackName).withStyle(ChatFormatting.DARK_GREEN), true);
    }
}