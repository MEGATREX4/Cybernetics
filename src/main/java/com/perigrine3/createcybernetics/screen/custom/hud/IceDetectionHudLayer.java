package com.perigrine3.createcybernetics.screen.custom.hud;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.IceDetectionClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class IceDetectionHudLayer {
    public static final ResourceLocation LAYER =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "ice_detection");

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 7;
    private static final int BAR_INNER_WIDTH = BAR_WIDTH - 2;
    private static final int BAR_INNER_HEIGHT = BAR_HEIGHT - 2;

    private static final int BAR_OFFSET_Y = 55;

    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;

    private static final int LOW_COLOR = 0xFF35F2FF;
    private static final int MEDIUM_COLOR = 0xFFFFD54A;
    private static final int HIGH_COLOR = 0xFFFF6B35;
    private static final int MAX_COLOR = 0xFFFF2B2B;

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private IceDetectionHudLayer() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER, IceDetectionHudLayer::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) return;
        if (minecraft.options.hideGui) return;
        if (!IceDetectionClientState.isActive()) return;

        int detection = IceDetectionClientState.getDetection();

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int barX = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - BAR_OFFSET_Y;

        int fillWidth = Math.round(BAR_INNER_WIDTH * detection / 100.0F);
        int fillColor = getDetectionColor(detection);

        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BORDER_COLOR);
        guiGraphics.fill(barX + 1, barY + 1, barX + BAR_WIDTH - 1, barY + BAR_HEIGHT - 1, BACKGROUND_COLOR);

        if (fillWidth > 0) {
            guiGraphics.fill(
                    barX + 1,
                    barY + 1,
                    barX + 1 + fillWidth,
                    barY + 1 + BAR_INNER_HEIGHT,
                    fillColor
            );
        }

        Component label = Component.translatable("gui.createcybernetics.ice_detection", detection);

        guiGraphics.drawCenteredString(
                minecraft.font,
                label,
                screenWidth / 2,
                barY - 10,
                TEXT_COLOR
        );
    }

    private static int getDetectionColor(int detection) {
        if (detection >= 100) {
            return MAX_COLOR;
        }

        if (detection >= 75) {
            return HIGH_COLOR;
        }

        if (detection >= 50) {
            return MEDIUM_COLOR;
        }

        return LOW_COLOR;
    }
}