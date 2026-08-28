package com.perigrine3.createcybernetics.screen.custom.computer;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.computer.ChatSpaceClientData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.generic.GameShardItem;
import com.perigrine3.createcybernetics.item.generic.InfologDataShardItem;
import com.perigrine3.createcybernetics.item.generic.InfologTextData;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceAddContactPayload;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceMarkReadPayload;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceSendMessagePayload;
import com.perigrine3.createcybernetics.screen.custom.chipware.InfologReadScreen;
import com.perigrine3.createcybernetics.screen.custom.computer.game.MinesweeperScreen;
import com.perigrine3.createcybernetics.screen.custom.computer.game.chess.ChessLauncherScreen;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ComputerScreen extends AbstractContainerScreen<ComputerMenu> {
    private static final ResourceLocation CONNECTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/computer/computer_gui.png");
    private static final ResourceLocation DISCONNECTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/computer/computer_gui_disconnected.png");
    private static final ResourceLocation FOLDER_ICON = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/computer/folder_icon.png");
    private static final ResourceLocation SETTINGS_ICON = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/computer/settings_icon.png");
    private static final ResourceLocation FILE_ICON = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/computer/file_icon.png");
    private static final ResourceLocation EXE_ICON = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/computer/exe_icon.png");
    private static final ResourceLocation CURSOR_ICON = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/computer/cursor.png");

    private static final int BACKGROUND_TEXTURE_WIDTH = 512;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 512;

    private static final int ICON_TEXTURE_WIDTH = 64;
    private static final int ICON_TEXTURE_HEIGHT = 64;

    private static final int CURSOR_TEXTURE_WIDTH = 34;
    private static final int CURSOR_TEXTURE_HEIGHT = 34;

    private static final float BACKGROUND_SCALE = 0.5F;

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 256;

    private static final int SCREEN_X = 17;
    private static final int SCREEN_Y = 17;
    private static final int SCREEN_WIDTH = 224;
    private static final int SCREEN_HEIGHT = 193;

    private static final int SCREEN_RIGHT = SCREEN_X + SCREEN_WIDTH;
    private static final int SCREEN_BOTTOM = SCREEN_Y + SCREEN_HEIGHT;

    private static final int CONTENT_PADDING_X = 8;
    private static final int CONTENT_PADDING_TOP = 6;
    private static final int CONTENT_PADDING_BOTTOM = 7;

    private static final int CONTENT_X = SCREEN_X + CONTENT_PADDING_X;
    private static final int CONTENT_Y = SCREEN_Y + CONTENT_PADDING_TOP;
    private static final int CONTENT_RIGHT = SCREEN_RIGHT - CONTENT_PADDING_X;
    private static final int CONTENT_BOTTOM = SCREEN_BOTTOM - CONTENT_PADDING_BOTTOM;

    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    private static final int CURSOR_WIDTH = 11;
    private static final int CURSOR_HEIGHT = 11;

    private static final float LABEL_SCALE = 0.5F;
    private static final int LABEL_RENDERED_WIDTH = 35;
    private static final int LABEL_WRAP_WIDTH = Math.round(LABEL_RENDERED_WIDTH / LABEL_SCALE);

    private static final int MAX_LABEL_LINES = 3;
    private static final int LABEL_LINE_HEIGHT = 9;
    private static final int ICON_TO_LABEL_GAP = 2;

    private static final int ICON_CLICK_WIDTH = 37;
    private static final int ICON_CLICK_HEIGHT = 33;

    private static final int NEON_GREEN = 0xFF39FF14;
    private static final int NEON_GREEN_DIM = 0xFF1BAA0A;
    private static final int NEON_GREEN_DARK = 0xFF0A3A05;
    private static final int BLACK = 0xFF000000;
    private static final int PANEL_BLACK = 0xDD000000;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int DISABLED_COLOR = 0xFF777777;

    private static final int HEADER_X = CONTENT_X;
    private static final int HEADER_Y = CONTENT_Y;

    private static final int DESKTOP_START_X = CONTENT_X + 9;
    private static final int DESKTOP_ICON_Y = CONTENT_Y + 25;

    private static final int FILE_GRID_COLUMNS = 4;
    private static final int FILE_GRID_START_X = CONTENT_X + 14;
    private static final int FILE_GRID_START_Y = CONTENT_Y + 24;
    private static final int FILE_GRID_SPACING_X = 52;
    private static final int FILE_GRID_SPACING_Y = 26;

    private static final int BACK_BUTTON_X = CONTENT_X;
    private static final int BACK_BUTTON_Y = CONTENT_BOTTOM - 14;
    private static final int BACK_BUTTON_WIDTH = 42;
    private static final int BACK_BUTTON_HEIGHT = 13;

    private static final int SYNC_INTERVAL_TICKS = 40;

    private static final int CHAT_INPUT_Y = CONTENT_Y + 22;
    private static final int CHAT_CODE_X = CONTENT_X;
    private static final int CHAT_CODE_WIDTH = 47;
    private static final int CHAT_NAME_X = CHAT_CODE_X + CHAT_CODE_WIDTH + 3;
    private static final int CHAT_NAME_WIDTH = 83;
    private static final int CHAT_ADD_BUTTON_X = CHAT_NAME_X + CHAT_NAME_WIDTH + 3;
    private static final int CHAT_ADD_BUTTON_WIDTH = 45;
    private static final int CHAT_INPUT_HEIGHT = 14;

    private static final int CONTACT_LIST_X = CONTENT_X;
    private static final int CONTACT_LIST_Y = CHAT_INPUT_Y + CHAT_INPUT_HEIGHT + 6;
    private static final int CONTACT_LIST_WIDTH = CONTENT_RIGHT - CONTENT_X;
    private static final int CONTACT_ROW_HEIGHT = 20;
    private static final int CONTACT_VISIBLE_ROWS = 6;

    private static final int CONVERSATION_X = CONTENT_X;
    private static final int CONVERSATION_Y = CONTENT_Y + 22;
    private static final int CONVERSATION_WIDTH = CONTENT_RIGHT - CONTENT_X;
    private static final int CONVERSATION_HEIGHT = 124;

    private static final int MESSAGE_INPUT_X = CONTENT_X;
    private static final int MESSAGE_INPUT_Y = CONTENT_BOTTOM - 33;
    private static final int MESSAGE_INPUT_WIDTH = 160;
    private static final int MESSAGE_INPUT_HEIGHT = 14;

    private static final int SEND_BUTTON_X = MESSAGE_INPUT_X + MESSAGE_INPUT_WIDTH + 4;
    private static final int SEND_BUTTON_WIDTH = CONTENT_RIGHT - SEND_BUTTON_X;

    private static final int NOTIFICATION_WIDTH = 102;
    private static final int NOTIFICATION_HEIGHT = 27;

    private final List<ComputerIcon> visibleIcons = new ArrayList<>();

    private Page currentPage = Page.DESKTOP;
    private ChatPage chatPage = ChatPage.CONTACTS;

    private EditBox contactCodeBox;
    private EditBox contactNameBox;
    private EditBox messageBox;

    private String selectedRemoteCode = "";

    private int contactScrollOffset;
    private int messageScrollOffset;
    private int syncTicker;

    private boolean customCursorActive;

    public ComputerScreen(ComputerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        currentPage = Page.DESKTOP;
        chatPage = ChatPage.CONTACTS;

        selectedRemoteCode = "";
        contactScrollOffset = 0;
        messageScrollOffset = 0;
        syncTicker = 0;

        createChatWidgets();
        updateChatWidgetVisibility();

        refreshVisibleIcons();
        hideSystemCursor();

        requestChatSync();
    }

    private void createChatWidgets() {
        contactCodeBox = new EditBox(font, leftPos + CHAT_CODE_X, topPos + CHAT_INPUT_Y, CHAT_CODE_WIDTH, CHAT_INPUT_HEIGHT, Component.translatable("gui.createcybernetics.computer.chatspace.code"));
        contactCodeBox.setMaxLength(5);
        contactCodeBox.setHint(Component.translatable("gui.createcybernetics.computer.chatspace.code_hint"));
        addRenderableWidget(contactCodeBox);

        contactNameBox = new EditBox(font, leftPos + CHAT_NAME_X, topPos + CHAT_INPUT_Y, CHAT_NAME_WIDTH, CHAT_INPUT_HEIGHT, Component.translatable("gui.createcybernetics.computer.chatspace.name"));
        contactNameBox.setMaxLength(32);
        contactNameBox.setHint(Component.translatable("gui.createcybernetics.computer.chatspace.name_hint"));
        addRenderableWidget(contactNameBox);

        messageBox = new EditBox(font, leftPos + MESSAGE_INPUT_X, topPos + MESSAGE_INPUT_Y, MESSAGE_INPUT_WIDTH, MESSAGE_INPUT_HEIGHT, Component.translatable("gui.createcybernetics.computer.chatspace.message"));
        messageBox.setMaxLength(512);
        messageBox.setHint(Component.translatable("gui.createcybernetics.computer.chatspace.message_hint"));
        addRenderableWidget(messageBox);
    }

    private void updateChatWidgetVisibility() {
        boolean onChatSpace = currentPage == Page.CHATSPACE;
        boolean contactsVisible = onChatSpace && chatPage == ChatPage.CONTACTS;
        boolean conversationVisible = onChatSpace && chatPage == ChatPage.CONVERSATION;

        contactCodeBox.visible = contactsVisible;
        contactCodeBox.active = contactsVisible;

        contactNameBox.visible = contactsVisible;
        contactNameBox.active = contactsVisible;

        messageBox.visible = conversationVisible;
        messageBox.active = conversationVisible;

        if (!contactsVisible) {
            contactCodeBox.setFocused(false);
            contactNameBox.setFocused(false);
        }

        if (!conversationVisible) {
            messageBox.setFocused(false);
        }
    }

    private void hideSystemCursor() {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        customCursorActive = true;
    }

    private void restoreSystemCursor() {
        if (!customCursorActive) {
            return;
        }

        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        customCursorActive = false;
    }

    @Override
    public void removed() {
        restoreSystemCursor();
        super.removed();
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        syncTicker++;

        if (syncTicker >= SYNC_INTERVAL_TICKS) {
            syncTicker = 0;
            requestChatSync();
        }

        clampContactScroll();
        clampMessageScroll();
    }

    private void requestChatSync() {
        PacketDistributor.sendToServer(new ChatSpaceRequestSyncPayload(menu.getComputerPos()));
    }

    private void refreshVisibleIcons() {
        visibleIcons.clear();

        switch (currentPage) {
            case DESKTOP -> buildDesktopIcons();
            case TUTORIALS -> buildShardIcons(stack -> stack.is(ModTags.Items.TUTORIAL_SHARDS), FILE_ICON);
            case INFOLOGS -> buildShardIcons(stack -> stack.is(ModTags.Items.LORE_SHARDS) || stack.is(ModItems.DATA_SHARD_INFOLOG.get()), FILE_ICON);
            case GAMES -> buildShardIcons(stack -> stack.is(ModTags.Items.GAME_SHARDS), EXE_ICON);
            case CHATSPACE, IDE, SETTINGS, SERVER_STATS -> {
            }
        }
    }

    private void buildDesktopIcons() {
        addDesktopIcon(0, FOLDER_ICON, Component.translatable("gui.createcybernetics.computer.tutorials"), Page.TUTORIALS);
        addDesktopIcon(1, FOLDER_ICON, Component.translatable("gui.createcybernetics.computer.infologs"), Page.INFOLOGS);
        addDesktopIcon(2, FOLDER_ICON, Component.translatable("gui.createcybernetics.computer.games"), Page.GAMES);
        addDesktopIcon(3, EXE_ICON, Component.translatable("gui.createcybernetics.computer.chatspace"), Page.CHATSPACE);
        addDesktopIcon(4, EXE_ICON, Component.translatable("gui.createcybernetics.computer.ide"), Page.IDE);
        addDesktopIcon(5, SETTINGS_ICON, Component.translatable("gui.createcybernetics.computer.settings"), Page.SETTINGS);

        if (menu.isServerTowerConnected()) {
            addDesktopIcon(6, EXE_ICON, Component.literal("server-stats"), Page.SERVER_STATS);
        }
    }

    private void addDesktopIcon(int column, ResourceLocation texture, Component label, Page destination) {
        int row = column / 4;
        int rowColumn = column % 4;

        visibleIcons.add(new ComputerIcon(DESKTOP_START_X + rowColumn * 52, DESKTOP_ICON_Y + row * 42, texture, label, destination, ItemStack.EMPTY, -1));
    }

    private void buildShardIcons(Predicate<ItemStack> filter, ResourceLocation iconTexture) {
        List<ComputerMenu.TowerShardEntry> matchingShards = menu.getTowerShardEntries().stream().filter(entry -> filter.test(entry.stack())).toList();

        for (int index = 0; index < matchingShards.size(); index++) {
            int column = index % FILE_GRID_COLUMNS;
            int row = index / FILE_GRID_COLUMNS;
            int iconX = FILE_GRID_START_X + column * FILE_GRID_SPACING_X;
            int iconY = FILE_GRID_START_Y + row * FILE_GRID_SPACING_Y;

            ComputerMenu.TowerShardEntry entry = matchingShards.get(index);
            ItemStack stack = entry.stack();

            visibleIcons.add(new ComputerIcon(iconX, iconY, iconTexture, getShardExecutableName(stack), null, stack, entry.slot()));
        }
    }

    private Component getShardExecutableName(ItemStack stack) {
        if (stack.getItem() instanceof GameShardItem gameShard) {
            return gameShard.getExecutableName(stack);
        }

        return stack.getHoverName();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation background = menu.isConnected() ? CONNECTED_TEXTURE : DISCONNECTED_TEXTURE;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos, topPos, 0.0F);
        guiGraphics.pose().scale(BACKGROUND_SCALE, BACKGROUND_SCALE, 1.0F);
        guiGraphics.blit(background, 0, 0, 0, 0, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderPage(guiGraphics, mouseX, mouseY);
        renderNotification(guiGraphics);
        renderCustomCursor(guiGraphics, mouseX, mouseY);
    }

    private void renderPage(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!menu.isConnected()) {
            renderDisconnectedMessage(guiGraphics);
            return;
        }

        renderPageHeader(guiGraphics);

        switch (currentPage) {
            case SETTINGS -> renderSettingsPage(guiGraphics);
            case SERVER_STATS -> renderServerStatsPage(guiGraphics);
            case CHATSPACE -> renderChatSpace(guiGraphics, mouseX, mouseY);
            default -> renderIcons(guiGraphics, mouseX, mouseY);
        }

        if (currentPage != Page.DESKTOP) {
            renderBackButton(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderDisconnectedMessage(GuiGraphics guiGraphics) {
        int centerX = leftPos + SCREEN_X + SCREEN_WIDTH / 2;
        int centerY = topPos + SCREEN_Y + SCREEN_HEIGHT / 2;

        guiGraphics.drawCenteredString(font, Component.translatable("gui.createcybernetics.computer.disconnected"), centerX, centerY - 8, ERROR_COLOR);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.createcybernetics.computer.disconnected.description"), centerX, centerY + 8, DISABLED_COLOR);
    }

    private void renderPageHeader(GuiGraphics guiGraphics) {
        Component header = switch (currentPage) {
            case DESKTOP -> Component.translatable("gui.createcybernetics.computer.desktop");
            case TUTORIALS -> Component.translatable("gui.createcybernetics.computer.tutorials");
            case INFOLOGS -> Component.translatable("gui.createcybernetics.computer.infologs");
            case GAMES -> Component.translatable("gui.createcybernetics.computer.games");

            case CHATSPACE -> {
                if (chatPage == ChatPage.CONVERSATION) {
                    ChatSpaceClientData.ClientContact contact = getSelectedContact();

                    if (contact != null) {
                        yield Component.literal("ChatSpace.exe - " + contact.displayName());
                    }
                }

                yield Component.translatable("gui.createcybernetics.computer.chatspace");
            }

            case IDE -> Component.translatable("gui.createcybernetics.computer.ide");
            case SETTINGS -> Component.translatable("gui.createcybernetics.computer.settings");
            case SERVER_STATS -> Component.literal("server-stats");
        };

        renderScaledText(guiGraphics, header, leftPos + HEADER_X, topPos + HEADER_Y, 0.65F, NEON_GREEN, false);
    }

    private void renderIcons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (ComputerIcon icon : visibleIcons) {
            int absoluteX = leftPos + icon.x();
            int absoluteY = topPos + icon.y();
            int clickX = absoluteX - (ICON_CLICK_WIDTH - ICON_WIDTH) / 2;

            boolean hovered = isInside(mouseX, mouseY, clickX, absoluteY - 1, ICON_CLICK_WIDTH, ICON_CLICK_HEIGHT);

            if (hovered) {
                guiGraphics.fill(clickX, absoluteY - 1, clickX + ICON_CLICK_WIDTH, absoluteY - 1 + ICON_CLICK_HEIGHT, 0x4439FF14);
            }

            renderScaledTexture(guiGraphics, icon.texture(), absoluteX, absoluteY, ICON_WIDTH, ICON_HEIGHT, ICON_TEXTURE_WIDTH, ICON_TEXTURE_HEIGHT);
            renderWrappedIconLabel(guiGraphics, icon.label(), absoluteX + ICON_WIDTH / 2, absoluteY + ICON_HEIGHT + ICON_TO_LABEL_GAP);
        }

        if (visibleIcons.isEmpty() && currentPage != Page.DESKTOP) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.createcybernetics.computer.folder_empty"), leftPos + SCREEN_X + SCREEN_WIDTH / 2, topPos + SCREEN_Y + SCREEN_HEIGHT / 2, NEON_GREEN_DIM);
        }
    }

    private void renderWrappedIconLabel(GuiGraphics guiGraphics, Component label, int centerX, int startY) {
        List<FormattedCharSequence> wrappedLines = font.split(label, LABEL_WRAP_WIDTH);
        int lineCount = Math.min(wrappedLines.size(), MAX_LABEL_LINES);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, startY, 0.0F);
        guiGraphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0F);

        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            FormattedCharSequence line = wrappedLines.get(lineIndex);
            int lineWidth = font.width(line);
            guiGraphics.drawString(font, line, -lineWidth / 2, lineIndex * LABEL_LINE_HEIGHT, NEON_GREEN, false);
        }

        guiGraphics.pose().popPose();
    }

    private void renderSettingsPage(GuiGraphics guiGraphics) {
        int panelX = leftPos + CONTENT_X;
        int panelY = topPos + CONTENT_Y + 20;
        int panelWidth = CONTENT_RIGHT - CONTENT_X;
        int panelHeight = CONTENT_BOTTOM - CONTENT_Y - 40;

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BLACK);
        drawBorder(guiGraphics, panelX, panelY, panelWidth, panelHeight, NEON_GREEN);

        renderScaledText(guiGraphics, Component.translatable("gui.createcybernetics.computer.settings.system"), panelX + 8, panelY + 9, 0.65F, NEON_GREEN, false);
        renderScaledText(guiGraphics, Component.translatable("gui.createcybernetics.computer.settings.connection"), panelX + 8, panelY + 29, 0.6F, NEON_GREEN, false);
        renderScaledText(guiGraphics, Component.translatable("gui.createcybernetics.computer.settings.connection.connected"), panelX + 99, panelY + 29, 0.6F, NEON_GREEN, false);
        renderScaledText(guiGraphics, Component.translatable("gui.createcybernetics.computer.settings.files", menu.getTowerShards().size()), panelX + 8, panelY + 47, 0.6F, NEON_GREEN, false);
        renderScaledText(guiGraphics, Component.translatable("gui.createcybernetics.computer.settings.computer_code", menu.getComputerCode()), panelX + 8, panelY + 65, 0.6F, NEON_GREEN, false);
        renderScaledText(guiGraphics, Component.translatable("gui.createcybernetics.computer.settings.unread", ChatSpaceClientData.getTotalUnreadCount()), panelX + 8, panelY + 83, 0.6F, NEON_GREEN, false);
    }

    private void renderServerStatsPage(GuiGraphics guiGraphics) {
        int panelX = leftPos + CONTENT_X;
        int panelY = topPos + CONTENT_Y + 20;
        int panelWidth = CONTENT_RIGHT - CONTENT_X;
        int panelHeight = CONTENT_BOTTOM - CONTENT_Y - 40;

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BLACK);
        drawBorder(guiGraphics, panelX, panelY, panelWidth, panelHeight, NEON_GREEN);

        renderScaledText(guiGraphics, Component.literal("NET ARCHITECTURE"), panelX + 8, panelY + 9, 0.7F, NEON_GREEN, false);

        renderScaledText(guiGraphics, Component.literal("DEFENSE LEVEL"), panelX + 8, panelY + 31, 0.6F, NEON_GREEN_DIM, false);
        renderScaledText(guiGraphics, Component.literal("NOT AVAILABLE"), panelX + 108, panelY + 31, 0.6F, NEON_GREEN, false);

        renderScaledText(guiGraphics, Component.literal("ICE PROGRAM"), panelX + 8, panelY + 50, 0.6F, NEON_GREEN_DIM, false);
        renderScaledText(guiGraphics, Component.literal("NOT AVAILABLE"), panelX + 108, panelY + 50, 0.6F, NEON_GREEN, false);

        renderScaledText(guiGraphics, Component.literal("ARCHITECTURE SIZE"), panelX + 8, panelY + 69, 0.6F, NEON_GREEN_DIM, false);
        renderScaledText(guiGraphics, Component.literal("NOT AVAILABLE"), panelX + 108, panelY + 69, 0.6F, NEON_GREEN, false);

        renderScaledText(guiGraphics, Component.literal("ACTIVE DAEMONS"), panelX + 8, panelY + 88, 0.6F, NEON_GREEN_DIM, false);
        renderScaledText(guiGraphics, Component.literal("NOT AVAILABLE"), panelX + 108, panelY + 88, 0.6F, NEON_GREEN, false);

        renderScaledText(guiGraphics, Component.literal("ACCESS NODES"), panelX + 8, panelY + 107, 0.6F, NEON_GREEN_DIM, false);
        renderScaledText(guiGraphics, Component.literal("NOT AVAILABLE"), panelX + 108, panelY + 107, 0.6F, NEON_GREEN, false);
    }

    private void renderChatSpace(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (chatPage == ChatPage.CONTACTS) {
            renderContactPage(guiGraphics, mouseX, mouseY);
        } else {
            renderConversationPage(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderContactPage(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderSmallButton(guiGraphics, leftPos + CHAT_ADD_BUTTON_X, topPos + CHAT_INPUT_Y, CHAT_ADD_BUTTON_WIDTH, CHAT_INPUT_HEIGHT, Component.translatable("gui.createcybernetics.computer.chatspace.add"), isInside(mouseX, mouseY, leftPos + CHAT_ADD_BUTTON_X, topPos + CHAT_INPUT_Y, CHAT_ADD_BUTTON_WIDTH, CHAT_INPUT_HEIGHT));

        List<ChatSpaceClientData.ClientContact> contacts = ChatSpaceClientData.getContacts();

        if (contacts.isEmpty()) {
            renderScaledTextCentered(guiGraphics, Component.translatable("gui.createcybernetics.computer.chatspace.no_contacts"), leftPos + CONTACT_LIST_X + CONTACT_LIST_WIDTH / 2, topPos + CONTACT_LIST_Y + 45, 0.6F, NEON_GREEN_DIM);
            return;
        }

        int maximumVisible = Math.min(CONTACT_VISIBLE_ROWS, contacts.size() - contactScrollOffset);

        for (int visibleIndex = 0; visibleIndex < maximumVisible; visibleIndex++) {
            int contactIndex = contactScrollOffset + visibleIndex;
            ChatSpaceClientData.ClientContact contact = contacts.get(contactIndex);
            int rowX = leftPos + CONTACT_LIST_X;
            int rowY = topPos + CONTACT_LIST_Y + visibleIndex * CONTACT_ROW_HEIGHT;

            boolean hovered = isInside(mouseX, mouseY, rowX, rowY, CONTACT_LIST_WIDTH, CONTACT_ROW_HEIGHT - 2);

            guiGraphics.fill(rowX, rowY, rowX + CONTACT_LIST_WIDTH, rowY + CONTACT_ROW_HEIGHT - 2, hovered ? 0xCC103B0C : 0xBB000000);
            drawBorder(guiGraphics, rowX, rowY, CONTACT_LIST_WIDTH, CONTACT_ROW_HEIGHT - 2, hovered ? NEON_GREEN : NEON_GREEN_DIM);

            renderScaledText(guiGraphics, Component.literal(contact.displayName()), rowX + 6, rowY + 4, 0.65F, NEON_GREEN, false);
            renderScaledText(guiGraphics, Component.literal(contact.remoteCode()), rowX + 95, rowY + 4, 0.55F, NEON_GREEN_DIM, false);

            if (contact.unreadCount() > 0) {
                Component unread = Component.literal(Integer.toString(contact.unreadCount()));
                renderScaledTextCentered(guiGraphics, unread, rowX + CONTACT_LIST_WIDTH - 11, rowY + 4, 0.65F, NEON_GREEN);
            }
        }

        renderScrollIndicator(guiGraphics, contacts.size(), CONTACT_VISIBLE_ROWS, contactScrollOffset, leftPos + CONTACT_LIST_X + CONTACT_LIST_WIDTH - 3, topPos + CONTACT_LIST_Y, CONTACT_VISIBLE_ROWS * CONTACT_ROW_HEIGHT - 2);
    }

    private void renderConversationPage(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ChatSpaceClientData.ClientContact contact = getSelectedContact();

        if (contact == null) {
            chatPage = ChatPage.CONTACTS;
            selectedRemoteCode = "";
            updateChatWidgetVisibility();
            return;
        }

        int panelX = leftPos + CONVERSATION_X;
        int panelY = topPos + CONVERSATION_Y;

        guiGraphics.fill(panelX, panelY, panelX + CONVERSATION_WIDTH, panelY + CONVERSATION_HEIGHT, PANEL_BLACK);
        drawBorder(guiGraphics, panelX, panelY, CONVERSATION_WIDTH, CONVERSATION_HEIGHT, NEON_GREEN);

        renderConversationMessages(guiGraphics, contact, panelX + 5, panelY + 5, CONVERSATION_WIDTH - 10, CONVERSATION_HEIGHT - 10);
        renderSmallButton(guiGraphics, leftPos + SEND_BUTTON_X, topPos + MESSAGE_INPUT_Y, SEND_BUTTON_WIDTH, MESSAGE_INPUT_HEIGHT, Component.translatable("gui.createcybernetics.computer.chatspace.send"), isInside(mouseX, mouseY, leftPos + SEND_BUTTON_X, topPos + MESSAGE_INPUT_Y, SEND_BUTTON_WIDTH, MESSAGE_INPUT_HEIGHT));
    }

    private void renderConversationMessages(GuiGraphics guiGraphics, ChatSpaceClientData.ClientContact contact, int x, int y, int width, int height) {
        List<RenderedMessageLine> renderedLines = buildRenderedMessageLines(contact, width);

        int visibleLineCount = Math.max(1, height / 7);
        int maximumOffset = Math.max(0, renderedLines.size() - visibleLineCount);

        messageScrollOffset = Mth.clamp(messageScrollOffset, 0, maximumOffset);

        int startIndex = Math.max(0, renderedLines.size() - visibleLineCount - messageScrollOffset);
        int endIndex = Math.min(renderedLines.size(), startIndex + visibleLineCount);

        int renderY = y;

        for (int index = startIndex; index < endIndex; index++) {
            RenderedMessageLine line = renderedLines.get(index);
            renderScaledText(guiGraphics, line.text(), x, renderY, 0.55F, line.ownMessage() ? NEON_GREEN : 0xFFB4FF9D, false);
            renderY += 7;
        }
    }

    private List<RenderedMessageLine> buildRenderedMessageLines(ChatSpaceClientData.ClientContact contact, int renderedWidth) {
        List<RenderedMessageLine> lines = new ArrayList<>();
        int wrapWidth = Math.round(renderedWidth / 0.55F);
        String ownCode = ChatSpaceClientData.getComputerCode();

        for (ChatSpaceClientData.ClientMessage message : contact.messages()) {
            boolean ownMessage = message.senderCode().equals(ownCode);
            Component prefix = Component.literal(ownMessage ? "YOU: " : contact.displayName() + ": ");
            Component fullMessage = prefix.copy().append(message.text());

            List<FormattedCharSequence> wrapped = font.split(fullMessage, wrapWidth);

            for (FormattedCharSequence sequence : wrapped) {
                lines.add(new RenderedMessageLine(Component.literal(sequenceToString(sequence)), ownMessage));
            }
        }

        return lines;
    }

    private static String sequenceToString(FormattedCharSequence sequence) {
        StringBuilder builder = new StringBuilder();

        sequence.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });

        return builder.toString();
    }

    private void renderNotification(GuiGraphics guiGraphics) {
        if (!ChatSpaceClientData.hasActiveNotification()) {
            return;
        }

        int x = leftPos + SCREEN_RIGHT - NOTIFICATION_WIDTH - 5;
        int y = topPos + SCREEN_BOTTOM - NOTIFICATION_HEIGHT - 5;

        guiGraphics.fill(x, y, x + NOTIFICATION_WIDTH, y + NOTIFICATION_HEIGHT, 0xEE000000);
        drawBorder(guiGraphics, x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, NEON_GREEN);

        renderScaledText(guiGraphics, Component.translatable("gui.createcybernetics.computer.chatspace.notification", ChatSpaceClientData.getPendingNotification()), x + 6, y + 8, 0.55F, NEON_GREEN, false);
    }

    private void renderScrollIndicator(GuiGraphics guiGraphics, int itemCount, int visibleCount, int scrollOffset, int x, int y, int height) {
        if (itemCount <= visibleCount) {
            return;
        }

        guiGraphics.fill(x, y, x + 2, y + height, NEON_GREEN_DARK);

        int handleHeight = Math.max(8, height * visibleCount / itemCount);
        int maximumOffset = itemCount - visibleCount;
        int handleTravel = height - handleHeight;
        int handleY = y + handleTravel * scrollOffset / maximumOffset;

        guiGraphics.fill(x, handleY, x + 2, handleY + handleHeight, NEON_GREEN);
    }

    private void renderSmallButton(GuiGraphics guiGraphics, int x, int y, int width, int height, Component text, boolean hovered) {
        guiGraphics.fill(x, y, x + width, y + height, hovered ? 0xCC123B0D : BLACK);
        drawBorder(guiGraphics, x, y, width, height, NEON_GREEN);
        renderScaledTextCentered(guiGraphics, text, x + width / 2, y + 4, 0.55F, NEON_GREEN);
    }

    private void renderBackButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int buttonX = leftPos + BACK_BUTTON_X;
        int buttonY = topPos + BACK_BUTTON_Y;

        boolean hovered = isInside(mouseX, mouseY, buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);

        guiGraphics.fill(buttonX, buttonY, buttonX + BACK_BUTTON_WIDTH, buttonY + BACK_BUTTON_HEIGHT, hovered ? 0xCC123B0D : BLACK);
        drawBorder(guiGraphics, buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, NEON_GREEN);

        renderScaledTextCentered(guiGraphics, Component.translatable("gui.createcybernetics.computer.back"), buttonX + BACK_BUTTON_WIDTH / 2, buttonY + 4, 0.55F, NEON_GREEN);
    }

    private void renderCustomCursor(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int inset = 2;

        int minimumX = leftPos + SCREEN_X + inset;
        int minimumY = topPos + SCREEN_Y + inset;
        int maximumX = leftPos + SCREEN_RIGHT - CURSOR_WIDTH - inset;
        int maximumY = topPos + SCREEN_BOTTOM - CURSOR_HEIGHT - inset;

        int constrainedX = Mth.clamp(mouseX, minimumX, maximumX);
        int constrainedY = Mth.clamp(mouseY, minimumY, maximumY);

        renderScaledTexture(guiGraphics, CURSOR_ICON, constrainedX, constrainedY, CURSOR_WIDTH, CURSOR_HEIGHT, CURSOR_TEXTURE_WIDTH, CURSOR_TEXTURE_HEIGHT);
    }

    private static void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int renderedWidth, int renderedHeight, int textureWidth, int textureHeight) {
        float scaleX = renderedWidth / (float) textureWidth;
        float scaleY = renderedHeight / (float) textureHeight;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scaleX, scaleY, 1.0F);
        guiGraphics.blit(texture, 0, 0, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        guiGraphics.pose().popPose();
    }

    private void renderScaledText(GuiGraphics guiGraphics, Component text, int x, int y, float scale, int color, boolean shadow) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, shadow);
        guiGraphics.pose().popPose();
    }

    private void renderScaledTextCentered(GuiGraphics guiGraphics, Component text, int centerX, int y, float scale, int color) {
        int textWidth = font.width(text);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, -textWidth / 2, 0, color, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (!isInsideMonitor(mouseX, mouseY)) {
            return true;
        }

        if (!menu.isConnected()) {
            return true;
        }

        if (currentPage != Page.DESKTOP && isBackButtonClicked(mouseX, mouseY)) {
            handleBackNavigation();
            return true;
        }

        if (currentPage == Page.CHATSPACE) {
            return handleChatSpaceClick(mouseX, mouseY);
        }

        if (currentPage == Page.SETTINGS || currentPage == Page.SERVER_STATS) {
            return true;
        }

        for (ComputerIcon icon : visibleIcons) {
            int absoluteX = leftPos + icon.x();
            int absoluteY = topPos + icon.y();
            int clickX = absoluteX - (ICON_CLICK_WIDTH - ICON_WIDTH) / 2;

            if (!isInside(mouseX, mouseY, clickX, absoluteY - 1, ICON_CLICK_WIDTH, ICON_CLICK_HEIGHT)) {
                continue;
            }

            handleIconClicked(icon);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleChatSpaceClick(double mouseX, double mouseY) {
        if (chatPage == ChatPage.CONTACTS) {
            if (isInside(mouseX, mouseY, leftPos + CHAT_ADD_BUTTON_X, topPos + CHAT_INPUT_Y, CHAT_ADD_BUTTON_WIDTH, CHAT_INPUT_HEIGHT)) {
                addContact();
                return true;
            }

            List<ChatSpaceClientData.ClientContact> contacts = ChatSpaceClientData.getContacts();

            int maximumVisible = Math.min(CONTACT_VISIBLE_ROWS, contacts.size() - contactScrollOffset);

            for (int visibleIndex = 0; visibleIndex < maximumVisible; visibleIndex++) {
                int rowX = leftPos + CONTACT_LIST_X;
                int rowY = topPos + CONTACT_LIST_Y + visibleIndex * CONTACT_ROW_HEIGHT;

                if (!isInside(mouseX, mouseY, rowX, rowY, CONTACT_LIST_WIDTH, CONTACT_ROW_HEIGHT - 2)) {
                    continue;
                }

                ChatSpaceClientData.ClientContact contact = contacts.get(contactScrollOffset + visibleIndex);

                openConversation(contact);
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        }

        if (isInside(mouseX, mouseY, leftPos + SEND_BUTTON_X, topPos + MESSAGE_INPUT_Y, SEND_BUTTON_WIDTH, MESSAGE_INPUT_HEIGHT)) {
            sendMessage();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }

    private void addContact() {
        String remoteCode = contactCodeBox.getValue().trim().toUpperCase();
        String displayName = contactNameBox.getValue().trim();

        if (remoteCode.length() != 5 || displayName.isBlank()) {
            return;
        }

        PacketDistributor.sendToServer(new ChatSpaceAddContactPayload(menu.getComputerPos(), remoteCode, displayName));

        contactCodeBox.setValue("");
        contactNameBox.setValue("");

        requestChatSync();
    }

    private void openConversation(ChatSpaceClientData.ClientContact contact) {
        selectedRemoteCode = contact.remoteCode();
        chatPage = ChatPage.CONVERSATION;
        messageScrollOffset = 0;

        updateChatWidgetVisibility();

        PacketDistributor.sendToServer(new ChatSpaceMarkReadPayload(menu.getComputerPos(), selectedRemoteCode));

        ChatSpaceClientData.clearNotification();

        requestChatSync();
    }

    private void sendMessage() {
        if (selectedRemoteCode.isBlank()) {
            return;
        }

        String message = messageBox.getValue().trim();

        if (message.isBlank()) {
            return;
        }

        PacketDistributor.sendToServer(new ChatSpaceSendMessagePayload(menu.getComputerPos(), selectedRemoteCode, message));

        messageBox.setValue("");
        messageScrollOffset = 0;

        requestChatSync();
    }

    private void handleBackNavigation() {
        if (currentPage == Page.CHATSPACE && chatPage == ChatPage.CONVERSATION) {
            chatPage = ChatPage.CONTACTS;
            selectedRemoteCode = "";
            messageScrollOffset = 0;

            updateChatWidgetVisibility();
            return;
        }

        currentPage = Page.DESKTOP;
        chatPage = ChatPage.CONTACTS;
        selectedRemoteCode = "";

        refreshVisibleIcons();
        updateChatWidgetVisibility();
    }

    private boolean isBackButtonClicked(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, leftPos + BACK_BUTTON_X, topPos + BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
    }

    private void handleIconClicked(ComputerIcon icon) {
        if (icon.destination() == Page.IDE) {
            Minecraft.getInstance().setScreen(new QuickhackIdeScreen(this, menu));
            return;
        }

        if (icon.destination() == Page.SERVER_STATS && !menu.isServerTowerConnected()) {
            return;
        }

        if (icon.destination() != null) {
            currentPage = icon.destination();

            if (currentPage == Page.CHATSPACE) {
                chatPage = ChatPage.CONTACTS;
                selectedRemoteCode = "";
                requestChatSync();
            }

            refreshVisibleIcons();
            updateChatWidgetVisibility();
            return;
        }

        ItemStack stack = icon.stack();

        if (stack.isEmpty()) {
            return;
        }

        if (stack.getItem() instanceof InfologDataShardItem) {
            openInfolog(stack, icon.towerSlot());
            return;
        }

        if (stack.getItem() instanceof GameShardItem gameShard) {
            openGame(gameShard);
        }
    }

    private void openGame(GameShardItem gameShard) {
        switch (gameShard.getGameId()) {
            case "minesweeper" -> Minecraft.getInstance().setScreen(new MinesweeperScreen(this));
            case "chess" -> Minecraft.getInstance().setScreen(new ChessLauncherScreen(this));

            default -> {
                Minecraft minecraft = Minecraft.getInstance();

                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.translatable("gui.createcybernetics.computer.game_unavailable", gameShard.getGameId()), true);
                }
            }
        }
    }

    private void openInfolog(ItemStack stack, int towerSlot) {
        if (towerSlot < 0) {
            return;
        }

        if (!InfologTextData.isLocked(stack)) {
            Minecraft.getInstance().setScreen(new ComputerInfologEditScreen(this, menu.getComputerPos(), towerSlot, stack));
            return;
        }

        String title = InfologTextData.getTitle(stack);
        String text = InfologTextData.getText(stack);

        Minecraft.getInstance().setScreen(new InfologReadScreen(this, stack, title, text));
    }

    private ChatSpaceClientData.ClientContact getSelectedContact() {
        if (selectedRemoteCode.isBlank()) {
            return null;
        }

        return ChatSpaceClientData.getContact(selectedRemoteCode);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentPage != Page.CHATSPACE) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int direction = scrollY > 0.0D ? -1 : 1;

        if (chatPage == ChatPage.CONTACTS) {
            contactScrollOffset += direction;
            clampContactScroll();

            return true;
        }

        messageScrollOffset += direction;
        clampMessageScroll();

        return true;
    }

    private void clampContactScroll() {
        int maximumOffset = Math.max(0, ChatSpaceClientData.getContacts().size() - CONTACT_VISIBLE_ROWS);
        contactScrollOffset = Mth.clamp(contactScrollOffset, 0, maximumOffset);
    }

    private void clampMessageScroll() {
        ChatSpaceClientData.ClientContact contact = getSelectedContact();

        if (contact == null) {
            messageScrollOffset = 0;
            return;
        }

        int estimatedLines = Math.max(contact.messages().size(), contact.messages().size() * 2);
        int visibleLines = Math.max(1, (CONVERSATION_HEIGHT - 10) / 7);
        int maximumOffset = Math.max(0, estimatedLines - visibleLines);

        messageScrollOffset = Mth.clamp(messageScrollOffset, 0, maximumOffset);
    }

    private boolean isInsideMonitor(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, leftPos + SCREEN_X, topPos + SCREEN_Y, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typingInTextBox = contactCodeBox != null && contactCodeBox.isFocused() || contactNameBox != null && contactNameBox.isFocused() || messageBox != null && messageBox.isFocused();

        if (typingInTextBox && Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (currentPage == Page.CHATSPACE) {
                if (chatPage == ChatPage.CONTACTS) {
                    addContact();
                    return true;
                }

                if (chatPage == ChatPage.CONVERSATION) {
                    sendMessage();
                    return true;
                }
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && currentPage != Page.DESKTOP) {
            handleBackNavigation();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Page {
        DESKTOP,
        TUTORIALS,
        INFOLOGS,
        GAMES,
        CHATSPACE,
        IDE,
        SETTINGS,
        SERVER_STATS
    }

    private enum ChatPage {
        CONTACTS,
        CONVERSATION
    }

    private record ComputerIcon(int x, int y, ResourceLocation texture, Component label, Page destination, ItemStack stack, int towerSlot) {}

    private record RenderedMessageLine(Component text, boolean ownMessage) {}
}