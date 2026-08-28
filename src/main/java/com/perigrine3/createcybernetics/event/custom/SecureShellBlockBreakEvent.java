package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramExecutionManager;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SecureShellBlockBreakEvent {
    private SecureShellBlockBreakEvent() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!event.getState().is(ModBlocks.DIGITAL_ARCHITECTURE.get())) return;
        if (!player.getMainHandItem().is(ModItems.MATERIALIZED_SECURE_SHELL.get())) return;

        MacroprogramExecutionManager.recordSecureShellBlockBreak(player);
    }
}