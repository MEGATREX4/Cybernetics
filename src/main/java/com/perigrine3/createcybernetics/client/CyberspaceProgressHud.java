package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.network.payload.CyberspaceProgressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CyberspaceProgressHud {
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 6;

    private static final int SAFE_COLOR = 0xFF35D04F;
    private static final int UNSAFE_COLOR = 0xFFD83232;
    private static final int BAR_BACKGROUND = 0xB0000000;
    private static final int BAR_BORDER = 0xCCFFFFFF;

    private static boolean active;
    private static int mode;
    private static int percent;

    private CyberspaceProgressHud() {}

    public static void handle(CyberspaceProgressPayload payload) {
        if (!payload.active()) {
            clear();
            return;
        }

        active = true;
        mode = payload.mode();
        percent = Math.max(0, Math.min(100, payload.percent()));
    }

    public static void clear() {
        active = false;
        mode = 0;
        percent = 0;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            clear();
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        Component progressMessage = getProgressMessage();

        Component warningMessage1 = mode == CyberspaceProgressPayload.UNSAFE_JACK_OUT
                ? Component.translatable("message.createcybernetics.cyberspace.unsafe_jack_out_warning1")
                : null;

        Component warningMessage2 = mode == CyberspaceProgressPayload.UNSAFE_JACK_OUT
                ? Component.translatable("message.createcybernetics.cyberspace.unsafe_jack_out_warning2")
                : null;

        int progressY = screenHeight - 68;

        int warning2Y = progressY - 12;
        int warning1Y = warning2Y - 12;

        int barY = warningMessage1 != null ? warning1Y - 12 : progressY - 12;

        int progressX = (screenWidth - mc.font.width(progressMessage)) / 2;
        guiGraphics.drawString(mc.font, progressMessage, progressX, progressY, 0xFFFFFFFF, true);

        if (warningMessage1 != null) {
            int warning1X = (screenWidth - mc.font.width(warningMessage1)) / 2;
            guiGraphics.drawString(mc.font, warningMessage1, warning1X, warning1Y, 0xFFFFFFFF, true);
        }

        if (warningMessage2 != null) {
            int warning2X = (screenWidth - mc.font.width(warningMessage2)) / 2;
            guiGraphics.drawString(mc.font, warningMessage2, warning2X, warning2Y, 0xFFFFFFFF, true);
        }

        renderProgressBar(guiGraphics, screenWidth, barY);
    }

    private static Component getProgressMessage() {
        return switch (mode) {
            case CyberspaceProgressPayload.JACK_IN ->
                    Component.translatable("message.createcybernetics.cyberspace.jacking_in", percent);

            case CyberspaceProgressPayload.SAFE_JACK_OUT ->
                    Component.translatable("message.createcybernetics.cyberspace.jacking_out", percent);

            case CyberspaceProgressPayload.UNSAFE_JACK_OUT ->
                    Component.translatable("message.createcybernetics.cyberspace.unsafe_jacking_out", percent);

            default -> Component.empty();
        };
    }

    private static void renderProgressBar(GuiGraphics guiGraphics, int screenWidth, int y) {
        int x = (screenWidth - BAR_WIDTH) / 2;
        int fillWidth = Math.round((BAR_WIDTH - 2) * (percent / 100.0F));
        int fillColor = mode == CyberspaceProgressPayload.UNSAFE_JACK_OUT ? UNSAFE_COLOR : SAFE_COLOR;

        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BAR_BORDER);
        guiGraphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, BAR_BACKGROUND);

        if (fillWidth > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT - 1, fillColor);
        }
    }
}