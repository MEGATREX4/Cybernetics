package com.perigrine3.createcybernetics.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.perigrine3.createcybernetics.CreateCybernetics;
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
import net.minecraft.resources.ResourceLocation;

public final class CybereyeOpticsConfigScreen extends Screen {

    private static final int SKIN_TEX_W = 64;
    private static final int SKIN_TEX_H = 64;

    private static final int GRID_COLS = 32;
    private static final int GRID_ROWS = 16;

    private static final int GRID_LINE = 0x44FFFFFF;
    private static final int GRID_BORDER = 0xAAFFFFFF;
    private static final int INVALID_FILL = 0xAA000000;
    private static final int DIM_OVERLAY = 0xCC000000;

    private static final int BTN_W = 96;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 5;

    private static final ResourceLocation MONOVISION =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/monovision.png");

    private static final ResourceLocation MULTIOPTICS1 =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics1.png");

    private static final ResourceLocation MULTIOPTICS2 =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics2.png");

    private static final ResourceLocation MULTIOPTICS3 =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics3.png");

    private static final ResourceLocation MULTIOPTICS4 =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics4.png");

    private final Screen parent;
    private final CybereyeVisualConfig.Layout workingLayout;

    private CybereyeVisualConfig.OpticsType activeType = CybereyeVisualConfig.OpticsType.MONOVISION;

    private int gridX;
    private int gridY;
    private int gridW;
    private int gridH;
    private int cell;

    private boolean dragging = false;
    private double dragStartMouseX;
    private double dragStartMouseY;
    private int dragStartOffsetX;
    private int dragStartOffsetY;

    private Button monovisionButton;
    private Button multi1Button;
    private Button multi2Button;
    private Button multi3Button;
    private Button multi4Button;

    public CybereyeOpticsConfigScreen(Screen parent, CybereyeVisualConfig.Layout workingLayout) {
        super(Component.literal("Optics Alignment"));
        this.parent = parent;
        this.workingLayout = workingLayout;
    }

    @Override
    protected void init() {
        cc$rebuildWidgets();
        preview();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        cc$rebuildWidgets();
    }

