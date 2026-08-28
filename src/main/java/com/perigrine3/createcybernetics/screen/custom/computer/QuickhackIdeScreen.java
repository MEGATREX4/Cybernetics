package com.perigrine3.createcybernetics.screen.custom.computer;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.quickhack.MicroprogramCategory;
import com.perigrine3.createcybernetics.common.quickhack.MicroprogramDefinition;
import com.perigrine3.createcybernetics.common.quickhack.MicroprogramRegistry;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackProgramData;
import com.perigrine3.createcybernetics.component.MicroprogramLibrary;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.generic.MicroprogramCreativeShardItem;
import com.perigrine3.createcybernetics.item.generic.MicroprogramDataShardItem;
import com.perigrine3.createcybernetics.item.generic.QuickhackShardItem;
import com.perigrine3.createcybernetics.network.payload.QuickhackSaveProgramPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class QuickhackIdeScreen extends Screen {
    private static final int GUI_WIDTH = 390;
    private static final int GUI_HEIGHT = 244;

    private static final int PANEL_COLOR = 0xFF000000;
    private static final int BORDER_COLOR = 0xFF39FF14;
    private static final int TEXT_COLOR = 0xFFE0FFE0;
    private static final int DIM_TEXT_COLOR = 0xFF7BAA7B;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int SELECTED_COLOR = 0xFF00FFFF;
    private static final int ROW_HOVER_COLOR = 0x4439FF14;

    private static final int TARGET_COLOR = 0xFF55AAFF;
    private static final int FUNCTION_COLOR = 0xFF55FF55;
    private static final int MODIFIER_COLOR = 0xFFCC66FF;

    private static final int LEFT_PANEL_WIDTH = 120;
    private static final int PROGRAM_PANEL_WIDTH = 150;
    private static final int RIGHT_PANEL_WIDTH = 92;
    private static final int PANEL_GAP = 5;

    private static final int ROW_HEIGHT = 14;
    private static final int VISIBLE_LIBRARY_ROWS = 9;
    private static final int VISIBLE_PROGRAM_ROWS = 10;

    private final Screen parentScreen;
    private final ComputerMenu menu;

    private final List<MicroprogramDefinition> program = new ArrayList<>();
    private final List<ShardEntry> quickhackShards = new ArrayList<>();
    private final Set<ResourceLocation> availableMicroprograms = new LinkedHashSet<>();
    private final List<Button> outlinedButtons = new ArrayList<>();

    private int leftPos;
    private int topPos;

    private int libraryX;
    private int libraryY;
    private int programX;
    private int programY;
    private int rightX;
    private int rightY;

    private int libraryScrollOffset;
    private int programScrollOffset;
    private int selectedShardIndex;
    private int microprogramLibraryShardCount;

    private MicroprogramCategory selectedCategory = MicroprogramCategory.TARGET;

    private EditBox nameBox;
    private Button targetButton;
    private Button functionButton;
    private Button modifierButton;
    private Button previousShardButton;
    private Button nextShardButton;
    private Button saveButton;

    public QuickhackIdeScreen(Screen parentScreen, ComputerMenu menu) {
        super(Component.translatable("gui.createcybernetics.quickhack_ide.title"));
        this.parentScreen = parentScreen;
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();

        outlinedButtons.clear();

        leftPos = (width - GUI_WIDTH) / 2;
        topPos = (height - GUI_HEIGHT) / 2;

        libraryX = leftPos + 8;
        libraryY = topPos + 50;

        programX = libraryX + LEFT_PANEL_WIDTH + PANEL_GAP;
        programY = libraryY;

        rightX = programX + PROGRAM_PANEL_WIDTH + PANEL_GAP;
        rightY = libraryY;

        rebuildMicroprogramAccess();
        rebuildShardList();

        nameBox = new EditBox(font, programX, topPos + 19, PROGRAM_PANEL_WIDTH, 18, Component.translatable("gui.createcybernetics.quickhack_ide.name"));
        nameBox.setMaxLength(QuickhackProgramData.MAX_NAME_LENGTH);
        nameBox.setHint(Component.translatable("gui.createcybernetics.quickhack_ide.name_hint"));
        nameBox.setResponder(value -> updateSaveButton());
        addRenderableWidget(nameBox);

        int categoryWidth = 38;

        targetButton = addOutlinedButton(Button.builder(Component.empty(), button -> {
            selectedCategory = MicroprogramCategory.TARGET;
            libraryScrollOffset = 0;
        }).bounds(libraryX, topPos + 19, categoryWidth, 18).build());

        functionButton = addOutlinedButton(Button.builder(Component.empty(), button -> {
            selectedCategory = MicroprogramCategory.FUNCTION;
            libraryScrollOffset = 0;
        }).bounds(libraryX + categoryWidth + 2, topPos + 19, categoryWidth, 18).build());

        modifierButton = addOutlinedButton(Button.builder(Component.empty(), button -> {
            selectedCategory = MicroprogramCategory.MODIFIER;
            libraryScrollOffset = 0;
        }).bounds(libraryX + (categoryWidth + 2) * 2, topPos + 19, categoryWidth, 18).build());

        previousShardButton = addOutlinedButton(Button.builder(Component.literal("<"), button -> selectPreviousShard()).bounds(rightX, topPos + 19, 22, 18).build());
        nextShardButton = addOutlinedButton(Button.builder(Component.literal(">"), button -> selectNextShard()).bounds(rightX + RIGHT_PANEL_WIDTH - 22, topPos + 19, 22, 18).build());

        saveButton = addOutlinedButton(Button.builder(Component.translatable("gui.createcybernetics.quickhack_ide.compile"), button -> saveProgram()).bounds(rightX, topPos + GUI_HEIGHT - 31, RIGHT_PANEL_WIDTH, 20).build());

        loadSelectedShard();
        updateSaveButton();
    }

    private Button addOutlinedButton(Button button) {
        outlinedButtons.add(button);
        return addRenderableWidget(button);
    }

    private void rebuildMicroprogramAccess() {
        availableMicroprograms.clear();
        microprogramLibraryShardCount = 0;

        for (ComputerMenu.TowerShardEntry entry : menu.getTowerShardEntries()) {
            ItemStack stack = entry.stack();

            if (stack.isEmpty()) {
                continue;
            }

            MicroprogramLibrary library;

            if (stack.is(ModItems.DATA_SHARD_MICROPROGRAMS.get())) {
                library = MicroprogramDataShardItem.getLibrary(stack);
            } else if (stack.getItem() instanceof MicroprogramCreativeShardItem) {
                library = MicroprogramCreativeShardItem.getLibrary(stack);
            } else {
                continue;
            }

            microprogramLibraryShardCount++;
            availableMicroprograms.addAll(library.microprograms());
        }
    }

    private void rebuildShardList() {
        quickhackShards.clear();

        for (ComputerMenu.TowerShardEntry entry : menu.getTowerShardEntries()) {
            ItemStack stack = entry.stack();

            if (!(stack.getItem() instanceof QuickhackShardItem)) {
                continue;
            }

            quickhackShards.add(new ShardEntry(entry.slot(), stack.copy()));
        }

        if (quickhackShards.isEmpty()) {
            selectedShardIndex = -1;
            program.clear();
            return;
        }

        selectedShardIndex = Math.max(0, Math.min(selectedShardIndex, quickhackShards.size() - 1));
    }

    private void loadSelectedShard() {
        program.clear();
        programScrollOffset = 0;

        ShardEntry entry = getSelectedShard();

        if (entry == null) {
            if (nameBox != null) {
                nameBox.setValue("");
            }

            updateSaveButton();
            return;
        }

        program.addAll(QuickhackProgramData.getNodes(entry.stack()));

        if (nameBox != null) {
            nameBox.setValue(QuickhackProgramData.getName(entry.stack()));
        }

        updateSaveButton();
    }

    private ShardEntry getSelectedShard() {
        if (selectedShardIndex < 0 || selectedShardIndex >= quickhackShards.size()) {
            return null;
        }

        return quickhackShards.get(selectedShardIndex);
    }

    private void selectPreviousShard() {
        if (quickhackShards.isEmpty()) {
            return;
        }

        selectedShardIndex--;

        if (selectedShardIndex < 0) {
            selectedShardIndex = quickhackShards.size() - 1;
        }

        loadSelectedShard();
    }

    private void selectNextShard() {
        if (quickhackShards.isEmpty()) {
            return;
        }

        selectedShardIndex++;

        if (selectedShardIndex >= quickhackShards.size()) {
            selectedShardIndex = 0;
        }

        loadSelectedShard();
    }

    private List<MicroprogramDefinition> getVisibleDefinitions() {
        return MicroprogramRegistry.getByCategory(selectedCategory).stream().filter(this::hasMicroprogramAccess).toList();
    }

    private boolean hasMicroprogramAccess(MicroprogramDefinition definition) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, definition.id());
        return availableMicroprograms.contains(id);
    }

    private boolean hasAllProgramAccess() {
        for (MicroprogramDefinition definition : program) {
            if (!hasMicroprogramAccess(definition)) {
                return false;
            }
        }

        return true;
    }

    private void appendNode(MicroprogramDefinition definition) {
        if (definition == null || program.size() >= QuickhackProgramData.MAX_PROGRAM_NODES || !hasMicroprogramAccess(definition)) {
            return;
        }

        program.add(definition);

        if (program.size() > VISIBLE_PROGRAM_ROWS) {
            programScrollOffset = program.size() - VISIBLE_PROGRAM_ROWS;
        }

        updateSaveButton();
    }

    private void removeNode(int programIndex) {
        if (programIndex < 0 || programIndex >= program.size()) {
            return;
        }

        program.remove(programIndex);

        int maxScroll = Math.max(0, program.size() - VISIBLE_PROGRAM_ROWS);
        programScrollOffset = Math.min(programScrollOffset, maxScroll);

        updateSaveButton();
    }

    private void updateSaveButton() {
        if (saveButton == null) {
            return;
        }

        String name = nameBox == null ? "" : QuickhackProgramData.sanitizeName(nameBox.getValue());
        saveButton.active = getSelectedShard() != null && !name.isBlank() && QuickhackProgramData.isValidProgram(program) && hasAllProgramAccess();
    }

    private void saveProgram() {
        ShardEntry shard = getSelectedShard();

        if (shard == null || nameBox == null) {
            return;
        }

        String name = QuickhackProgramData.sanitizeName(nameBox.getValue());

        if (name.isBlank() || !QuickhackProgramData.isValidProgram(program) || !hasAllProgramAccess()) {
            return;
        }

        List<String> nodeIds = program.stream().map(MicroprogramDefinition::id).toList();

        PacketDistributor.sendToServer(new QuickhackSaveProgramPayload(menu.getComputerPos(), shard.towerSlot(), name, nodeIds));

        ItemStack localStack = shard.stack().copy();
        QuickhackProgramData.setProgram(localStack, name, program);
        quickhackShards.set(selectedShardIndex, new ShardEntry(shard.towerSlot(), localStack));

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("gui.createcybernetics.quickhack_ide.saved", name + ".exe"), true);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, PANEL_COLOR);
        drawBorder(guiGraphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, BORDER_COLOR);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderButtonOutlines(guiGraphics);
        renderCategoryButtonLabels(guiGraphics);

        guiGraphics.drawString(font, Component.literal("ide.exe"), leftPos + 8, topPos + 7, BORDER_COLOR, false);

        renderLibrary(guiGraphics, mouseX, mouseY);
        renderProgram(guiGraphics, mouseX, mouseY);
        renderShardPanel(guiGraphics);
        renderMicroprogramTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderButtonOutlines(GuiGraphics guiGraphics) {
        for (Button button : outlinedButtons) {
            drawBorder(guiGraphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(), BORDER_COLOR);
        }
    }

    private void renderCategoryButtonLabels(GuiGraphics guiGraphics) {
        int targetColor = selectedCategory == MicroprogramCategory.TARGET ? 0xFFFFFFFF : TARGET_COLOR;
        int functionColor = selectedCategory == MicroprogramCategory.FUNCTION ? 0xFFFFFFFF : FUNCTION_COLOR;
        int modifierColor = selectedCategory == MicroprogramCategory.MODIFIER ? 0xFFFFFFFF : MODIFIER_COLOR;

        guiGraphics.drawCenteredString(font, "TARGET", targetButton.getX() + targetButton.getWidth() / 2, targetButton.getY() + 5, targetColor);
        guiGraphics.drawCenteredString(font, "FUNC", functionButton.getX() + functionButton.getWidth() / 2, functionButton.getY() + 5, functionColor);
        guiGraphics.drawCenteredString(font, "MOD", modifierButton.getX() + modifierButton.getWidth() / 2, modifierButton.getY() + 5, modifierColor);
    }

    private void renderLibrary(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int categoryColor = getCategoryColor(selectedCategory);

        guiGraphics.drawString(font, Component.translatable("gui.createcybernetics.quickhack_ide.library"), libraryX, libraryY - 11, categoryColor, false);

        if (microprogramLibraryShardCount <= 0) {
            guiGraphics.drawWordWrap(font, Component.translatable("gui.createcybernetics.quickhack_ide.no_library"), libraryX, libraryY + 3, LEFT_PANEL_WIDTH, ERROR_COLOR);
            return;
        }

        List<MicroprogramDefinition> definitions = getVisibleDefinitions();
        int maxScroll = Math.max(0, definitions.size() - VISIBLE_LIBRARY_ROWS);
        libraryScrollOffset = Math.max(0, Math.min(libraryScrollOffset, maxScroll));

        if (definitions.isEmpty()) {
            guiGraphics.drawWordWrap(font, Component.translatable("gui.createcybernetics.quickhack_ide.no_category_programs"), libraryX, libraryY + 3, LEFT_PANEL_WIDTH, DIM_TEXT_COLOR);
            return;
        }

        for (int visibleIndex = 0; visibleIndex < VISIBLE_LIBRARY_ROWS; visibleIndex++) {
            int definitionIndex = visibleIndex + libraryScrollOffset;

            if (definitionIndex >= definitions.size()) {
                break;
            }

            MicroprogramDefinition definition = definitions.get(definitionIndex);
            int rowY = libraryY + visibleIndex * ROW_HEIGHT;

            if (isInside(mouseX, mouseY, libraryX, rowY, LEFT_PANEL_WIDTH, ROW_HEIGHT)) {
                guiGraphics.fill(libraryX, rowY, libraryX + LEFT_PANEL_WIDTH, rowY + ROW_HEIGHT, ROW_HOVER_COLOR);
            }

            guiGraphics.drawString(font, definition.displayName(), libraryX + 2, rowY + 3, TEXT_COLOR, false);

            String ramText = definition.ramCost() >= 0 ? "+" + definition.ramCost() : Integer.toString(definition.ramCost());
            int ramWidth = font.width(ramText);

            guiGraphics.drawString(font, ramText, libraryX + LEFT_PANEL_WIDTH - ramWidth - 3, rowY + 3, DIM_TEXT_COLOR, false);
        }
    }

    private void renderProgram(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui.createcybernetics.quickhack_ide.program"), programX, programY - 11, TEXT_COLOR, false);

        int maxScroll = Math.max(0, program.size() - VISIBLE_PROGRAM_ROWS);
        programScrollOffset = Math.max(0, Math.min(programScrollOffset, maxScroll));

        for (int visibleIndex = 0; visibleIndex < VISIBLE_PROGRAM_ROWS; visibleIndex++) {
            int programIndex = visibleIndex + programScrollOffset;

            if (programIndex >= program.size()) {
                break;
            }

            MicroprogramDefinition definition = program.get(programIndex);
            int rowY = programY + visibleIndex * ROW_HEIGHT;

            if (isInside(mouseX, mouseY, programX, rowY, PROGRAM_PANEL_WIDTH, ROW_HEIGHT)) {
                guiGraphics.fill(programX, rowY, programX + PROGRAM_PANEL_WIDTH, rowY + ROW_HEIGHT, ROW_HOVER_COLOR);
            }

            String prefix = switch (definition.category()) {
                case TARGET -> "T";
                case FUNCTION -> "F";
                case MODIFIER -> "M";
            };

            int prefixColor = getCategoryColor(definition.category());

            guiGraphics.drawString(font, prefix, programX + 2, rowY + 3, prefixColor, false);
            guiGraphics.drawString(font, definition.displayName(), programX + 14, rowY + 3, hasMicroprogramAccess(definition) ? TEXT_COLOR : ERROR_COLOR, false);
        }

        int footerY = topPos + GUI_HEIGHT - 35;

        guiGraphics.drawString(font, Component.literal(program.size() + "/" + QuickhackProgramData.MAX_PROGRAM_NODES + " Nodes"), programX, footerY, DIM_TEXT_COLOR, false);
        guiGraphics.drawString(font, Component.literal("RAM: " + QuickhackProgramData.calculateRamCost(program)), programX, footerY + 11, SELECTED_COLOR, false);

        if (!program.isEmpty() && !QuickhackProgramData.isValidProgram(program)) {
            guiGraphics.drawString(font, Component.translatable("gui.createcybernetics.quickhack_ide.invalid"), programX, footerY + 22, ERROR_COLOR, false);
        } else if (!program.isEmpty() && !hasAllProgramAccess()) {
            guiGraphics.drawString(font, Component.translatable("gui.createcybernetics.quickhack_ide.missing_microprogram"), programX, footerY + 22, ERROR_COLOR, false);
        }
    }

    private void renderShardPanel(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("gui.createcybernetics.quickhack_ide.shard"), rightX, rightY - 11, TEXT_COLOR, false);

        ShardEntry shard = getSelectedShard();

        if (shard == null) {
            guiGraphics.drawWordWrap(font, Component.translatable("gui.createcybernetics.quickhack_ide.no_shard"), rightX, rightY + 4, RIGHT_PANEL_WIDTH, ERROR_COLOR);
            return;
        }

        guiGraphics.renderItem(shard.stack(), rightX + RIGHT_PANEL_WIDTH / 2 - 8, rightY + 3);

        String slotText = "Tower Slot " + (shard.towerSlot() + 1);
        guiGraphics.drawCenteredString(font, slotText, rightX + RIGHT_PANEL_WIDTH / 2, rightY + 24, DIM_TEXT_COLOR);

        String currentName = QuickhackProgramData.getName(shard.stack());

        if (currentName.isBlank()) {
            currentName = "BLANK";
        } else {
            currentName += ".exe";
        }

        guiGraphics.drawWordWrap(font, Component.literal(currentName), rightX, rightY + 39, RIGHT_PANEL_WIDTH, TEXT_COLOR);
        guiGraphics.drawString(font, Component.literal("RAM " + QuickhackProgramData.calculateRamCost(program)), rightX, rightY + 67, SELECTED_COLOR, false);

        String libraryText = microprogramLibraryShardCount == 1 ? availableMicroprograms.size() + " Programs" : availableMicroprograms.size() + " Programs / " + microprogramLibraryShardCount + " Libraries";
        guiGraphics.drawWordWrap(font, Component.literal(libraryText), rightX, rightY + 82, RIGHT_PANEL_WIDTH, DIM_TEXT_COLOR);
    }

    private void renderMicroprogramTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<MicroprogramDefinition> definitions = getVisibleDefinitions();

        for (int visibleIndex = 0; visibleIndex < VISIBLE_LIBRARY_ROWS; visibleIndex++) {
            int definitionIndex = visibleIndex + libraryScrollOffset;

            if (definitionIndex >= definitions.size()) {
                break;
            }

            int rowY = libraryY + visibleIndex * ROW_HEIGHT;

            if (isInside(mouseX, mouseY, libraryX, rowY, LEFT_PANEL_WIDTH, ROW_HEIGHT)) {
                renderMicroprogramTooltip(guiGraphics, definitions.get(definitionIndex), mouseX, mouseY);
                return;
            }
        }

        for (int visibleIndex = 0; visibleIndex < VISIBLE_PROGRAM_ROWS; visibleIndex++) {
            int programIndex = visibleIndex + programScrollOffset;

            if (programIndex >= program.size()) {
                break;
            }

            int rowY = programY + visibleIndex * ROW_HEIGHT;

            if (isInside(mouseX, mouseY, programX, rowY, PROGRAM_PANEL_WIDTH, ROW_HEIGHT)) {
                renderMicroprogramTooltip(guiGraphics, program.get(programIndex), mouseX, mouseY);
                return;
            }
        }
    }

    private void renderMicroprogramTooltip(GuiGraphics guiGraphics, MicroprogramDefinition definition, int mouseX, int mouseY) {
        List<Component> tooltip = List.of(
                Component.literal(definition.displayName()).withStyle(getCategoryFormatting(definition.category())),
                definition.getDescription(),
                Component.literal("RAM Cost: " + definition.ramCost()).withStyle(ChatFormatting.GRAY)
        );

        guiGraphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<MicroprogramDefinition> definitions = getVisibleDefinitions();

            for (int visibleIndex = 0; visibleIndex < VISIBLE_LIBRARY_ROWS; visibleIndex++) {
                int definitionIndex = visibleIndex + libraryScrollOffset;

                if (definitionIndex >= definitions.size()) {
                    break;
                }

                int rowY = libraryY + visibleIndex * ROW_HEIGHT;

                if (isInside(mouseX, mouseY, libraryX, rowY, LEFT_PANEL_WIDTH, ROW_HEIGHT)) {
                    appendNode(definitions.get(definitionIndex));
                    return true;
                }
            }

            for (int visibleIndex = 0; visibleIndex < VISIBLE_PROGRAM_ROWS; visibleIndex++) {
                int programIndex = visibleIndex + programScrollOffset;

                if (programIndex >= program.size()) {
                    break;
                }

                int rowY = programY + visibleIndex * ROW_HEIGHT;

                if (isInside(mouseX, mouseY, programX, rowY, PROGRAM_PANEL_WIDTH, ROW_HEIGHT)) {
                    removeNode(programIndex);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInside(mouseX, mouseY, libraryX, libraryY, LEFT_PANEL_WIDTH, VISIBLE_LIBRARY_ROWS * ROW_HEIGHT)) {
            List<MicroprogramDefinition> definitions = getVisibleDefinitions();
            int maxScroll = Math.max(0, definitions.size() - VISIBLE_LIBRARY_ROWS);

            if (scrollY < 0.0D) {
                libraryScrollOffset = Math.min(maxScroll, libraryScrollOffset + 1);
            } else if (scrollY > 0.0D) {
                libraryScrollOffset = Math.max(0, libraryScrollOffset - 1);
            }

            return true;
        }

        if (isInside(mouseX, mouseY, programX, programY, PROGRAM_PANEL_WIDTH, VISIBLE_PROGRAM_ROWS * ROW_HEIGHT)) {
            int maxScroll = Math.max(0, program.size() - VISIBLE_PROGRAM_ROWS);

            if (scrollY < 0.0D) {
                programScrollOffset = Math.min(maxScroll, programScrollOffset + 1);
            } else if (scrollY > 0.0D) {
                programScrollOffset = Math.max(0, programScrollOffset - 1);
            }

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean inventoryKey = Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode);

        if (nameBox != null && nameBox.isFocused() && inventoryKey) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(parentScreen);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int getCategoryColor(MicroprogramCategory category) {
        return switch (category) {
            case TARGET -> TARGET_COLOR;
            case FUNCTION -> FUNCTION_COLOR;
            case MODIFIER -> MODIFIER_COLOR;
        };
    }

    private static ChatFormatting getCategoryFormatting(MicroprogramCategory category) {
        return switch (category) {
            case TARGET -> ChatFormatting.AQUA;
            case FUNCTION -> ChatFormatting.GREEN;
            case MODIFIER -> ChatFormatting.LIGHT_PURPLE;
        };
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

    private record ShardEntry(int towerSlot, ItemStack stack) {}
}