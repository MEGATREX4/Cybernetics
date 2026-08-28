package com.perigrine3.createcybernetics.client.gui;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CCIconButton extends AbstractButton {

    public enum Icon {
        CYBEREYE_SKIN(
                ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/cybereye_skin.png"),
                16, 16
        ),
        HUD_LAYOUT(
                ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/hud_layout.png"),
                16, 16
        ),
        CYBERSPACE_COLOR(
                ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/item/brainupgrades_cyberdeck.png"),
                16, 16
        );

        final ResourceLocation texture;
        final int textureWidth;
        final int textureHeight;

        Icon(ResourceLocation texture, int textureWidth, int textureHeight) {
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }
    }

    private final Icon icon;
    private final Runnable onPress;
    private final String markerId;

    private static final int DRAW_ICON_W = 16;
    private static final int DRAW_ICON_H = 16;

    public CCIconButton(int x, int y, int w, int h, Icon icon, Component tooltip, String markerId, Runnable onPress) {
        super(x, y, w, h, Component.empty());
        this.icon = icon;
        this.onPress = onPress;
        this.markerId = markerId;

        setTooltip(Tooltip.create(tooltip));
    }

    public String cc$getMarkerId() {
        return markerId;
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(gg, mouseX, mouseY, partialTick);

        int ix = getX() + (width - DRAW_ICON_W) / 2;
        int iy = getY() + (height - DRAW_ICON_H) / 2;

        if (!this.active) {
            gg.setColor(1f, 1f, 1f, 0.35f);
        }

        gg.blit(icon.texture, ix, iy, DRAW_ICON_W, DRAW_ICON_H, 0, 0, icon.textureWidth, icon.textureHeight, icon.textureWidth, icon.textureHeight);

        gg.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }
}