    private void cc$rebuildWidgets() {
        clearWidgets();
        computeGridLayout();

        int sideX = gridX + gridW + 12;
        int y = gridY;

        monovisionButton = addRenderableWidget(Button.builder(Component.literal("Monovision"), button -> selectType(CybereyeVisualConfig.OpticsType.MONOVISION))
                .pos(sideX, y)
                .size(BTN_W, BTN_H)
                .build());

        multi1Button = addRenderableWidget(Button.builder(Component.literal("Multioptics I"), button -> selectType(CybereyeVisualConfig.OpticsType.MULTIOPTICS_1))
                .pos(sideX, y + (BTN_H + BTN_GAP))
                .size(BTN_W, BTN_H)
                .build());

        multi2Button = addRenderableWidget(Button.builder(Component.literal("Multioptics II"), button -> selectType(CybereyeVisualConfig.OpticsType.MULTIOPTICS_2))
                .pos(sideX, y + (BTN_H + BTN_GAP) * 2)
                .size(BTN_W, BTN_H)
                .build());

        multi3Button = addRenderableWidget(Button.builder(Component.literal("Multioptics III"), button -> selectType(CybereyeVisualConfig.OpticsType.MULTIOPTICS_3))
                .pos(sideX, y + (BTN_H + BTN_GAP) * 3)
                .size(BTN_W, BTN_H)
                .build());

        multi4Button = addRenderableWidget(Button.builder(Component.literal("Multioptics IV"), button -> selectType(CybereyeVisualConfig.OpticsType.MULTIOPTICS_4))
                .pos(sideX, y + (BTN_H + BTN_GAP) * 4)
                .size(BTN_W, BTN_H)
                .build());

        int arrowsY = y + (BTN_H + BTN_GAP) * 5 + 8;
        int arrowW = 28;

        addRenderableWidget(Button.builder(Component.literal("↑"), button -> move(0, -1))
                .pos(sideX + 34, arrowsY)
                .size(arrowW, BTN_H)
                .build());

        addRenderableWidget(Button.builder(Component.literal("←"), button -> move(-1, 0))
                .pos(sideX, arrowsY + BTN_H + BTN_GAP)
                .size(arrowW, BTN_H)
                .build());

        addRenderableWidget(Button.builder(Component.literal("↓"), button -> move(0, 1))
                .pos(sideX + 34, arrowsY + BTN_H + BTN_GAP)
                .size(arrowW, BTN_H)
                .build());

        addRenderableWidget(Button.builder(Component.literal("→"), button -> move(1, 0))
                .pos(sideX + 68, arrowsY + BTN_H + BTN_GAP)
                .size(arrowW, BTN_H)
                .build());

        int bottomButtonsY = gridY + gridH + 8;
        int resetW = 96;
        int saveW = 60;
        int doneW = 60;
        int bottomButtonsW = resetW + saveW + doneW + BTN_GAP * 2;
        int bottomButtonsX = gridX + (gridW - bottomButtonsW) / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.createcybernetics.cybereye_optics.reset"), button -> resetActive())
                .pos(bottomButtonsX, bottomButtonsY)
                .size(resetW, BTN_H)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.createcybernetics.save"), button -> save())
                .pos(bottomButtonsX + resetW + BTN_GAP, bottomButtonsY)
                .size(saveW, BTN_H)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> done())
                .pos(bottomButtonsX + resetW + saveW + BTN_GAP * 2, bottomButtonsY)
                .size(doneW, BTN_H)
                .build());

        updateButtonStates();
    }

    private void computeGridLayout() {
        int reservedSide = BTN_W + 40;

        int availableWidth = Math.max(96, this.width - reservedSide - 30);
        int availableHeight = Math.max(48, this.height - 100);

        int byWidth = availableWidth / GRID_COLS;
        int byHeight = availableHeight / GRID_ROWS;

        this.cell = Math.max(3, Math.min(24, Math.min(byWidth, byHeight)));
        this.gridW = GRID_COLS * cell;
        this.gridH = GRID_ROWS * cell;

        int totalWidth = gridW + 12 + BTN_W;

        this.gridX = Math.max(10, (this.width - totalWidth) / 2);
        this.gridY = 48 + Math.max(0, (availableHeight - gridH) / 2);
    }

    private void selectType(CybereyeVisualConfig.OpticsType type) {
        activeType = type;
        dragging = false;
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (monovisionButton != null) monovisionButton.active = activeType != CybereyeVisualConfig.OpticsType.MONOVISION;
        if (multi1Button != null) multi1Button.active = activeType != CybereyeVisualConfig.OpticsType.MULTIOPTICS_1;
        if (multi2Button != null) multi2Button.active = activeType != CybereyeVisualConfig.OpticsType.MULTIOPTICS_2;
        if (multi3Button != null) multi3Button.active = activeType != CybereyeVisualConfig.OpticsType.MULTIOPTICS_3;
        if (multi4Button != null) multi4Button.active = activeType != CybereyeVisualConfig.OpticsType.MULTIOPTICS_4;
    }

    private void move(int x, int y) {
        CybereyeVisualConfig.Offset offset = workingLayout.getOffset(activeType);

        offset.x = CybereyeVisualConfig.clampOffset(offset.x + x);
        offset.y = CybereyeVisualConfig.clampOffset(offset.y + y);

        preview();
    }

    private void resetActive() {
        CybereyeVisualConfig.Offset offset = workingLayout.getOffset(activeType);
        offset.x = 0;
        offset.y = 0;

        preview();
    }

    private void done() {
        preview();
        Minecraft.getInstance().setScreen(parent);
    }

    private void save() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        CybereyeIrisConfigClient.saveAndSync(player, workingLayout);
    }

    private void preview() {
        if (Minecraft.getInstance().player != null) {
            CybereyeIrisConfigClient.applyLocalPreview(Minecraft.getInstance().player, workingLayout);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        if (!isInGrid(mouseX, mouseY)) return false;

        CybereyeVisualConfig.Offset offset = workingLayout.getOffset(activeType);

        dragging = true;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragStartOffsetX = offset.x;
        dragStartOffsetY = offset.y;

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (button != 0 || !dragging) return false;

        int pixelDeltaX = (int) Math.round((mouseX - dragStartMouseX) / cell);
        int pixelDeltaY = (int) Math.round((mouseY - dragStartMouseY) / cell);

        CybereyeVisualConfig.Offset offset = workingLayout.getOffset(activeType);

        offset.x = CybereyeVisualConfig.clampOffset(dragStartOffsetX + pixelDeltaX);
        offset.y = CybereyeVisualConfig.clampOffset(dragStartOffsetY + pixelDeltaY);

        preview();

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        computeGridLayout();

        gg.fill(0, 0, this.width, this.height, DIM_OVERLAY);

        super.render(gg, mouseX, mouseY, partialTick);

        renderHeadTexture(gg);
        renderOpticsTexture(gg);
        renderInvalidCells(gg);
        renderGrid(gg);

        gg.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        CybereyeVisualConfig.Offset offset = workingLayout.getOffset(activeType);
        Component status = Component.translatable("screen.createcybernetics.cybereye_optics.position", displayName(activeType), offset.x, offset.y);

        gg.drawCenteredString(this.font, status, this.width / 2, 24, 0xAAAAAA);

        Component instructions = Component.translatable("screen.createcybernetics.cybereye_optics.instructions");
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
            if (texture != null) {
                texture.setFilter(false, false);
            }
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

    private void renderOpticsTexture(GuiGraphics gg) {
        ResourceLocation texture = textureFor(activeType);
        CybereyeVisualConfig.Offset offset = workingLayout.getOffset(activeType);

        Minecraft mc = Minecraft.getInstance();

        try {
            AbstractTexture abstractTexture = mc.getTextureManager().getTexture(texture);
            if (abstractTexture != null) {
                abstractTexture.setFilter(false, false);
            }
        } catch (Throwable ignored) {
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, texture);

        for (int sourceV = 0; sourceV < GRID_ROWS; sourceV++) {
            for (int sourceU = 0; sourceU < GRID_COLS; sourceU++) {
                int targetU = sourceU + offset.x;
                int targetV = sourceV + offset.y;

                if (targetU < 0 || targetU >= GRID_COLS) continue;
                if (targetV < 0 || targetV >= GRID_ROWS) continue;

                int x = gridX + targetU * cell;
                int y = gridY + targetV * cell;

                gg.blit(
                        texture,
                        x, y,
                        cell, cell,
                        sourceU, sourceV,
                        1, 1,
                        SKIN_TEX_W, SKIN_TEX_H
                );
            }
        }

        RenderSystem.disableBlend();
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

    private boolean isInGrid(double mouseX, double mouseY) {
        return mouseX >= gridX && mouseX < gridX + gridW && mouseY >= gridY && mouseY < gridY + gridH;
    }

    private static ResourceLocation textureFor(CybereyeVisualConfig.OpticsType type) {
        return switch (type) {
            case MONOVISION -> MONOVISION;
            case MULTIOPTICS_1 -> MULTIOPTICS1;
            case MULTIOPTICS_2 -> MULTIOPTICS2;
            case MULTIOPTICS_3 -> MULTIOPTICS3;
            case MULTIOPTICS_4 -> MULTIOPTICS4;
        };
    }

    private static String displayName(CybereyeVisualConfig.OpticsType type) {
        return switch (type) {
            case MONOVISION -> "Monovision";
            case MULTIOPTICS_1 -> "Multioptics I";
            case MULTIOPTICS_2 -> "Multioptics II";
            case MULTIOPTICS_3 -> "Multioptics III";
            case MULTIOPTICS_4 -> "Multioptics IV";
        };
    }

    @Override
    public void onClose() {
        done();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}