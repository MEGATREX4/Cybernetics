package com.perigrine3.createcybernetics.common.quickhack;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class QuickhackGameplayEvents {
    private static final int OVERCLOCK_ARM_LATENCY_TICKS = 5;
    private static final Map<UUID, Long> NEXT_ARM_INPUT_TICK = new HashMap<>();

    private QuickhackGameplayEvents() {}

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        CyberwareSlot armSlot = armSlot(player, InteractionHand.MAIN_HAND);

        if (QuickhackRuntimeManager.isArmInteractionDesynced(player, armSlot) && player.getRandom().nextBoolean()) {
            event.setCanceled(true);
            return;
        }

        if (shouldThrottleArmInput(player, armSlot)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (shouldCancelInteraction(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (shouldCancelInteraction(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (shouldCancelInteraction(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (shouldCancelInteraction(event.getEntity(), InteractionHand.MAIN_HAND)) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldCancelInteraction(Player player, InteractionHand hand) {
        CyberwareSlot armSlot = armSlot(player, hand);

        if (QuickhackRuntimeManager.isArmInteractionDesynced(player, armSlot) && player.getRandom().nextBoolean()) {
            return true;
        }

        return shouldThrottleArmInput(player, armSlot);
    }

    private static boolean shouldThrottleArmInput(Player player, CyberwareSlot armSlot) {
        if (!QuickhackRuntimeManager.hasOverclockedArms(player, armSlot)) return false;

        long now = player.level().getGameTime();
        long nextAllowed = NEXT_ARM_INPUT_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE);

        if (now >= nextAllowed) {
            NEXT_ARM_INPUT_TICK.put(player.getUUID(), now + OVERCLOCK_ARM_LATENCY_TICKS);
            return false;
        }

        return true;
    }

    private static CyberwareSlot armSlot(Player player, InteractionHand hand) {
        boolean mainArmRight = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        boolean usingRightArm = (hand == InteractionHand.MAIN_HAND) == mainArmRight;
        return usingRightArm ? CyberwareSlot.RARM : CyberwareSlot.LARM;
    }
}