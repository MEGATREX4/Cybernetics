package com.perigrine3.createcybernetics.client.hud;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.MacroprogramClientRuntimeState;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.macroprogram.MacroprogramDefinition;
import com.perigrine3.createcybernetics.item.macroprogram.MacroprogramItem;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CyberspaceMacroprogramHud {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace_macroprogram_hud");

    private static final int LEFT_MARGIN = 8;
    private static final int TOP_MARGIN = 8;
    private static final int LINE_HEIGHT = 11;

    private static final int HEADER_COLOR = 0xFF55FFFF;
    private static final int NORMAL_COLOR = 0xFFFFFFFF;
    private static final int ACTIVE_COLOR = 0xFF55FF55;
    private static final int COOLDOWN_COLOR = 0xFFFF5555;

    private CyberspaceMacroprogramHud() {}

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, CyberspaceMacroprogramHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;
        if (mc.options.hideGui) return;
        if (!mc.player.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) return;
        if (!mc.player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = mc.player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < PlayerCyberwareData.CYBERDECK_MACROPROGRAM_SLOT_COUNT; i++) {
            int slot = PlayerCyberwareData.CYBERDECK_MACROPROGRAM_SLOT_START + i;
            ItemStack stack = data.getCyberdeckStack(slot);

            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof MacroprogramItem macroprogramItem)) continue;

            entries.add(new Entry(stack, macroprogramItem.getDefinition()));
        }

        if (entries.isEmpty()) return;

        int x = LEFT_MARGIN;
        int y = TOP_MARGIN;

        graphics.drawString(mc.font, "MACROPROGRAMS", x, y, HEADER_COLOR, true);
        y += LINE_HEIGHT + 2;

        for (Entry entry : entries) {
            MacroprogramClientRuntimeState.State state = MacroprogramClientRuntimeState.get(entry.definition().id());
            String line = createDisplayLine(entry.definition());
            int color = NORMAL_COLOR;

            if (state.active()) {
                color = ACTIVE_COLOR;
            } else if (state.coolingDown()) {
                color = COOLDOWN_COLOR;
            }

            graphics.drawString(mc.font, line, x, y, color, true);
            y += LINE_HEIGHT;
        }
    }

    private static String createDisplayLine(MacroprogramDefinition definition) {
        MacroprogramClientRuntimeState.State state = MacroprogramClientRuntimeState.get(definition.id());
        String name = definition.getDisplayName().getString();

        if (state.active()) {
            if (state.usesRemaining() >= 0) {
                return name + "  ACTIVE [" + state.usesRemaining() + "]";
            }

            if (state.untimedActive()) {
                return name + "  ACTIVE";
            }

            return name + "  " + formatTicks(state.activeRemaining());
        }

        if (state.coolingDown()) {
            return name + "  " + formatTicks(state.cooldownRemaining());
        }

        return name;
    }

    private static String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, (ticks + 19) / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    private record Entry(ItemStack stack, MacroprogramDefinition definition) {}
}