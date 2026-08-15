package com.perigrine3.createcybernetics.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.perigrine3.createcybernetics.client.CybereyeIrisConfigClient;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CybereyeSkinConfigScreen extends Screen {

    private static final int SKIN_TEX_W = 64;
    private static final int SKIN_TEX_H = 64;

    private static final int GRID_COLS = 32;
    private static final int GRID_ROWS = 16;

    private static final int GRID_LINE = 0x44FFFFFF;
    private static final int GRID_BORDER = 0xAAFFFFFF;

    private static final int PRIMARY_FILL = 0x8800FF00;
    private static final int PRIMARY_EDGE = 0xFF00FF00;

    private static final int SECONDARY_FILL = 0x8800AAFF;
    private static final int SECONDARY_EDGE = 0xFF00AAFF;

    private static final int HOVER_EDGE = 0xFFFFFFFF;
    private static final int INVALID_FILL = 0xAA000000;
    private static final int DIM_OVERLAY = 0xCC000000;

    private static final int BTN_H = 20;
    private static final int BTN_GAP = 6;

    private final Screen parent;

    private @Nullable UUID playerId;
    private CybereyeVisualConfig.Layout workingLayout = CybereyeVisualConfig.defaults();
    private CybereyeVisualConfig.Channel activeChannel = CybereyeVisualConfig.Channel.PRIMARY;

    private int gridX;
    private int gridY;
    private int cell;
    private int gridW;
    private int gridH;

    private boolean painting = false;
    private boolean paintValue = true;
    private boolean saved = false;

    private Button primaryButton;
    private Button secondaryButton;
    private Button clearButton;
    private Button opticsButton;
    private Button saveButton;
    private Button backButton;

    public CybereyeSkinConfigScreen(Screen parent) {
        super(Component.translatable("screen.createcybernetics.cybereye_skin"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LocalPlayer player = Minecraft.getInstance().player;
        this.playerId = player != null ? player.getUUID() : null;

        if (this.playerId != null) {
            this.workingLayout = CybereyeIrisConfigClient.get(this.playerId).copy();
        } else {
            this.workingLayout = CybereyeVisualConfig.defaults();
        }

        this.saved = false;

        cc$rebuildWidgets();

        if (player != null) {
            CybereyeIrisConfigClient.applyLocalPreview(player, workingLayout);
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        cc$rebuildWidgets();
    }

    private void cc$rebuildWidgets() {
        clearWidgets();
        computeGridLayout();

        int controlsY = gridY + gridH + 10;

        int channelW = 76;
        int clearW = 70;
        int opticsW = 110;
        int saveW = 60;
        int backW = 60;

        int firstRowW = channelW * 2 + clearW + BTN_GAP * 2;
        int firstRowX = (this.width - firstRowW) / 2;

        primaryButton = addRenderableWidget(Button.builder(Component.literal("Primary"), button -> selectChannel(CybereyeVisualConfig.Channel.PRIMARY))
                .pos(firstRowX, controlsY)
                .size(channelW, BTN_H)
                .build());

        secondaryButton = addRenderableWidget(Button.builder(Component.literal("Secondary"), button -> selectChannel(CybereyeVisualConfig.Channel.SECONDARY))
                .pos(firstRowX + channelW + BTN_GAP, controlsY)
                .size(channelW, BTN_H)
                .build());

        clearButton = addRenderableWidget(Button.builder(Component.literal("Clear"), button -> clearActiveChannel())
                .pos(firstRowX + channelW * 2 + BTN_GAP * 2, controlsY)
                .size(clearW, BTN_H)
                .build());

        int secondRowW = opticsW + saveW + backW + BTN_GAP * 2;
        int secondRowX = (this.width - secondRowW) / 2;
        int secondRowY = controlsY + BTN_H + BTN_GAP;

        opticsButton = addRenderableWidget(Button.builder(Component.literal("Optics Alignment"), button -> openOpticsScreen())
                .pos(secondRowX, secondRowY)
                .size(opticsW, BTN_H)
                .build());

        saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.createcybernetics.save"), button -> save())
                .pos(secondRowX + opticsW + BTN_GAP, secondRowY)
                .size(saveW, BTN_H)
                .build());

        backButton = addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> cancelAndBack())
                .pos(secondRowX + opticsW + saveW + BTN_GAP * 2, secondRowY)
                .size(backW, BTN_H)
                .build());

        updateButtonStates();
    }

    private void computeGridLayout() {
        int availableWidth = Math.max(96, this.width - 32);
        int availableHeight = Math.max(48, this.height - 120);

        int byWidth = availableWidth / GRID_COLS;
        int byHeight = availableHeight / GRID_ROWS;

        this.cell = Math.max(3, Math.min(24, Math.min(byWidth, byHeight)));
        this.gridW = GRID_COLS * cell;
        this.gridH = GRID_ROWS * cell;

        this.gridX = (this.width - gridW) / 2;
        this.gridY = 48 + Math.max(0, (availableHeight - gridH) / 2);
    }

    private void selectChannel(CybereyeVisualConfig.Channel channel) {
        this.activeChannel = channel;
        updateButtonStates();
    }

    private void clearActiveChannel() {
        activeMask().clear();
        preview();
    }

    private void openOpticsScreen() {
        Minecraft.getInstance().setScreen(new CybereyeOpticsConfigScreen(this, workingLayout));
    }

    private void save() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        CybereyeIrisConfigClient.saveAndSync(player, workingLayout);
        saved = true;
    }

    private void cancelAndBack() {
        if (!saved) {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player != null) {
                CybereyeIrisConfigClient.applyCachedConfigToPlayer(player);
            }
        }

        Minecraft.getInstance().setScreen(parent);
    }

    private void updateButtonStates() {
        if (primaryButton != null) {
            primaryButton.active = activeChannel != CybereyeVisualConfig.Channel.PRIMARY;
        }

        if (secondaryButton != null) {
            secondaryButton.active = activeChannel != CybereyeVisualConfig.Channel.SECONDARY;
        }
    }

    private CybereyeVisualConfig.PixelMask activeMask() {
        return activeChannel == CybereyeVisualConfig.Channel.PRIMARY ? workingLayout.primary : workingLayout.secondary;
    }

    private CybereyeVisualConfig.PixelMask otherMask() {
        return activeChannel == CybereyeVisualConfig.Channel.PRIMARY ? workingLayout.secondary : workingLayout.primary;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 && button != 1) return false;
        if (!isInGrid(mouseX, mouseY)) return false;

        int u = (int) ((mouseX - gridX) / cell);
        int v = (int) ((mouseY - gridY) / cell);

        if (!CybereyeVisualConfig.isSelectableHeadPixel(u, v)) return false;

        painting = true;
        paintValue = button == 0;

        paintPixel(u, v);

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (!painting) return false;
        if (!isInGrid(mouseX, mouseY)) return true;

        int u = (int) ((mouseX - gridX) / cell);
        int v = (int) ((mouseY - gridY) / cell);

        if (CybereyeVisualConfig.isSelectableHeadPixel(u, v)) {
            paintPixel(u, v);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            painting = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void paintPixel(int u, int v) {
        if (paintValue) {
            activeMask().set(u, v, true);
            otherMask().set(u, v, false);
        } else {
            workingLayout.primary.set(u, v, false);
            workingLayout.secondary.set(u, v, false);
        }

        preview();
    }

    private void preview() {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            CybereyeIrisConfigClient.applyLocalPreview(player, workingLayout);
        }
    }

    private boolean isInGrid(double mouseX, double mouseY) {
        return mouseX >= gridX && mouseX < gridX + gridW && mouseY >= gridY && mouseY < gridY + gridH;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        computeGridLayout();

        gg.fill(0, 0, this.width, this.height, DIM_OVERLAY);

        super.render(gg, mouseX, mouseY, partialTick);

        renderHeadTexture(gg);
        renderInvalidCells(gg);
        renderSelections(gg);
        renderGrid(gg);
        renderHover(gg, mouseX, mouseY);

        gg.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        Component status = Component.translatable(activeChannel == CybereyeVisualConfig.Channel.PRIMARY
                ? "screen.createcybernetics.cybereye_skin.editing_primary"
                : "screen.createcybernetics.cybereye_skin.editing_secondary");

        gg.drawCenteredString(this.font, status, this.width / 2, 24,
                activeChannel == CybereyeVisualConfig.Channel.PRIMARY ? 0x55FF55 : 0x55AAFF);

        Component instructions = Component.translatable("screen.createcybernetics.cybereye_skin.instructions");
        gg.drawCenteredString(this.font, instructions, this.width / 2, 34, 0xAAAAAA);
    }

    private void renderHeadTexture(GuiGraphics gg) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            gg.fill(gridX, gridY, gridX + gridW, gridY + gridH, 0x66000000);
            return;
        }

        AbstractClientPlayer player = mc.player;
        var skin = player.getSkin().texture();

        try {
            AbstractTexture texture = mc.getTextureManager().getTexture(skin);
            texture.setFilter(false, false);
        } catch (Throwable ignored) {
        }

        RenderSystem.setShaderTexture(0, skin);

        for (int v = 0; v < GRID_ROWS; v++) {
            for (int u = 0; u < GRID_COLS; u++) {
                int x = gridX + u * cell;
                int y = gridY + v * cell;

                gg.blit(
                        skin,
                        x, y,
                        cell, cell,
                        u, v,
                        1, 1,
                        SKIN_TEX_W, SKIN_TEX_H
                );
            }
        }
    }

    private void renderInvalidCells(GuiGraphics gg) {
        for (int v = 0; v < GRID_ROWS; v++) {
            for (int u = 0; u < GRID_COLS; u++) {
                if (CybereyeVisualConfig.isSelectableHeadPixel(u, v)) continue;

                int x = gridX + u * cell;
                int y = gridY + v * cell;

                gg.fill(x, y, x + cell, y + cell, INVALID_FILL);
            }
        }
    }

    private void renderSelections(GuiGraphics gg) {
        for (int v = 0; v < GRID_ROWS; v++) {
            for (int u = 0; u < GRID_COLS; u++) {
                if (workingLayout.primary.get(u, v)) {
                    renderCellSelection(gg, u, v, PRIMARY_FILL, PRIMARY_EDGE);
                }

                if (workingLayout.secondary.get(u, v)) {
                    renderCellSelection(gg, u, v, SECONDARY_FILL, SECONDARY_EDGE);
                }
            }
        }
    }

    private void renderCellSelection(GuiGraphics gg, int u, int v, int fill, int edge) {
        int x = gridX + u * cell;
        int y = gridY + v * cell;

        gg.fill(x, y, x + cell, y + cell, fill);
        gg.hLine(x, x + cell, y, edge);
        gg.hLine(x, x + cell, y + cell, edge);
        gg.vLine(x, y, y + cell, edge);
        gg.vLine(x + cell, y, y + cell, edge);
    }

    private void renderGrid(GuiGraphics gg) {
        gg.hLine(gridX, gridX + gridW, gridY, GRID_BORDER);
        gg.hLine(gridX, gridX + gridW, gridY + gridH, GRID_BORDER);
        gg.vLine(gridX, gridY, gridY + gridH, GRID_BORDER);
        gg.vLine(gridX + gridW, gridY, gridY + gridH, GRID_BORDER);

        for (int x = 1; x < GRID_COLS; x++) {
            int px = gridX + x * cell;
            gg.vLine(px, gridY, gridY + gridH, GRID_LINE);
        }

        for (int y = 1; y < GRID_ROWS; y++) {
            int py = gridY + y * cell;
            gg.hLine(gridX, gridX + gridW, py, GRID_LINE);
        }
    }

    private void renderHover(GuiGraphics gg, int mouseX, int mouseY) {
        if (!isInGrid(mouseX, mouseY)) return;

        int u = (mouseX - gridX) / cell;
        int v = (mouseY - gridY) / cell;

        if (!CybereyeVisualConfig.isSelectableHeadPixel(u, v)) return;

        int x = gridX + u * cell;
        int y = gridY + v * cell;

        gg.hLine(x, x + cell, y, HOVER_EDGE);
        gg.hLine(x, x + cell, y + cell, HOVER_EDGE);
        gg.vLine(x, y, y + cell, HOVER_EDGE);
        gg.vLine(x + cell, y, y + cell, HOVER_EDGE);
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