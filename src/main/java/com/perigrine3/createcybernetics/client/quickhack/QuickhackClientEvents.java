package com.perigrine3.createcybernetics.client.quickhack;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class QuickhackClientEvents {
    private static final int MOTOR_LATENCY_TICKS = 5;
    private static final Deque<InputSnapshot> INPUT_HISTORY = new ArrayDeque<>();
    private static UUID activePlayerId;

    private QuickhackClientEvents() {}


    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) {
            QuickhackClientState.clear();
            INPUT_HISTORY.clear();
            activePlayerId = null;
            return;
        }

        if (activePlayerId == null || !activePlayerId.equals(player.getUUID())) {
            QuickhackClientState.clear();
            INPUT_HISTORY.clear();
            activePlayerId = player.getUUID();
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getEntity() != player) {
            INPUT_HISTORY.clear();
            return;
        }

        if (QuickhackClientState.invertMovement()) {
            event.getInput().leftImpulse = -event.getInput().leftImpulse;
            event.getInput().forwardImpulse = -event.getInput().forwardImpulse;

            boolean up = event.getInput().up;
            boolean down = event.getInput().down;
            boolean left = event.getInput().left;
            boolean right = event.getInput().right;

            event.getInput().up = down;
            event.getInput().down = up;
            event.getInput().left = right;
            event.getInput().right = left;
        }

        if (!QuickhackClientState.motorLatency()) {
            INPUT_HISTORY.clear();
            return;
        }

        INPUT_HISTORY.addLast(new InputSnapshot(
                event.getInput().leftImpulse,
                event.getInput().forwardImpulse,
                event.getInput().jumping,
                event.getInput().shiftKeyDown,
                event.getInput().up,
                event.getInput().down,
                event.getInput().left,
                event.getInput().right
        ));

        if (INPUT_HISTORY.size() <= MOTOR_LATENCY_TICKS) {
            event.getInput().leftImpulse = 0.0F;
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
            event.getInput().up = false;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            return;
        }

        InputSnapshot delayed = INPUT_HISTORY.removeFirst();

        event.getInput().leftImpulse = delayed.leftImpulse();
        event.getInput().forwardImpulse = delayed.forwardImpulse();
        event.getInput().jumping = delayed.jumping();
        event.getInput().shiftKeyDown = delayed.shiftKeyDown();
        event.getInput().up = delayed.up();
        event.getInput().down = delayed.down();
        event.getInput().left = delayed.left();
        event.getInput().right = delayed.right();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!QuickhackClientState.invertVision()) return;
        event.setRoll(Mth.wrapDegrees(event.getRoll() + 180.0F));
    }

    private record InputSnapshot(float leftImpulse, float forwardImpulse, boolean jumping, boolean shiftKeyDown, boolean up, boolean down, boolean left, boolean right) {}
}
