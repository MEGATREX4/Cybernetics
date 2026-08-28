package com.perigrine3.createcybernetics.client.gui;

import com.perigrine3.createcybernetics.client.CyberdeckRamHud;
import com.perigrine3.createcybernetics.client.HudConfigClient;
import com.perigrine3.createcybernetics.screen.custom.hud.CyberwareHudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class HudLayoutScreen extends Screen {

    private static final int BUTTON_W = 138;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 5;

    private static final int PANEL_PAD = 6;
    private static final int PANEL_W = BUTTON_W + PANEL_PAD * 2;
    private static final int PANEL_BG = 0xAA050505;
    private static final int PANEL_BORDER = 0xFF2A2A2A;
    private static final int PANEL_FOOTER_H = 61;
    private static final int PANEL_SCROLL_STEP = BUTTON_H + BUTTON_GAP;

    private static final int TAB_W = 14;
    private static final int TAB_H = 52;
    private static final int TAB_BG = 0xCC101010;
    private static final int TAB_BORDER = 0xFF555555;

    private static final int SELECTED_BORDER = 0xFF55FF55;
    private static final int HOVER_BORDER = 0xAAFFFFFF;
    private static final int NORMAL_BORDER = 0x66FFFFFF;

    private static final int RAM_RESIZE_HANDLE_SIZE = 7;
    private static final int RAM_RESIZE_HANDLE_COLOR = 0xFFFFFFFF;

    private static final float SCALE_SCROLL_STEP = 0.026666667f;

    private final Screen parent;

    private @Nullable UUID playerId;
    private HudConfigClient.HudConfig working;

    private @Nullable HudConfigClient.HudComponent dragging;
    private @Nullable HudConfigClient.HudComponent selected;

    private boolean ramSelected;
    private boolean ramDragging;
    private boolean ramResizing;

    private int dragOffsetPxX;
    private int dragOffsetPxY;

    private int ramDragOffsetX;
    private int ramDragOffsetY;

    private boolean panelOpen = true;
    private int panelScrollOffset;
    private int panelMaxScrollOffset;
    private boolean savedPulse;
    private int savedPulseTicks;

    private Button btnPanelTab;

    private Button btnBack;
    private Button btnSave;
    private Button btnReset;

    private Button btnHudLayer;
    private Button btnBattery;
    private Button btnCoords;
    private Button btnToggleables;
    private Button btnShards;
    private Button btnTarget;
    private Button btnMinimap;

    public HudLayoutScreen(Screen parent) {
        super(Component.translatable("screen.createcybernetics.hud_layout"));
        this.parent = parent;
        this.working = HudConfigClient.defaultConfig();
    }

    @Override
    protected void init() {
        LocalPlayer player = Minecraft.getInstance().player;
        this.playerId = player != null ? player.getUUID() : null;

        if (this.playerId != null) {
            this.working = HudConfigClient.get(this.playerId).copy();
        } else {
            this.working = HudConfigClient.defaultConfig();
        }

        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        clampRamToScreen();
        clampPanelScroll();
        rebuildWidgets();
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        clampRamToScreen();
        clampPanelScroll();
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        int panelX = panelOpen ? this.width - PANEL_W : this.width;
        int tabX = panelOpen ? panelX - TAB_W : this.width - TAB_W;
        int tabY = (this.height / 2) - (TAB_H / 2);

        btnPanelTab = addRenderableWidget(Button.builder(Component.literal(panelOpen ? ">" : "<"), b -> togglePanel())
                .pos(tabX, tabY)
                .size(TAB_W, TAB_H)
                .build());

        if (!panelOpen) {
            return;
        }

        panelMaxScrollOffset = Math.max(0, panelContentHeight() - panelViewportBottom());
        panelScrollOffset = clampInt(panelScrollOffset, 0, panelMaxScrollOffset);

        int x = panelX + PANEL_PAD;
        int y = PANEL_PAD - panelScrollOffset;

        btnBack = addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnSave = addRenderableWidget(Button.builder(Component.translatable("gui.createcybernetics.save"), b -> save())
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnReset = addRenderableWidget(Button.builder(Component.literal("RESET LAYOUT"), b -> reset())
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP + 8;

        btnHudLayer = addRenderableWidget(Button.builder(hudLayerLabel(), b -> toggleHudLayer())
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnBattery = addRenderableWidget(Button.builder(batteryLabel(), b -> cycleBattery())
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnCoords = addRenderableWidget(Button.builder(coordsLabel(), b -> toggle(HudConfigClient.HudComponent.COORDS))
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnToggleables = addRenderableWidget(Button.builder(toggleablesLabel(), b -> toggle(HudConfigClient.HudComponent.TOGGLE_LIST))
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnShards = addRenderableWidget(Button.builder(shardsLabel(), b -> toggle(HudConfigClient.HudComponent.SHARDS))
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnTarget = addRenderableWidget(Button.builder(targetLabel(), b -> cycleTarget())
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + BUTTON_GAP;

        btnMinimap = addRenderableWidget(Button.builder(minimapLabel(), b -> toggle(HudConfigClient.HudComponent.MINIMAP))
                .pos(x, y)
                .size(BUTTON_W, BUTTON_H)
                .build());

        updatePanelButtonVisibility();
    }

    private void togglePanel() {
        panelOpen = !panelOpen;
        rebuildWidgets();
    }

    private void save() {
        if (playerId == null) return;

        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        clampRamToScreen();
        working.sanitize();
        HudConfigClient.save(playerId, working.copy());

        savedPulse = true;
        savedPulseTicks = 24;
    }

    private void reset() {
        working = HudConfigClient.defaultConfig();
        selected = null;
        dragging = null;
        ramSelected = false;
        ramDragging = false;
        ramResizing = false;
        panelScrollOffset = 0;
        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        clampRamToScreen();
        refreshButtonLabels();
    }

    private void toggleHudLayer() {
        working.hudLayer.enabled = !working.hudLayer.enabled;
        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        refreshButtonLabels();
    }

    private void toggle(HudConfigClient.HudComponent component) {
        HudConfigClient.ComponentLayout layout = working.layout(component);
        layout.enabled = !layout.enabled;
        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        refreshButtonLabels();
    }

    private void cycleBattery() {
        working.battery.enabled = true;

        working.batteryMode = switch (working.batteryMode) {
            case TEXT_ONLY -> HudConfigClient.BatteryMode.ICON_ONLY;
            case ICON_ONLY -> HudConfigClient.BatteryMode.ICON_PLUS_CAPACITY;
            case ICON_PLUS_CAPACITY -> HudConfigClient.BatteryMode.ICON_PLUS_CAPACITY_PLUS_STATS;
            case ICON_PLUS_CAPACITY_PLUS_STATS -> HudConfigClient.BatteryMode.TEXT_ONLY;
        };

        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        refreshButtonLabels();
    }

    private void cycleTarget() {
        working.targetMode = switch (working.targetMode) {
            case ABOVE_HOTBAR -> HudConfigClient.TargetMode.UNDER_CROSSHAIR;
            case UNDER_CROSSHAIR -> HudConfigClient.TargetMode.OFF;
            case OFF -> HudConfigClient.TargetMode.ABOVE_HOTBAR;
        };

        working.target.enabled = working.targetMode != HudConfigClient.TargetMode.OFF;

        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        refreshButtonLabels();
    }

    private void refreshButtonLabels() {
        if (btnHudLayer != null) btnHudLayer.setMessage(hudLayerLabel());
        if (btnBattery != null) btnBattery.setMessage(batteryLabel());
        if (btnCoords != null) btnCoords.setMessage(coordsLabel());
        if (btnToggleables != null) btnToggleables.setMessage(toggleablesLabel());
        if (btnShards != null) btnShards.setMessage(shardsLabel());
        if (btnTarget != null) btnTarget.setMessage(targetLabel());
        if (btnMinimap != null) btnMinimap.setMessage(minimapLabel());
    }

    private Component hudLayerLabel() {
        return Component.literal("HUD FRAME: " + onOff(working.hudLayer.enabled));
    }

    private Component batteryLabel() {
        return Component.literal("BATTERY: " + switch (working.batteryMode) {
            case TEXT_ONLY -> "TEXT";
            case ICON_ONLY -> "ICON";
            case ICON_PLUS_CAPACITY -> "ICON+CAP";
            case ICON_PLUS_CAPACITY_PLUS_STATS -> "FULL";
        });
    }

    private Component coordsLabel() {
        return Component.literal("COORDS: " + onOff(working.coords.enabled));
    }

    private Component toggleablesLabel() {
        return Component.literal("TOGGLES: " + onOff(working.toggleList.enabled));
    }

    private Component shardsLabel() {
        return Component.literal("CHIPWARE: " + onOff(working.shards.enabled));
    }

    private Component targetLabel() {
        return Component.literal("TARGET: " + switch (working.targetMode) {
            case ABOVE_HOTBAR -> "HOTBAR";
            case UNDER_CROSSHAIR -> "CROSSHAIR";
            case OFF -> "OFF";
        });
    }

    private Component minimapLabel() {
        return Component.literal("MINIMAP: " + onOff(working.minimap.enabled));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void tick() {
        super.tick();

        if (savedPulse) {
            savedPulseTicks--;
            if (savedPulseTicks <= 0) {
                savedPulse = false;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        fitWorkingToScreen();
        clampWorkingToScreenBounds();
        clampRamToScreen();

        CyberwareHudLayer.renderHudLayerPreview(gg, partialTick, working);
        CyberwareHudLayer.renderHudPreview(gg, partialTick, working);
        CyberdeckRamHud.renderPreview(gg, working);

        renderComponentOutlines(gg, mouseX, mouseY);
        renderRamOutline(gg, mouseX, mouseY);
        renderPanel(gg);

        super.render(gg, mouseX, mouseY, partialTick);

        if (panelOpen) {
            int titleX = this.width - PANEL_W + PANEL_W / 2;
            int titleY = this.height - 49;

            gg.drawCenteredString(this.font, Component.literal("Drag HUD blocks"), titleX, titleY, 0xFFFFFF);
            gg.drawCenteredString(this.font, Component.literal("Wheel = resize"), titleX, titleY + 11, 0xAAAAAA);
            gg.drawCenteredString(this.font, Component.literal("RAM corner = reshape"), titleX, titleY + 22, 0xAAAAAA);

            if (savedPulse) {
                gg.drawCenteredString(this.font, Component.literal("Saved"), titleX, titleY + 33, 0x55FF55);
            }
        }
    }

    private void renderPanel(GuiGraphics gg) {
        int tabX = panelOpen ? this.width - PANEL_W - TAB_W : this.width - TAB_W;
        int tabY = (this.height / 2) - (TAB_H / 2);

        gg.fill(tabX, tabY, tabX + TAB_W, tabY + TAB_H, TAB_BG);
        gg.fill(tabX, tabY, tabX + TAB_W, tabY + 1, TAB_BORDER);
        gg.fill(tabX, tabY + TAB_H - 1, tabX + TAB_W, tabY + TAB_H, TAB_BORDER);
        gg.fill(tabX, tabY, tabX + 1, tabY + TAB_H, TAB_BORDER);
        gg.fill(tabX + TAB_W - 1, tabY, tabX + TAB_W, tabY + TAB_H, TAB_BORDER);

        if (!panelOpen) {
            return;
        }

        int x0 = this.width - PANEL_W;
        int x1 = this.width;

        gg.fill(x0, 0, x1, this.height, PANEL_BG);
        gg.fill(x0, 0, x0 + 1, this.height, PANEL_BORDER);

        renderPanelScrollbar(gg);
    }

    private int panelViewportBottom() {
        return Math.max(PANEL_PAD, this.height - PANEL_FOOTER_H);
    }

    private int panelContentHeight() {
        return PANEL_PAD + (BUTTON_H * 10) + (BUTTON_GAP * 9) + 8;
    }

    private void clampPanelScroll() {
        panelMaxScrollOffset = Math.max(0, panelContentHeight() - panelViewportBottom());
        panelScrollOffset = clampInt(panelScrollOffset, 0, panelMaxScrollOffset);
    }

    private void updatePanelButtonVisibility() {
        int viewportTop = 0;
        int viewportBottom = panelViewportBottom();

        setPanelButtonVisible(btnBack, viewportTop, viewportBottom);
        setPanelButtonVisible(btnSave, viewportTop, viewportBottom);
        setPanelButtonVisible(btnReset, viewportTop, viewportBottom);
        setPanelButtonVisible(btnHudLayer, viewportTop, viewportBottom);
        setPanelButtonVisible(btnBattery, viewportTop, viewportBottom);
        setPanelButtonVisible(btnCoords, viewportTop, viewportBottom);
        setPanelButtonVisible(btnToggleables, viewportTop, viewportBottom);
        setPanelButtonVisible(btnShards, viewportTop, viewportBottom);
        setPanelButtonVisible(btnTarget, viewportTop, viewportBottom);
        setPanelButtonVisible(btnMinimap, viewportTop, viewportBottom);
    }

    private static void setPanelButtonVisible(@Nullable Button button, int viewportTop, int viewportBottom) {
        if (button == null) return;

        button.visible = button.getY() >= viewportTop && button.getY() + button.getHeight() <= viewportBottom;
        button.active = button.visible;
    }

    private void renderPanelScrollbar(GuiGraphics gg) {
        if (panelMaxScrollOffset <= 0) return;

        int trackX = this.width - 3;
        int trackTop = PANEL_PAD;
        int trackBottom = panelViewportBottom() - PANEL_PAD;
        int trackHeight = Math.max(1, trackBottom - trackTop);

        gg.fill(trackX, trackTop, trackX + 1, trackBottom, 0x66555555);

        int viewportHeight = Math.max(1, panelViewportBottom());
        int thumbHeight = Math.max(8, Math.round(trackHeight * viewportHeight / (float) panelContentHeight()));
        int availableTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackTop + Math.round(availableTravel * panelScrollOffset / (float) panelMaxScrollOffset);

        gg.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    private void renderComponentOutlines(GuiGraphics gg, int mouseX, int mouseY) {
        CyberwareHudLayer.HudWidgetRects rects = CyberwareHudLayer.computeRectsForConfig(Minecraft.getInstance(), working);

        for (HudConfigClient.HudComponent component : HudConfigClient.HudComponent.values()) {
            if (!working.enabled(component)) continue;

            CyberwareHudLayer.HudRect rect = rects.rect(component);
            if (rect == null) continue;

            int color;

            if (component == selected && !ramSelected) {
                color = SELECTED_BORDER;
            } else if (rect.containsGui(mouseX, mouseY, Minecraft.getInstance())) {
                color = HOVER_BORDER;
            } else {
                color = NORMAL_BORDER;
            }

            drawGuiRectOutline(gg, rect, color);
        }
    }

    private static void drawGuiRectOutline(GuiGraphics gg, CyberwareHudLayer.HudRect rect, int color) {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();

        int x = Math.round((float) (rect.x() / guiScale));
        int y = Math.round((float) (rect.y() / guiScale));
        int w = Math.max(1, Math.round((float) (rect.w() / guiScale)));
        int h = Math.max(1, Math.round((float) (rect.h() / guiScale)));

        gg.fill(x, y, x + w, y + 1, color);
        gg.fill(x, y + h - 1, x + w, y + h, color);
        gg.fill(x, y, x + 1, y + h, color);
        gg.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void renderRamOutline(GuiGraphics gg, int mouseX, int mouseY) {
        RamRect rect = getRamRect();

        int color;

        if (ramSelected) {
            color = SELECTED_BORDER;
        } else if (rect.contains(mouseX, mouseY)) {
            color = HOVER_BORDER;
        } else {
            color = NORMAL_BORDER;
        }

        gg.fill(rect.x, rect.y, rect.x + rect.width, rect.y + 1, color);
        gg.fill(rect.x, rect.y + rect.height - 1, rect.x + rect.width, rect.y + rect.height, color);
        gg.fill(rect.x, rect.y, rect.x + 1, rect.y + rect.height, color);
        gg.fill(rect.x + rect.width - 1, rect.y, rect.x + rect.width, rect.y + rect.height, color);

        RamRect handle = getRamResizeHandle(rect);

        gg.fill(handle.x, handle.y, handle.x + handle.width, handle.y + handle.height, RAM_RESIZE_HANDLE_COLOR);
    }

    private RamRect getRamRect() {
        return new RamRect(
                working.ram.pixelX(this.width),
                working.ram.pixelY(this.height),
                working.ram.pixelWidth(this.width),
                working.ram.pixelHeight(this.height)
        );
    }

    private static RamRect getRamResizeHandle(RamRect rect) {
        int size = Math.min(RAM_RESIZE_HANDLE_SIZE, Math.min(rect.width, rect.height));

        return new RamRect(rect.x + rect.width - size, rect.y + rect.height - size, size, size);
    }

    private boolean mouseIsOverSlidePanelArea(double mouseX, double mouseY) {
        if (panelOpen) {
            int panelX = this.width - PANEL_W;
            int tabX = panelX - TAB_W;
            return mouseX >= tabX && mouseX < this.width && mouseY >= 0 && mouseY < this.height;
        }

        int tabX = this.width - TAB_W;
        int tabY = (this.height / 2) - (TAB_H / 2);

        return mouseX >= tabX && mouseX < tabX + TAB_W && mouseY >= tabY && mouseY < tabY + TAB_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseIsOverSlidePanelArea(mouseX, mouseY)) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            return panelOpen;
        }

        if (button == 0) {
            RamRect ramRect = getRamRect();
            RamRect resizeHandle = getRamResizeHandle(ramRect);

            if (resizeHandle.contains(mouseX, mouseY)) {
                ramSelected = true;
                ramResizing = true;
                ramDragging = false;
                selected = null;
                dragging = null;
                return true;
            }

            if (ramRect.contains(mouseX, mouseY)) {
                ramSelected = true;
                ramDragging = true;
                ramResizing = false;
                selected = null;
                dragging = null;

                ramDragOffsetX = (int) Math.round(mouseX) - ramRect.x;
                ramDragOffsetY = (int) Math.round(mouseY) - ramRect.y;

                return true;
            }

            HudConfigClient.HudComponent hit = findHitComponent(mouseX, mouseY);

            if (hit != null) {
                ramSelected = false;
                ramDragging = false;
                ramResizing = false;

                selected = hit;
                dragging = hit;

                Minecraft mc = Minecraft.getInstance();
                double guiScale = mc.getWindow().getGuiScale();

                int mousePxX = Math.round((float) (mouseX * guiScale));
                int mousePxY = Math.round((float) (mouseY * guiScale));

                int screenPxW = mc.getWindow().getScreenWidth();
                int screenPxH = mc.getWindow().getScreenHeight();

                if (hit == HudConfigClient.HudComponent.HUD_LEFT) {
                    dragOffsetPxX = mousePxX - working.hudLayer.leftPixelX(screenPxW);
                    dragOffsetPxY = mousePxY - working.hudLayer.pixelY(screenPxH);
                } else if (hit == HudConfigClient.HudComponent.HUD_RIGHT) {
                    dragOffsetPxX = mousePxX - working.hudLayer.rightPixelX(screenPxW);
                    dragOffsetPxY = mousePxY - working.hudLayer.pixelY(screenPxH);
                } else {
                    HudConfigClient.ComponentLayout layout = working.layout(hit);
                    dragOffsetPxX = mousePxX - layout.pixelX(screenPxW);
                    dragOffsetPxY = mousePxY - layout.pixelY(screenPxH);
                }

                return true;
            }

            ramSelected = false;
            selected = null;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && ramResizing) {
            resizeRam((int) Math.round(mouseX), (int) Math.round(mouseY));
            return true;
        }

        if (button == 0 && ramDragging) {
            RamRect rect = getRamRect();

            int newX = (int) Math.round(mouseX) - ramDragOffsetX;
            int newY = (int) Math.round(mouseY) - ramDragOffsetY;

            newX = clampInt(newX, 0, Math.max(0, this.width - rect.width));
            newY = clampInt(newY, 0, Math.max(0, this.height - rect.height));

            working.ram.setPositionFromPixel(newX, newY, this.width, this.height);
            return true;
        }

        if (button == 0 && dragging != null) {
            Minecraft mc = Minecraft.getInstance();
            double guiScale = mc.getWindow().getGuiScale();

            int mousePxX = Math.round((float) (mouseX * guiScale));
            int mousePxY = Math.round((float) (mouseY * guiScale));

            int screenPxW = mc.getWindow().getScreenWidth();
            int screenPxH = mc.getWindow().getScreenHeight();

            if (dragging == HudConfigClient.HudComponent.HUD_LEFT) {
                working.hudLayer.setLeftFromPixel(mousePxX - dragOffsetPxX, screenPxW);
                working.hudLayer.setYFromPixel(mousePxY - dragOffsetPxY, screenPxH);
            } else if (dragging == HudConfigClient.HudComponent.HUD_RIGHT) {
                working.hudLayer.setRightFromPixel(mousePxX - dragOffsetPxX, screenPxW);
                working.hudLayer.setYFromPixel(mousePxY - dragOffsetPxY, screenPxH);
            } else {
                HudConfigClient.ComponentLayout layout = working.layout(dragging);
                layout.setFromPixel(mousePxX - dragOffsetPxX, mousePxY - dragOffsetPxY, screenPxW, screenPxH);
            }

            clampWorkingToScreenBounds();
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void resizeRam(int mouseX, int mouseY) {
        RamRect current = getRamRect();

        int maxWidth = Math.max(1, this.width - current.x);
        int maxHeight = Math.max(1, this.height - current.y);

        int requestedWidth = clampInt(mouseX - current.x, CyberdeckRamHud.ICON_SIZE, maxWidth);

        int minimumBaseHeight = CyberdeckRamHud.minimumHeightForRows(Minecraft.getInstance(), 1);
        int requestedHeight = clampInt(mouseY - current.y, minimumBaseHeight, maxHeight);

        int maxRam = Math.max(1, CyberdeckRamHud.getPreviewMaxRam());

        int maxRowsForRequestedHeight = Math.max(1, (requestedHeight - this.font.lineHeight - CyberdeckRamHud.TITLE_ICON_GAP + CyberdeckRamHud.ICON_SPACING) / CyberdeckRamHud.ICON_STEP);

        int requiredColumns = Mth.ceil(maxRam / (float) maxRowsForRequestedHeight);
        int requiredWidth = CyberdeckRamHud.minimumWidthForColumns(requiredColumns);

        int finalWidth = Math.max(requestedWidth, requiredWidth);

        if (finalWidth > maxWidth) {
            finalWidth = maxWidth;
        }

        int columns = Math.max(1, (finalWidth + CyberdeckRamHud.ICON_SPACING) / CyberdeckRamHud.ICON_STEP);
        int requiredRows = Mth.ceil(maxRam / (float) columns);
        int requiredHeight = CyberdeckRamHud.minimumHeightForRows(Minecraft.getInstance(), requiredRows);

        int finalHeight = Math.max(requestedHeight, requiredHeight);

        if (finalHeight > maxHeight) {
            finalHeight = maxHeight;

            int rowsThatFit = Math.max(1, (finalHeight - this.font.lineHeight - CyberdeckRamHud.TITLE_ICON_GAP + CyberdeckRamHud.ICON_SPACING) / CyberdeckRamHud.ICON_STEP);
            int columnsNeeded = Mth.ceil(maxRam / (float) rowsThatFit);
            finalWidth = Math.max(finalWidth, CyberdeckRamHud.minimumWidthForColumns(columnsNeeded));
            finalWidth = Math.min(finalWidth, maxWidth);
        }

        working.ram.setSizeFromPixel(finalWidth, finalHeight, this.width, this.height);
        clampRamToScreen();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (ramDragging || ramResizing)) {
            ramDragging = false;
            ramResizing = false;
            clampRamToScreen();
            return true;
        }

        if (button == 0 && dragging != null) {
            dragging = null;
            clampWorkingToScreenBounds();
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panelOpen && mouseIsOverSlidePanelArea(mouseX, mouseY)) {
            if (scrollY != 0.0D && panelMaxScrollOffset > 0) {
                panelScrollOffset = clampInt(panelScrollOffset - (int) Math.signum(scrollY) * PANEL_SCROLL_STEP, 0, panelMaxScrollOffset);
                rebuildWidgets();
            }

            return true;
        }

        if (mouseIsOverSlidePanelArea(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        RamRect ramRect = getRamRect();

        if (ramRect.contains(mouseX, mouseY)) {
            ramSelected = true;
            selected = null;
            return true;
        }

        HudConfigClient.HudComponent hovered = findHitComponent(mouseX, mouseY);

        if (hovered == HudConfigClient.HudComponent.TOGGLE_LIST && scrollY != 0.0D) {
            CyberwareHudLayer.scrollToggleSidebar(scrollY);
            selected = hovered;
            ramSelected = false;
            return true;
        }

        HudConfigClient.HudComponent target = selected != null && !ramSelected ? selected : hovered;

        if (target != null) {
            ramSelected = false;

            if (target == HudConfigClient.HudComponent.HUD_LEFT || target == HudConfigClient.HudComponent.HUD_RIGHT) {
                working.hudLayer.adjustScale((float) scrollY * SCALE_SCROLL_STEP);
            } else {
                HudConfigClient.ComponentLayout layout = working.layout(target);
                layout.adjustScale((float) scrollY * SCALE_SCROLL_STEP);
            }

            fitWorkingToScreen();
            clampWorkingToScreenBounds();
            selected = target;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private @Nullable HudConfigClient.HudComponent findHitComponent(double mouseX, double mouseY) {
        CyberwareHudLayer.HudWidgetRects rects = CyberwareHudLayer.computeRectsForConfig(Minecraft.getInstance(), working);

        HudConfigClient.HudComponent[] order = {
                HudConfigClient.HudComponent.MINIMAP,
                HudConfigClient.HudComponent.TARGET,
                HudConfigClient.HudComponent.SHARDS,
                HudConfigClient.HudComponent.TOGGLE_LIST,
                HudConfigClient.HudComponent.COORDS,
                HudConfigClient.HudComponent.BATTERY,
                HudConfigClient.HudComponent.HUD_RIGHT,
                HudConfigClient.HudComponent.HUD_LEFT
        };

        for (HudConfigClient.HudComponent component : order) {
            if (!working.enabled(component)) continue;

            CyberwareHudLayer.HudRect rect = rects.rect(component);
            if (rect != null && rect.containsGui(mouseX, mouseY, Minecraft.getInstance())) {
                return component;
            }
        }

        return null;
    }

    private void fitWorkingToScreen() {
        Minecraft mc = Minecraft.getInstance();
        int screenPxW = mc.getWindow().getScreenWidth();
        int screenPxH = mc.getWindow().getScreenHeight();

        fitHudLayerToScreen(screenPxW, screenPxH);
        fitComponentToScreen(HudConfigClient.HudComponent.BATTERY, screenPxW, screenPxH);
        fitComponentToScreen(HudConfigClient.HudComponent.COORDS, screenPxW, screenPxH);
        fitComponentToScreen(HudConfigClient.HudComponent.TOGGLE_LIST, screenPxW, screenPxH);
        fitComponentToScreen(HudConfigClient.HudComponent.SHARDS, screenPxW, screenPxH);
        fitComponentToScreen(HudConfigClient.HudComponent.TARGET, screenPxW, screenPxH);
        fitComponentToScreen(HudConfigClient.HudComponent.MINIMAP, screenPxW, screenPxH);
    }

    private void fitComponentToScreen(HudConfigClient.HudComponent component, int screenPxW, int screenPxH) {
        if (!working.enabled(component)) return;
        if (component == HudConfigClient.HudComponent.HUD_LEFT || component == HudConfigClient.HudComponent.HUD_RIGHT) return;

        HudConfigClient.ComponentLayout layout = working.layout(component);

        for (int attempts = 0; attempts < 256; attempts++) {
            CyberwareHudLayer.HudRect rect = CyberwareHudLayer.computeRectsForConfig(Minecraft.getInstance(), working).rect(component);
            if (rect == null) return;
            if (rect.w() <= screenPxW && rect.h() <= screenPxH) return;

            float previousScale = layout.scale;
            layout.adjustScale(-SCALE_SCROLL_STEP);

            if (layout.scale == previousScale) return;
        }
    }

    private void fitHudLayerToScreen(int screenPxW, int screenPxH) {
        if (!working.hudLayer.enabled) return;

        for (int attempts = 0; attempts < 256; attempts++) {
            CyberwareHudLayer.HudWidgetRects rects = CyberwareHudLayer.computeRectsForConfig(Minecraft.getInstance(), working);
            CyberwareHudLayer.HudRect left = rects.rect(HudConfigClient.HudComponent.HUD_LEFT);
            CyberwareHudLayer.HudRect right = rects.rect(HudConfigClient.HudComponent.HUD_RIGHT);

            boolean leftFits = left == null || left.w() <= screenPxW && left.h() <= screenPxH;
            boolean rightFits = right == null || right.w() <= screenPxW && right.h() <= screenPxH;

            if (leftFits && rightFits) return;

            float previousScale = working.hudLayer.scale;
            working.hudLayer.adjustScale(-SCALE_SCROLL_STEP);

            if (working.hudLayer.scale == previousScale) return;
        }
    }

    private void clampWorkingToScreenBounds() {
        Minecraft mc = Minecraft.getInstance();
        int screenPxW = mc.getWindow().getScreenWidth();
        int screenPxH = mc.getWindow().getScreenHeight();

        clampHudLayerToScreen(screenPxW, screenPxH);
        clampComponentToScreen(HudConfigClient.HudComponent.BATTERY, screenPxW, screenPxH);
        clampComponentToScreen(HudConfigClient.HudComponent.COORDS, screenPxW, screenPxH);
        clampComponentToScreen(HudConfigClient.HudComponent.TOGGLE_LIST, screenPxW, screenPxH);
        clampComponentToScreen(HudConfigClient.HudComponent.SHARDS, screenPxW, screenPxH);
        clampComponentToScreen(HudConfigClient.HudComponent.TARGET, screenPxW, screenPxH);
        clampComponentToScreen(HudConfigClient.HudComponent.MINIMAP, screenPxW, screenPxH);
    }

    private void clampComponentToScreen(HudConfigClient.HudComponent component, int screenPxW, int screenPxH) {
        if (!working.enabled(component)) return;
        if (component == HudConfigClient.HudComponent.HUD_LEFT || component == HudConfigClient.HudComponent.HUD_RIGHT) return;

        CyberwareHudLayer.HudWidgetRects rects = CyberwareHudLayer.computeRectsForConfig(Minecraft.getInstance(), working);
        CyberwareHudLayer.HudRect rect = rects.rect(component);
        if (rect == null) return;

        int clampedX = clampInt(rect.x(), 0, Math.max(0, screenPxW - rect.w()));
        int clampedY = clampInt(rect.y(), 0, Math.max(0, screenPxH - rect.h()));

        if (clampedX == rect.x() && clampedY == rect.y()) return;

        HudConfigClient.ComponentLayout layout = working.layout(component);
        layout.setFromPixel(clampedX, clampedY, screenPxW, screenPxH);
    }

    private void clampHudLayerToScreen(int screenPxW, int screenPxH) {
        if (!working.hudLayer.enabled) return;

        CyberwareHudLayer.HudWidgetRects rects = CyberwareHudLayer.computeRectsForConfig(Minecraft.getInstance(), working);

        CyberwareHudLayer.HudRect left = rects.rect(HudConfigClient.HudComponent.HUD_LEFT);
        CyberwareHudLayer.HudRect right = rects.rect(HudConfigClient.HudComponent.HUD_RIGHT);

        if (left != null) {
            int clampedLeftX = clampInt(left.x(), 0, Math.max(0, screenPxW - left.w()));
            working.hudLayer.setLeftFromPixel(clampedLeftX, screenPxW);
        }

        if (right != null) {
            int clampedRightX = clampInt(right.x(), 0, Math.max(0, screenPxW - right.w()));
            working.hudLayer.setRightFromPixel(clampedRightX, screenPxW);
        }

        CyberwareHudLayer.HudRect ySource = left != null ? left : right;

        if (ySource != null) {
            int clampedY = clampInt(ySource.y(), 0, Math.max(0, screenPxH - ySource.h()));
            working.hudLayer.setYFromPixel(clampedY, screenPxH);
        }
    }

    private void clampRamToScreen() {
        working.ram.sanitize();

        int x = working.ram.pixelX(this.width);
        int y = working.ram.pixelY(this.height);
        int w = working.ram.pixelWidth(this.width);
        int h = working.ram.pixelHeight(this.height);

        w = Math.min(w, this.width);
        h = Math.min(h, this.height);

        x = clampInt(x, 0, Math.max(0, this.width - w));
        y = clampInt(y, 0, Math.max(0, this.height - h));

        working.ram.setBoundsFromPixel(x, y, w, h, this.width, this.height);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private static final class RamRect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private RamRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }
    }
}