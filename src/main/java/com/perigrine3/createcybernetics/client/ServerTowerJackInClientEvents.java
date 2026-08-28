package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.network.payload.ServerTowerJackInHoldPayload;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ServerTowerJackInClientEvents {
    private static boolean wasHolding;

    private ServerTowerJackInClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            wasHolding = false;
            return;
        }

        if (mc.level.dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) {
            if (wasHolding) {
                PacketDistributor.sendToServer(new ServerTowerJackInHoldPayload(false));
                wasHolding = false;
            }

            return;
        }

        boolean holding = false;

        if (mc.screen == null && mc.options.keyUse.isDown() && mc.hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);

            holding = state.is(ModBlocks.SERVER_TOWER.get()) || state.is(ModBlocks.SERVER_TOWER_PART.get());
        }

        if (holding) {
            PacketDistributor.sendToServer(new ServerTowerJackInHoldPayload(true));
        } else if (wasHolding) {
            PacketDistributor.sendToServer(new ServerTowerJackInHoldPayload(false));
        }

        wasHolding = holding;
    }
}