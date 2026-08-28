package com.perigrine3.createcybernetics.client.gui;

import com.perigrine3.createcybernetics.client.CyberspaceColorConfigClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CyberspaceColorConfigScreen extends Screen {

    private static final int DIM_OVERLAY = 0xCC000000;

    private static final int WHEEL_SIZE = 120;
    private static final int WHEEL_OUTER_RADIUS = 60;
    private static final int WHEEL_INNER_RADIUS = 42;

    private static final int SLIDER_W = 160;
    private static final int SLIDER_H = 12;

    private static final int BTN_W = 70;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 6;
    private static final int SIDE_GAP = 12;

    private final Screen parent;

    private @Nullable UUID playerId;

    private float hue;
    private float saturation;
    private float brightness;

    private float originalHue;
    private float originalSaturation;
    private float originalBrightness;

    private int wheelX;
    private int wheelY;

    private int buttonX;
    private int buttonY;

    private int saturationX;
    private int saturationY;

    private int brightnessX;
    private int brightnessY;

    private DragTarget dragTarget = DragTarget.NONE;

    private enum DragTarget {
        NONE,
        HUE,
        SATURATION,
        BRIGHTNESS
    }

    public CyberspaceColorConfigScreen(Screen parent) {
        super(Component.translatable("screen.createcybernetics.cyberspace_color"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LocalPlayer player = Minecraft.getInstance().player;
        this.playerId = player != null ? player.getUUID() : null;

        int color = this.playerId != null
                ? CyberspaceColorConfigClient.getLocalColor(this.playerId)
                : CyberspaceColorConfigClient.DEFAULT_COLOR;

        float[] hsv = rgbToHsv(color);

        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.brightness = hsv[2];

        this.originalHue = hue;
        this.originalSaturation = saturation;
        this.originalBrightness = brightness;

        rebuildWidgets();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();
        computeLayout();

        addRenderableWidget(Button.builder(Component.translatable("gui.createcybernetics.save"), button -> save())
                .pos(buttonX, buttonY)
                .size(BTN_W, BTN_H)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.createcybernetics.reset"), button -> reset())
                .pos(buttonX, buttonY + BTN_H + BTN_GAP)
                .size(BTN_W, BTN_H)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> cancelAndBack())
                .pos(buttonX, buttonY + (BTN_H + BTN_GAP) * 2)
                .size(BTN_W, BTN_H)
                .build());
    }

    private void computeLayout() {
        int groupW = WHEEL_SIZE + SIDE_GAP + BTN_W;

        wheelX = (this.width - groupW) / 2;
        wheelY = Math.max(42, (this.height - 270) / 2 + 36);

        buttonX = wheelX + WHEEL_SIZE + SIDE_GAP;

        int buttonStackH = BTN_H * 3 + BTN_GAP * 2;
        buttonY = wheelY + (WHEEL_SIZE - buttonStackH) / 2;

        saturationX = wheelX + (WHEEL_SIZE - SLIDER_W) / 2;
        saturationY = wheelY + WHEEL_SIZE + 24;

        brightnessX = saturationX;
        brightnessY = saturationY + 34;
    }

    private void save() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        int color = getCurrentColor();

        CyberspaceColorConfigClient.saveAndSync(player, color);

        float[] hsv = rgbToHsv(color);

        originalHue = hsv[0];
        originalSaturation = hsv[1];
        originalBrightness = hsv[2];
    }

    private void reset() {
        float[] hsv = rgbToHsv(CyberspaceColorConfigClient.DEFAULT_COLOR);

        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
    }

    private void cancelAndBack() {
        hue = originalHue;
        saturation = originalSaturation;
        brightness = originalBrightness;

        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        if (isInsideHueRing(mouseX, mouseY)) {
            dragTarget = DragTarget.HUE;
            updateHue(mouseX, mouseY);
            return true;
        }

        if (isInside(mouseX, mouseY, saturationX, saturationY, SLIDER_W, SLIDER_H)) {
            dragTarget = DragTarget.SATURATION;
            updateSaturation(mouseX);
            return true;
        }

        if (isInside(mouseX, mouseY, brightnessX, brightnessY, SLIDER_W, SLIDER_H)) {
            dragTarget = DragTarget.BRIGHTNESS;
            updateBrightness(mouseX);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (button != 0) return false;

        switch (dragTarget) {
            case HUE -> updateHue(mouseX, mouseY);
            case SATURATION -> updateSaturation(mouseX);
            case BRIGHTNESS -> updateBrightness(mouseX);
            case NONE -> {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragTarget = DragTarget.NONE;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateHue(double mouseX, double mouseY) {
        double centerX = wheelX + WHEEL_SIZE / 2.0;
        double centerY = wheelY + WHEEL_SIZE / 2.0;

        double angle = Math.atan2(mouseY - centerY, mouseX - centerX);
        float normalized = (float) (angle / (Math.PI * 2.0));

        if (normalized < 0.0F) normalized += 1.0F;

        hue = normalized;
    }

    private void updateSaturation(double mouseX) {
        saturation = clamp01((float) ((mouseX - saturationX) / SLIDER_W));
    }

    private void updateBrightness(double mouseX) {
        brightness = clamp01((float) ((mouseX - brightnessX) / SLIDER_W));
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        computeLayout();

        gg.fill(0, 0, this.width, this.height, DIM_OVERLAY);

        super.render(gg, mouseX, mouseY, partialTick);

        gg.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);

        renderHueWheel(gg);
        renderSaturationSlider(gg);
        renderBrightnessSlider(gg);
        renderPreview(gg);

        gg.drawString(this.font, Component.translatable("screen.createcybernetics.cyberspace_color.saturation"), saturationX, saturationY - 11, 0xAAAAAA);
        gg.drawString(this.font, Component.translatable("screen.createcybernetics.cyberspace_color.brightness"), brightnessX, brightnessY - 11, 0xAAAAAA);

        String hex = String.format("#%06X", getCurrentColor() & 0x00FFFFFF);
        gg.drawCenteredString(this.font, hex, saturationX + SLIDER_W / 2, brightnessY + SLIDER_H + 8, 0xFFFFFF);
    }

    private void renderHueWheel(GuiGraphics gg) {
        double center = WHEEL_SIZE / 2.0;

        for (int y = 0; y < WHEEL_SIZE; y += 2) {
            for (int x = 0; x < WHEEL_SIZE; x += 2) {
                double dx = x + 1.0 - center;
                double dy = y + 1.0 - center;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < WHEEL_INNER_RADIUS || distance > WHEEL_OUTER_RADIUS) continue;

                double angle = Math.atan2(dy, dx);
                float pixelHue = (float) (angle / (Math.PI * 2.0));

                if (pixelHue < 0.0F) pixelHue += 1.0F;

                int color = hsvToRgb(pixelHue, 1.0F, 1.0F);

                gg.fill(wheelX + x, wheelY + y, wheelX + x + 2, wheelY + y + 2, color);
            }
        }

        double angle = hue * Math.PI * 2.0;
        double radius = (WHEEL_INNER_RADIUS + WHEEL_OUTER_RADIUS) * 0.5;

        int markerX = wheelX + WHEEL_SIZE / 2 + (int) Math.round(Math.cos(angle) * radius);
        int markerY = wheelY + WHEEL_SIZE / 2 + (int) Math.round(Math.sin(angle) * radius);

        renderMarker(gg, markerX, markerY);
    }

    private void renderSaturationSlider(GuiGraphics gg) {
        for (int x = 0; x < SLIDER_W; x++) {
            float value = x / (float) (SLIDER_W - 1);
            int color = hsvToRgb(hue, value, brightness);

            gg.fill(saturationX + x, saturationY, saturationX + x + 1, saturationY + SLIDER_H, color);
        }

        int markerX = saturationX + Math.round(saturation * (SLIDER_W - 1));
        renderSliderMarker(gg, markerX, saturationY);
    }

    private void renderBrightnessSlider(GuiGraphics gg) {
        for (int x = 0; x < SLIDER_W; x++) {
            float value = x / (float) (SLIDER_W - 1);
            int color = hsvToRgb(hue, saturation, value);

            gg.fill(brightnessX + x, brightnessY, brightnessX + x + 1, brightnessY + SLIDER_H, color);
        }

        int markerX = brightnessX + Math.round(brightness * (SLIDER_W - 1));
        renderSliderMarker(gg, markerX, brightnessY);
    }

    private void renderPreview(GuiGraphics gg) {
        int previewSize = WHEEL_INNER_RADIUS * 2 - 14;
        int previewX = wheelX + (WHEEL_SIZE - previewSize) / 2;
        int previewY = wheelY + (WHEEL_SIZE - previewSize) / 2;

        int color = getCurrentColor();

        gg.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000);
        gg.fill(previewX + 2, previewY + 2, previewX + previewSize - 2, previewY + previewSize - 2, color);
    }

    private static void renderMarker(GuiGraphics gg, int x, int y) {
        gg.fill(x - 3, y - 3, x + 4, y + 4, 0xFF000000);
        gg.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFFF);
        gg.fill(x - 1, y - 1, x + 2, y + 2, 0xFF000000);
    }

    private static void renderSliderMarker(GuiGraphics gg, int x, int y) {
        gg.fill(x - 1, y - 2, x + 2, y + SLIDER_H + 2, 0xFF000000);
        gg.fill(x, y - 1, x + 1, y + SLIDER_H + 1, 0xFFFFFFFF);
    }

    private boolean isInsideHueRing(double mouseX, double mouseY) {
        double centerX = wheelX + WHEEL_SIZE / 2.0;
        double centerY = wheelY + WHEEL_SIZE / 2.0;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        return distance >= WHEEL_INNER_RADIUS && distance <= WHEEL_OUTER_RADIUS;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int getCurrentColor() {
        return hsvToRgb(hue, saturation, brightness);
    }

    private static int hsvToRgb(float hue, float saturation, float brightness) {
        hue = hue - (float) Math.floor(hue);
        saturation = clamp01(saturation);
        brightness = clamp01(brightness);

        float h = hue * 6.0F;
        int sector = (int) Math.floor(h);
        float fraction = h - sector;

        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * fraction);
        float t = brightness * (1.0F - saturation * (1.0F - fraction));

        float r;
        float g;
        float b;

        switch (sector % 6) {
            case 0 -> {
                r = brightness;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = brightness;
                b = p;
            }
            case 2 -> {
                r = p;
                g = brightness;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = brightness;
            }
            case 4 -> {
                r = t;
                g = p;
                b = brightness;
            }
            default -> {
                r = brightness;
                g = p;
                b = q;
            }
        }

        int ri = Math.round(r * 255.0F);
        int gi = Math.round(g * 255.0F);
        int bi = Math.round(b * 255.0F);

        return 0xFF000000 | ri << 16 | gi << 8 | bi;
    }

    private static float[] rgbToHsv(int color) {
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue;

        if (delta == 0.0F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = ((g - b) / delta) % 6.0F;
        } else if (max == g) {
            hue = (b - r) / delta + 2.0F;
        } else {
            hue = (r - g) / delta + 4.0F;
        }

        hue /= 6.0F;

        if (hue < 0.0F) {
            hue += 1.0F;
        }

        float saturation = max == 0.0F ? 0.0F : delta / max;

        return new float[]{hue, saturation, max};
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    public void onClose() {
        cancelAndBack();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}