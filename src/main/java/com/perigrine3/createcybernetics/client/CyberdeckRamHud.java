package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CyberdeckRamHud {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberdeck_ram");

    private static final ResourceLocation RAM_FULL = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/ram_full.png");
    private static final ResourceLocation RAM_EMPTY = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/ram_empty.png");

    private static final ResourceLocation CYBERSPACE_DIMENSION = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace");

    public static final int ICON_SIZE = 9;
    public static final int ICON_SPACING = 1;
    public static final int ICON_STEP = ICON_SIZE + ICON_SPACING;
    public static final int TITLE_ICON_GAP = 3;

    private static final int PREVIEW_MAX_RAM = 25;

    private CyberdeckRamHud() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, CyberdeckRamHud::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;
        if (mc.options.hideGui) return;
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        boolean hasCyberdeck = data.hasSpecificItem(ModItems.BRAINUPGRADES_CYBERDECK.get(), CyberwareSlot.BRAIN);
        boolean inCyberspace = player.level().dimension().location().equals(CYBERSPACE_DIMENSION);

        if (!hasCyberdeck && !inCyberspace) return;

        int maxRam = data.getMaxCyberdeckRam(player);
        if (maxRam <= 0) return;

        int currentRam = Mth.clamp(data.getCyberdeckRam(), 0, maxRam);

        HudConfigClient.HudConfig cfg = HudConfigClient.get(player.getUUID());

        renderRam(guiGraphics, mc, cfg.ram, currentRam, maxRam);
    }

    public static void renderPreview(GuiGraphics guiGraphics, HudConfigClient.HudConfig cfg) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        int maxRam = PREVIEW_MAX_RAM;
        int currentRam = PREVIEW_MAX_RAM / 2;

        if (player != null && player.hasData(ModAttachments.CYBERWARE)) {
            PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

            if (data != null) {
                int actualMaxRam = data.getMaxCyberdeckRam(player);

                if (actualMaxRam > 0) {
                    maxRam = actualMaxRam;
                    currentRam = Mth.clamp(data.getCyberdeckRam(), 0, maxRam);
                }
            }
        }

        renderRam(guiGraphics, mc, cfg.ram, currentRam, maxRam);
    }

    private static void renderRam(GuiGraphics guiGraphics, Minecraft mc, HudConfigClient.RamLayout layout, int currentRam, int maxRam) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = layout.pixelX(screenWidth);
        int y = layout.pixelY(screenHeight);
        int width = layout.pixelWidth(screenWidth);
        int height = layout.pixelHeight(screenHeight);

        int titleHeight = mc.font.lineHeight;
        int iconAreaY = y + titleHeight + TITLE_ICON_GAP;
        int iconAreaHeight = Math.max(ICON_SIZE, height - titleHeight - TITLE_ICON_GAP);

        int columns = Math.max(1, (width + ICON_SPACING) / ICON_STEP);
        int rowsThatFit = Math.max(1, (iconAreaHeight + ICON_SPACING) / ICON_STEP);

        int requiredRows = Mth.ceil(maxRam / (float) columns);

        if (requiredRows > rowsThatFit) {
            columns = Math.max(columns, Mth.ceil(maxRam / (float) rowsThatFit));
            requiredRows = Mth.ceil(maxRam / (float) columns);
        }

        String title = "Cyberdeck RAM: " + currentRam + "/" + maxRam;
        int titleX = x + width / 2 - mc.font.width(title) / 2;

        guiGraphics.drawString(mc.font, title, titleX, y, 0xFFFFFFFF, true);

        int ramIndex = 0;

        for (int row = 0; row < requiredRows && ramIndex < maxRam; row++) {
            int iconsInRow = Math.min(columns, maxRam - ramIndex);
            int rowWidth = iconsInRow * ICON_SIZE + Math.max(0, iconsInRow - 1) * ICON_SPACING;
            int rowX = x + width / 2 - rowWidth / 2;
            int rowY = iconAreaY + row * ICON_STEP;

            for (int column = 0; column < iconsInRow; column++) {
                int iconX = rowX + column * ICON_STEP;

                ResourceLocation texture = ramIndex < currentRam ? RAM_FULL : RAM_EMPTY;

                guiGraphics.blit(texture, iconX, rowY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

                ramIndex++;
            }
        }
    }

    public static int getPreviewMaxRam() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return PREVIEW_MAX_RAM;
        if (!player.hasData(ModAttachments.CYBERWARE)) return PREVIEW_MAX_RAM;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return PREVIEW_MAX_RAM;

        int maxRam = data.getMaxCyberdeckRam(player);

        return maxRam > 0 ? maxRam : PREVIEW_MAX_RAM;
    }

    public static int minimumWidthForColumns(int columns) {
        int clampedColumns = Math.max(1, columns);

        return clampedColumns * ICON_SIZE + Math.max(0, clampedColumns - 1) * ICON_SPACING;
    }

    public static int minimumHeightForRows(Minecraft mc, int rows) {
        int clampedRows = Math.max(1, rows);

        return mc.font.lineHeight + TITLE_ICON_GAP + clampedRows * ICON_SIZE + Math.max(0, clampedRows - 1) * ICON_SPACING;
    }

    public static void show() {
    }

    public static void reset() {
    }
}