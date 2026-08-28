package com.perigrine3.createcybernetics.client.cyberdeck;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.network.payload.OpenCyberdeckPayload;
import com.perigrine3.createcybernetics.screen.custom.crafting.ExpandedInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CyberdeckInventoryButtonEvents {
    private static final int BUTTON_X_OFFSET = 59;
    private static final int BUTTON_Y_OFFSET = 44;

    private CyberdeckInventoryButtonEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof ExpandedInventoryScreen)) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) return;
        if (!hasCyberdeck(player)) return;

        int leftPos = (event.getScreen().width - 176) / 2;
        int topPos = (event.getScreen().height - 166) / 2;

        CyberdeckInventoryButton button = new CyberdeckInventoryButton(
                leftPos + BUTTON_X_OFFSET,
                topPos + BUTTON_Y_OFFSET,
                () -> PacketDistributor.sendToServer(new OpenCyberdeckPayload())
        );

        event.addListener(button);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof ExpandedInventoryScreen)) return;

        for (var child : event.getScreen().children()) {
            if (!(child instanceof CyberdeckInventoryButton button)) continue;

            button.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            button.renderTooltip(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }

    private static boolean hasCyberdeck(LocalPlayer player) {
        if (!player.hasData(ModAttachments.CYBERWARE)) return false;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;

        return data.hasSpecificItem(ModItems.BRAINUPGRADES_CYBERDECK.get(), CyberwareSlot.BRAIN);
    }
}