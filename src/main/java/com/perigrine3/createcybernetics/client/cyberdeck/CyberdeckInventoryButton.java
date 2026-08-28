package com.perigrine3.createcybernetics.client.cyberdeck;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CyberdeckInventoryButton extends AbstractButton {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/item/brainupgrades_cyberdeck.png");

    private final Runnable onPress;

    public CyberdeckInventoryButton(int x, int y, Runnable onPress) {
        super(x, y, 16, 16, Component.translatable("gui.createcybernetics.cyberdeck.inventory_button"));

        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(TEXTURE, getX(), getY(), 0, 0, width, height, 16, 16);

        if (isHoveredOrFocused()) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
        }
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationOutput) {
        defaultButtonNarrationText(narrationOutput);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHovered) return;

        Minecraft minecraft = Minecraft.getInstance();
        graphics.renderTooltip(minecraft.font, getMessage(), mouseX, mouseY);
    }
}