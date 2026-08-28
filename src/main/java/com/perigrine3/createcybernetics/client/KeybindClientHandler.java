package com.perigrine3.createcybernetics.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.generic.InfologDataShardItem;
import com.perigrine3.createcybernetics.item.generic.InfologTextData;
import com.perigrine3.createcybernetics.network.payload.*;
import com.perigrine3.createcybernetics.screen.custom.arm_cannon.ArmCannonWheelScreen;
import com.perigrine3.createcybernetics.screen.custom.chipware.InfologEditScreen;
import com.perigrine3.createcybernetics.screen.custom.chipware.InfologReadScreen;
import com.perigrine3.createcybernetics.screen.custom.cyberdeck.CyberdeckQuickhackWheelScreen;
import com.perigrine3.createcybernetics.screen.custom.cyberdeck.MacroprogramWheelScreen;
import com.perigrine3.createcybernetics.screen.custom.toggle_wheel.CyberwareToggleWheelScreen;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class KeybindClientHandler {
    private KeybindClientHandler() {}

    private static boolean wasUnsafeJackOutHeld;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean inCyberspace = mc.player.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL);

        if (inCyberspace) {
            drainCyberwareKeybinds();
            handleMacroprogramWheel(mc);
            handleCyberdeckKey(mc);
            handleCyberdeckQuickhackWheel(mc);
            handleUnsafeJackOutKey(mc);
            return;
        }

        if (wasUnsafeJackOutHeld) {
            PacketDistributor.sendToServer(new CyberspaceUnsafeJackOutHoldPayload(false));
            wasUnsafeJackOutHeld = false;
        }

        while (ModKeyMappings.CYBERWARE_WHEEL.get().consumeClick()) {
            if (CyberwareToggleWheelScreen.isWheelOpen()) {
                CyberwareToggleWheelScreen.closeWheel();
                if (mc.screen != null) mc.setScreen(null);
            } else {
                mc.setScreen(new CyberwareToggleWheelScreen());
            }
        }

        while (ModKeyMappings.ARM_CANNON_WHEEL.get().consumeClick()) {
            if (ArmCannonWheelScreen.isOpen()) {
                ArmCannonWheelScreen.close();
                if (mc.screen != null) mc.setScreen(null);
            } else {
                ArmCannonWheelScreen.open(4);

                if (mc.player.hasData(ModAttachments.CYBERWARE)) {
                    PlayerCyberwareData data = mc.player.getData(ModAttachments.CYBERWARE);
                    ArmCannonWheelScreen.setPreselectedIndex(data.getArmCannonSelected());
                }

                mc.setScreen(new ArmCannonWheelScreen());
            }
        }

        while (ModKeyMappings.SPINAL_INJECTOR.get().consumeClick()) {
            if (mc.screen != null) continue;
            OpenSpinalInjectorPayload payload = new OpenSpinalInjectorPayload();
            PacketDistributor.sendToServer(payload);
        }

        while (ModKeyMappings.ARM_CANNON.get().consumeClick()) {
            if (mc.screen != null) continue;
            OpenArmCannonPayload payload = new OpenArmCannonPayload();
            PacketDistributor.sendToServer(payload);
        }

        while (ModKeyMappings.HEAT_ENGINE.get().consumeClick()) {
            if (mc.screen != null) continue;
            OpenHeatEnginePayload payload = new OpenHeatEnginePayload();
            PacketDistributor.sendToServer(payload);
        }

        handleCyberdeckKey(mc);
        handleCyberdeckQuickhackWheel(mc);

        while (ModKeyMappings.INFOLOG.get().consumeClick()) {
            if (mc.screen != null) continue;
            if (!mc.player.hasData(ModAttachments.CYBERWARE)) continue;

            PlayerCyberwareData data = mc.player.getData(ModAttachments.CYBERWARE);
            if (data == null) continue;

            int found = -1;
            ItemStack foundStack = ItemStack.EMPTY;

            for (int i = 0; i < PlayerCyberwareData.CHIPWARE_SLOT_COUNT; i++) {
                ItemStack st = data.getChipwareStack(i);
                if (st.isEmpty()) continue;

                if (st.getItem() instanceof InfologDataShardItem) {
                    found = i;
                    foundStack = st;
                    break;
                }
            }

            if (found == -1) continue;

            String text = InfologTextData.getText(foundStack);

            if (InfologTextData.isLocked(foundStack)) {
                String title = InfologTextData.getTitle(foundStack);

                mc.setScreen(new InfologReadScreen(
                        found,
                        title,
                        text
                ));

                continue;
            }

            mc.setScreen(new InfologEditScreen(found, text));
        }
    }

    private static void handleMacroprogramWheel(Minecraft mc) {
        while (ModKeyMappings.CYBERWARE_WHEEL.get().consumeClick()) {
            if (MacroprogramWheelScreen.isWheelOpen()) {
                MacroprogramWheelScreen.closeWheel();
                if (mc.screen != null) mc.setScreen(null);
            } else {
                mc.setScreen(new MacroprogramWheelScreen());
            }
        }
    }

    private static void handleCyberdeckKey(Minecraft mc) {
        while (ModKeyMappings.CYBERDECK.get().consumeClick()) {
            if (mc.screen != null) continue;
            PacketDistributor.sendToServer(new OpenCyberdeckPayload());
        }
    }

    private static void handleCyberdeckQuickhackWheel(Minecraft mc) {
        while (ModKeyMappings.CYBERDECK_WHEEL.get().consumeClick()) {
            if (CyberdeckQuickhackWheelScreen.isOpen()) {
                CyberdeckQuickhackWheelScreen.close();
                if (mc.screen != null) mc.setScreen(null);
            } else {
                mc.setScreen(new CyberdeckQuickhackWheelScreen());
            }
        }
    }

    private static void handleUnsafeJackOutKey(Minecraft mc) {
        boolean held = mc.screen == null && ModKeyMappings.UNSAFE_JACK_OUT.get().isDown();

        if (held) {
            PacketDistributor.sendToServer(new CyberspaceUnsafeJackOutHoldPayload(true));
        } else if (wasUnsafeJackOutHeld) {
            PacketDistributor.sendToServer(new CyberspaceUnsafeJackOutHoldPayload(false));
        }

        wasUnsafeJackOutHeld = held;
    }

    private static void drainCyberwareKeybinds() {
        while (ModKeyMappings.SPINAL_INJECTOR.get().consumeClick()) {}
        while (ModKeyMappings.ARM_CANNON.get().consumeClick()) {}
        while (ModKeyMappings.ARM_CANNON_WHEEL.get().consumeClick()) {}
        while (ModKeyMappings.HEAT_ENGINE.get().consumeClick()) {}
        while (ModKeyMappings.INFOLOG.get().consumeClick()) {}
    }

    private static void passthroughMovementKeys(Minecraft mc) {
        passthrough(mc, mc.options.keyUp);
        passthrough(mc, mc.options.keyDown);
        passthrough(mc, mc.options.keyLeft);
        passthrough(mc, mc.options.keyRight);
        passthrough(mc, mc.options.keyJump);
        passthrough(mc, mc.options.keySprint);
        passthrough(mc, mc.options.keyShift);
    }

    private static void passthrough(Minecraft mc, KeyMapping key) {
        long window = mc.getWindow().getWindow();
        var k = key.getKey();

        boolean down;
        if (k.getType() == InputConstants.Type.MOUSE) {
            down = GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
        } else {
            down = InputConstants.isKeyDown(window, k.getValue());
        }

        if (key.isDown() != down) {
            key.setDown(down);
        }
    }
}