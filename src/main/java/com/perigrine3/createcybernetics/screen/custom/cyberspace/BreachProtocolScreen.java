package com.perigrine3.createcybernetics.screen.custom.cyberspace;

import com.perigrine3.createcybernetics.client.IceDetectionClientState;
import com.perigrine3.createcybernetics.network.payload.BreachProtocolPayloads;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BreachProtocolScreen extends Screen {
    private static final int GRID_SIZE = 5;

    private static final int CELL_WIDTH = 30;
    private static final int CELL_HEIGHT = 20;
    private static final int CELL_GAP = 3;

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 210;

    private static final int BACKGROUND = 0xEE05080A;
    private static final int PANEL_BORDER = 0xFF26E5FF;
    private static final int TEXT = 0xFFE6FCFF;
    private static final int DIM_TEXT = 0xFF668A90;
    private static final int ACTIVE_CELL = 0x5535DDF5;
    private static final int HOVER_CELL = 0x8848E9FF;
    private static final int SELECTED_CELL = 0xAA18353A;
    private static final int CELL_BORDER = 0xFF20818E;
    private static final int INVALID_CODE = 0xFF75555A;
    private static final int TIMER_COLOR = 0xFFFF5555;

    private final UUID sessionId;
    private final List<String> grid;
    private final List<List<String>> codes;
    private final int durationTicks;

    private final Set<Integer> selectedIndices = new HashSet<>();
    private List<String> input = new ArrayList<>();

    private boolean selectingRow = true;
    private int requiredIndex;
    private boolean waitingForServer;
    private boolean closedByServer;

    private boolean timerStarted;
    private long deadlineMillis;

    public BreachProtocolScreen(BreachProtocolPayloads.OpenPayload payload) {
        super(Component.translatable("gui.createcybernetics.breach_protocol"));

        this.sessionId = payload.sessionId();
        this.grid = List.copyOf(payload.grid());

        List<List<String>> copiedCodes = new ArrayList<>();

        for (List<String> code : payload.codes()) {
            copiedCodes.add(List.copyOf(code));
        }

        this.codes = List.copyOf(copiedCodes);
        this.durationTicks = payload.durationTicks();
    }

    public boolean matchesSession(UUID sessionId) {
        return this.sessionId.equals(sessionId);
    }

    public void applyState(BreachProtocolPayloads.StatePayload payload) {
        if (!matchesSession(payload.sessionId())) {
            return;
        }

        selectedIndices.clear();
        selectedIndices.addAll(payload.selectedIndices());

        input = new ArrayList<>(payload.input());
        selectingRow = payload.selectingRow();
        requiredIndex = payload.requiredIndex();
        waitingForServer = false;
    }

    public void closeFromServer() {
        closedByServer = true;

        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;

        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, BACKGROUND);
        drawBorder(guiGraphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BORDER);

        guiGraphics.drawString(
                font,
                Component.translatable("gui.createcybernetics.breach_protocol"),
                panelX + 10,
                panelY + 9,
                TEXT,
                false
        );

        renderTimer(guiGraphics, panelX, panelY);

        int currentDetection = IceDetectionClientState.getDetection();

        guiGraphics.drawString(
                font,
                Component.literal("ICE DETECTION " + currentDetection + "%"),
                panelX + 10,
                panelY + 24,
                TEXT,
                false
        );

        if (!timerStarted) {
            guiGraphics.drawString(
                    font,
                    Component.literal("SELECT FIRST BYTE TO BEGIN"),
                    panelX + 185,
                    panelY + 24,
                    DIM_TEXT,
                    false
            );
        }

        renderBuffer(guiGraphics, panelX, panelY);
        renderGrid(guiGraphics, panelX, panelY, mouseX, mouseY);
        renderCodes(guiGraphics, panelX, panelY, currentDetection);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTimer(GuiGraphics guiGraphics, int panelX, int panelY) {
        Component timerText;

        if (timerStarted) {
            long remainingMillis = Math.max(0L, deadlineMillis - Util.getMillis());
            float remainingSeconds = remainingMillis / 1000.0F;

            timerText = Component.literal(String.format("%.1fs", remainingSeconds));
        } else {
            float durationSeconds = durationTicks / 20.0F;
            timerText = Component.literal(String.format("%.1fs", durationSeconds));
        }

        guiGraphics.drawString(
                font,
                timerText,
                panelX + PANEL_WIDTH - 45,
                panelY + 9,
                timerStarted ? TIMER_COLOR : DIM_TEXT,
                false
        );
    }

    private void renderBuffer(GuiGraphics guiGraphics, int panelX, int panelY) {
        int x = panelX + 10;
        int y = panelY + 43;

        guiGraphics.drawString(font, Component.literal("BUFFER"), x, y, TEXT, false);

        int bufferY = y + 12;

        for (int i = 0; i < 6; i++) {
            int cellX = x + i * 28;

            guiGraphics.fill(cellX, bufferY, cellX + 24, bufferY + 14, 0xAA071316);
            drawBorder(guiGraphics, cellX, bufferY, 24, 14, CELL_BORDER);

            String token = i < input.size() ? input.get(i) : "--";

            guiGraphics.drawCenteredString(
                    font,
                    token,
                    cellX + 12,
                    bufferY + 3,
                    i < input.size() ? TEXT : DIM_TEXT
            );
        }
    }

    private void renderGrid(GuiGraphics guiGraphics, int panelX, int panelY, int mouseX, int mouseY) {
        int gridX = panelX + 10;
        int gridY = panelY + 82;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                int index = row * GRID_SIZE + column;

                int x = gridX + column * (CELL_WIDTH + CELL_GAP);
                int y = gridY + row * (CELL_HEIGHT + CELL_GAP);

                boolean selected = selectedIndices.contains(index);
                boolean validAxis = isValidAxis(row, column);
                boolean hovered = mouseX >= x && mouseX < x + CELL_WIDTH && mouseY >= y && mouseY < y + CELL_HEIGHT;

                int background;

                if (selected) {
                    background = SELECTED_CELL;
                } else if (validAxis && hovered) {
                    background = HOVER_CELL;
                } else if (validAxis) {
                    background = ACTIVE_CELL;
                } else {
                    background = 0xAA071316;
                }

                guiGraphics.fill(x, y, x + CELL_WIDTH, y + CELL_HEIGHT, background);
                drawBorder(guiGraphics, x, y, CELL_WIDTH, CELL_HEIGHT, selected || validAxis ? PANEL_BORDER : CELL_BORDER);

                guiGraphics.drawCenteredString(
                        font,
                        grid.get(index),
                        x + CELL_WIDTH / 2,
                        y + 6,
                        selected || validAxis ? TEXT : DIM_TEXT
                );
            }
        }
    }

    private void renderCodes(GuiGraphics guiGraphics, int panelX, int panelY, int currentDetection) {
        int x = panelX + 185;
        int y = panelY + 43;

        guiGraphics.drawString(
                font,
                Component.translatable("gui.createcybernetics.breach_protocol.input_codes"),
                x,
                y,
                TEXT,
                false
        );

        int codeY = y + 17;

        for (List<String> code : codes) {
            boolean valid = startsWith(code, input);

            int detectionChange = detectionChangeForLength(code.size());
            int projectedDetection = Mth.clamp(currentDetection + detectionChange, 0, 100);

            int color = valid ? TEXT : INVALID_CODE;

            guiGraphics.drawString(
                    font,
                    Component.literal(String.join(" ", code)),
                    x,
                    codeY,
                    color,
                    false
            );

            String deltaText;

            if (detectionChange > 0) {
                deltaText = "+" + detectionChange;
            } else {
                deltaText = Integer.toString(detectionChange);
            }

            guiGraphics.drawString(
                    font,
                    Component.literal(code.size() + " BYTE  |  ICE " + deltaText + "  |  " + currentDetection + " → " + projectedDetection),
                    x,
                    codeY + 11,
                    valid ? DIM_TEXT : INVALID_CODE,
                    false
            );

            codeY += 31;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || waitingForServer) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;

        int gridX = panelX + 10;
        int gridY = panelY + 82;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int column = 0; column < GRID_SIZE; column++) {
                int x = gridX + column * (CELL_WIDTH + CELL_GAP);
                int y = gridY + row * (CELL_HEIGHT + CELL_GAP);

                if (mouseX < x || mouseX >= x + CELL_WIDTH || mouseY < y || mouseY >= y + CELL_HEIGHT) {
                    continue;
                }

                int index = row * GRID_SIZE + column;

                if (selectedIndices.contains(index)) {
                    return true;
                }

                if (!isValidAxis(row, column)) {
                    return true;
                }

                if (!timerStarted && selectedIndices.isEmpty()) {
                    timerStarted = true;
                    deadlineMillis = Util.getMillis() + durationTicks * 50L;
                }

                waitingForServer = true;

                PacketDistributor.sendToServer(
                        new BreachProtocolPayloads.SelectPayload(
                                sessionId,
                                row,
                                column
                        )
                );

                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isValidAxis(int row, int column) {
        if (selectedIndices.contains(row * GRID_SIZE + column)) {
            return false;
        }

        if (selectingRow) {
            return row == requiredIndex;
        }

        return column == requiredIndex;
    }

    private static int detectionChangeForLength(int length) {
        return switch (length) {
            case 3 -> 10;
            case 4 -> 5;
            case 5 -> 0;
            case 6 -> -5;
            default -> 0;
        };
    }

    private static boolean startsWith(List<String> code, List<String> input) {
        if (input.size() > code.size()) {
            return false;
        }

        for (int i = 0; i < input.size(); i++) {
            if (!code.get(i).equals(input.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onClose() {
        if (!closedByServer) {
            PacketDistributor.sendToServer(new BreachProtocolPayloads.CancelPayload(sessionId));
        }

        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}