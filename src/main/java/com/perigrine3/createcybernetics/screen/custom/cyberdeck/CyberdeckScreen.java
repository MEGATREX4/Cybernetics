package com.perigrine3.createcybernetics.screen.custom.cyberdeck;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.macroprogram.MacroprogramItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CyberdeckScreen extends AbstractContainerScreen<CyberdeckMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/cyberdeck.png");

    private static final int TERMINAL_X = 111;
    private static final int TERMINAL_Y = 79;
    private static final int TERMINAL_WIDTH = 48;
    private static final int TERMINAL_HEIGHT = 35;

    private static final int TERMINAL_PADDING = 2;
    private static final float TERMINAL_TEXT_SCALE = 0.5F;
    private static final int TERMINAL_TEXT_COLOR = 0xFF39FF14;
    private static final int TERMINAL_TEXT_DIM = 0xFF1FAF12;
    private static final int TERMINAL_HOVER_COLOR = 0xFFB6FF9F;

    private static final int LIST_ROW_HEIGHT = 6;
    private static final int LIST_VISIBLE_ROWS = 5;

    private static final int DETAIL_HEADER_HEIGHT = 6;
    private static final int DETAIL_LINE_HEIGHT = 5;

    private int listScroll;
    private int detailScroll;
    private int selectedMacroprogramMenuSlot = -1;

    public CyberdeckScreen(CyberdeckMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 199;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderMacroprogramTerminal(guiGraphics, mouseX, mouseY);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderSlotPurposeTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderMacroprogramTerminal(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int screenX = this.leftPos + TERMINAL_X;
        int screenY = this.topPos + TERMINAL_Y;

        guiGraphics.enableScissor(screenX, screenY, screenX + TERMINAL_WIDTH, screenY + TERMINAL_HEIGHT);

        if (selectedMacroprogramMenuSlot >= 0 && getSelectedMacroprogramStack().isEmpty()) {
            selectedMacroprogramMenuSlot = -1;
            detailScroll = 0;
        }

        if (selectedMacroprogramMenuSlot >= 0) {
            renderMacroprogramDetail(guiGraphics, mouseX, mouseY);
        } else {
            renderMacroprogramList(guiGraphics, mouseX, mouseY);
        }

        guiGraphics.disableScissor();
    }

    private void renderMacroprogramList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Integer> installedSlots = getInstalledMacroprogramSlots();

        if (installedSlots.isEmpty()) {
            Component line1 = Component.translatable("gui.createcybernetics.cyberdeck.macroprogram.none.1");
            Component line2 = Component.translatable("gui.createcybernetics.cyberdeck.macroprogram.none.2");

            drawTerminalText(guiGraphics, line1, TERMINAL_PADDING, 10, TERMINAL_TEXT_COLOR);
            drawTerminalText(guiGraphics, line2, TERMINAL_PADDING, 17, TERMINAL_TEXT_COLOR);
            return;
        }

        int maxScroll = Math.max(0, installedSlots.size() - LIST_VISIBLE_ROWS);
        listScroll = Math.min(listScroll, maxScroll);

        int visibleCount = Math.min(LIST_VISIBLE_ROWS, installedSlots.size() - listScroll);

        for (int row = 0; row < visibleCount; row++) {
            int menuSlotIndex = installedSlots.get(listScroll + row);
            ItemStack stack = this.menu.slots.get(menuSlotIndex).getItem();

            int rowY = TERMINAL_PADDING + row * LIST_ROW_HEIGHT;
            boolean hovered = isInsideTerminal(mouseX, mouseY, TERMINAL_PADDING, rowY, TERMINAL_WIDTH - TERMINAL_PADDING * 2, LIST_ROW_HEIGHT);

            String name = truncateTerminalText(stack.getHoverName().getString(), TERMINAL_WIDTH - TERMINAL_PADDING * 2);
            int color = hovered ? TERMINAL_HOVER_COLOR : TERMINAL_TEXT_COLOR;

            drawTerminalText(guiGraphics, Component.literal("> " + name), TERMINAL_PADDING, rowY, color);
        }

        if (installedSlots.size() > LIST_VISIBLE_ROWS) {
            int indicatorX = TERMINAL_WIDTH - 3;
            int trackTop = 2;
            int trackBottom = TERMINAL_HEIGHT - 2;
            int trackHeight = trackBottom - trackTop;

            int thumbHeight = Math.max(4, trackHeight * LIST_VISIBLE_ROWS / installedSlots.size());
            int thumbTravel = trackHeight - thumbHeight;
            int thumbY = trackTop;

            if (maxScroll > 0) {
                thumbY += Math.round(thumbTravel * (listScroll / (float) maxScroll));
            }

            guiGraphics.fill(this.leftPos + TERMINAL_X + indicatorX, this.topPos + TERMINAL_Y + trackTop,
                    this.leftPos + TERMINAL_X + indicatorX + 1, this.topPos + TERMINAL_Y + trackBottom, 0xFF0A4908);

            guiGraphics.fill(this.leftPos + TERMINAL_X + indicatorX, this.topPos + TERMINAL_Y + thumbY,
                    this.leftPos + TERMINAL_X + indicatorX + 1, this.topPos + TERMINAL_Y + thumbY + thumbHeight, TERMINAL_TEXT_COLOR);
        }
    }

    private void renderMacroprogramDetail(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack stack = getSelectedMacroprogramStack();
        if (stack.isEmpty()) return;

        boolean backHovered = isInsideTerminal(mouseX, mouseY, TERMINAL_PADDING, 1, 20, DETAIL_HEADER_HEIGHT);

        drawTerminalText(
                guiGraphics,
                Component.translatable("gui.createcybernetics.cyberdeck.macroprogram.back"),
                TERMINAL_PADDING,
                1,
                backHovered ? TERMINAL_HOVER_COLOR : TERMINAL_TEXT_COLOR
        );

        String name = truncateTerminalText(stack.getHoverName().getString(), TERMINAL_WIDTH - TERMINAL_PADDING * 2);
        drawTerminalText(guiGraphics, Component.literal(name), TERMINAL_PADDING, 7, TERMINAL_HOVER_COLOR);

        Component description = getMacroprogramDescription(stack);
        int logicalWidth = Math.round((TERMINAL_WIDTH - TERMINAL_PADDING * 2) / TERMINAL_TEXT_SCALE);
        List<FormattedCharSequence> lines = this.font.split(description, logicalWidth);

        int visibleLines = 4;
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        detailScroll = Math.min(detailScroll, maxScroll);

        int count = Math.min(visibleLines, lines.size() - detailScroll);

        for (int i = 0; i < count; i++) {
            drawTerminalSequence(guiGraphics, lines.get(detailScroll + i), TERMINAL_PADDING, 14 + i * DETAIL_LINE_HEIGHT, TERMINAL_TEXT_DIM);
        }
    }

    private void renderSlotPurposeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.hoveredSlot == null) return;
        if (this.hoveredSlot.hasItem()) return;

        int menuSlotIndex = this.menu.slots.indexOf(this.hoveredSlot);
        if (menuSlotIndex < 0) return;

        List<Component> tooltip = new ArrayList<>();

        if (menuSlotIndex < CyberdeckMenu.QUICKHACK_SLOT_COUNT) {
            tooltip.add(Component.translatable("gui.createcybernetics.cyberdeck.slot.quickhack"));
            tooltip.add(Component.translatable("gui.createcybernetics.cyberdeck.slot.quickhack.desc"));
        } else if (menuSlotIndex == CyberdeckMenu.MICROPROGRAM_CHIP_SLOT) {
            tooltip.add(Component.translatable("gui.createcybernetics.cyberdeck.slot.microprogram"));
            tooltip.add(Component.translatable("gui.createcybernetics.cyberdeck.slot.microprogram.desc"));
        } else if (menuSlotIndex >= CyberdeckMenu.MACROPROGRAM_SLOT_START && menuSlotIndex < CyberdeckMenu.CYBERDECK_SLOT_COUNT) {
            tooltip.add(Component.translatable("gui.createcybernetics.cyberdeck.slot.macroprogram"));
            tooltip.add(Component.translatable("gui.createcybernetics.cyberdeck.slot.macroprogram.desc"));
        }

        if (!tooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideTerminal(mouseX, mouseY, 0, 0, TERMINAL_WIDTH, TERMINAL_HEIGHT)) {
            if (selectedMacroprogramMenuSlot >= 0) {
                if (isInsideTerminal(mouseX, mouseY, TERMINAL_PADDING, 1, 20, DETAIL_HEADER_HEIGHT)) {
                    selectedMacroprogramMenuSlot = -1;
                    detailScroll = 0;
                    return true;
                }

                return true;
            }

            List<Integer> installedSlots = getInstalledMacroprogramSlots();

            int localY = (int) mouseY - (this.topPos + TERMINAL_Y);
            int row = (localY - TERMINAL_PADDING) / LIST_ROW_HEIGHT;

            if (localY >= TERMINAL_PADDING && row >= 0 && row < LIST_VISIBLE_ROWS) {
                int listIndex = listScroll + row;

                if (listIndex >= 0 && listIndex < installedSlots.size()) {
                    selectedMacroprogramMenuSlot = installedSlots.get(listIndex);
                    detailScroll = 0;
                    return true;
                }
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInsideTerminal(mouseX, mouseY, 0, 0, TERMINAL_WIDTH, TERMINAL_HEIGHT)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (selectedMacroprogramMenuSlot >= 0) {
            ItemStack stack = getSelectedMacroprogramStack();
            if (stack.isEmpty()) return true;

            Component description = getMacroprogramDescription(stack);
            int logicalWidth = Math.round((TERMINAL_WIDTH - TERMINAL_PADDING * 2) / TERMINAL_TEXT_SCALE);
            int lineCount = this.font.split(description, logicalWidth).size();
            int maxScroll = Math.max(0, lineCount - 4);

            if (scrollY < 0.0D) {
                detailScroll = Math.min(maxScroll, detailScroll + 1);
            } else if (scrollY > 0.0D) {
                detailScroll = Math.max(0, detailScroll - 1);
            }

            return true;
        }

        List<Integer> installedSlots = getInstalledMacroprogramSlots();
        int maxScroll = Math.max(0, installedSlots.size() - LIST_VISIBLE_ROWS);

        if (scrollY < 0.0D) {
            listScroll = Math.min(maxScroll, listScroll + 1);
        } else if (scrollY > 0.0D) {
            listScroll = Math.max(0, listScroll - 1);
        }

        return true;
    }

    private List<Integer> getInstalledMacroprogramSlots() {
        List<Integer> installed = new ArrayList<>();

        for (int i = 0; i < CyberdeckMenu.MACROPROGRAM_SLOT_COUNT; i++) {
            int menuSlotIndex = CyberdeckMenu.MACROPROGRAM_SLOT_START + i;

            if (menuSlotIndex < 0 || menuSlotIndex >= this.menu.slots.size()) continue;

            ItemStack stack = this.menu.slots.get(menuSlotIndex).getItem();
            if (stack.isEmpty()) continue;

            installed.add(menuSlotIndex);
        }

        return installed;
    }

    private ItemStack getSelectedMacroprogramStack() {
        if (selectedMacroprogramMenuSlot < CyberdeckMenu.MACROPROGRAM_SLOT_START) return ItemStack.EMPTY;
        if (selectedMacroprogramMenuSlot >= CyberdeckMenu.CYBERDECK_SLOT_COUNT) return ItemStack.EMPTY;
        if (selectedMacroprogramMenuSlot >= this.menu.slots.size()) return ItemStack.EMPTY;

        return this.menu.slots.get(selectedMacroprogramMenuSlot).getItem();
    }

    private Component getMacroprogramDescription(ItemStack stack) {
        if (stack.getItem() instanceof MacroprogramItem macroprogramItem) {
            return macroprogramItem.getDefinition().getDescription();
        }

        return Component.empty();
    }

    private void drawTerminalText(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + TERMINAL_X + x, this.topPos + TERMINAL_Y + y, 0);
        guiGraphics.pose().scale(TERMINAL_TEXT_SCALE, TERMINAL_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawTerminalSequence(GuiGraphics guiGraphics, FormattedCharSequence text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + TERMINAL_X + x, this.topPos + TERMINAL_Y + y, 0);
        guiGraphics.pose().scale(TERMINAL_TEXT_SCALE, TERMINAL_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private String truncateTerminalText(String text, int availablePixels) {
        if (text == null || text.isEmpty()) return "";

        int logicalWidth = Math.round(availablePixels / TERMINAL_TEXT_SCALE);

        if (this.font.width(text) <= logicalWidth) {
            return text;
        }

        String ellipsis = "..";
        int allowed = Math.max(0, logicalWidth - this.font.width(ellipsis));

        return this.font.plainSubstrByWidth(text, allowed) + ellipsis;
    }

    private boolean isInsideTerminal(double mouseX, double mouseY, int x, int y, int width, int height) {
        int screenX = this.leftPos + TERMINAL_X + x;
        int screenY = this.topPos + TERMINAL_Y + y;

        return mouseX >= screenX && mouseX < screenX + width && mouseY >= screenY && mouseY < screenY + height;
    }
}