package com.perigrine3.createcybernetics.mixin.client;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.client.chipware.ChipwareInventoryButton;
import com.perigrine3.createcybernetics.client.cyberdeck.CyberdeckInventoryButton;
import com.perigrine3.createcybernetics.client.gui.NavigationMapScreen;
import com.perigrine3.createcybernetics.client.navigation.NavigationInventoryButton;
import com.perigrine3.createcybernetics.client.vampyres.VampyresInventoryButton;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import com.perigrine3.createcybernetics.network.payload.OpenChipwareMiniPayload;
import com.perigrine3.createcybernetics.network.payload.OpenCyberdeckPayload;
import com.perigrine3.createcybernetics.network.payload.OpenVampyresPayload;
import com.perigrine3.createcybernetics.screen.custom.hud.CyberpunkMinimapRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryButtonsMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Unique private ChipwareInventoryButton cc$chipwareBtn;
    @Unique private VampyresInventoryButton cc$vampyresBtn;
    @Unique private NavigationInventoryButton cc$navigationBtn;
    @Unique private CyberdeckInventoryButton cc$cyberdeckBtn;

    protected CreativeModeInventoryButtonsMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void cc$addInventoryButtons(CallbackInfo ci) {
        int chipwareX = this.leftPos + 127;
        int chipwareY = this.topPos + 4;

        this.cc$chipwareBtn = new ChipwareInventoryButton(
                chipwareX,
                chipwareY,
                () -> PacketDistributor.sendToServer(new OpenChipwareMiniPayload())
        );

        this.addRenderableWidget(this.cc$chipwareBtn);

        int vampyresX = this.leftPos + 127;
        int vampyresY = this.topPos + 20;

        this.cc$vampyresBtn = new VampyresInventoryButton(
                vampyresX,
                vampyresY,
                () -> PacketDistributor.sendToServer(new OpenVampyresPayload())
        );

        this.addRenderableWidget(this.cc$vampyresBtn);

        int navigationX = this.leftPos + 143;
        int navigationY = this.topPos + 20;

        this.cc$navigationBtn = new NavigationInventoryButton(
                navigationX,
                navigationY,
                () -> Minecraft.getInstance().setScreen(new NavigationMapScreen((CreativeModeInventoryScreen)(Object)this))
        );

        this.addRenderableWidget(this.cc$navigationBtn);

        int cyberdeckX = this.leftPos + 127;
        int cyberdeckY = this.topPos + 36;

        this.cc$cyberdeckBtn = new CyberdeckInventoryButton(
                cyberdeckX,
                cyberdeckY,
                () -> PacketDistributor.sendToServer(new OpenCyberdeckPayload())
        );

        this.addRenderableWidget(this.cc$cyberdeckBtn);

        cc$updateInventoryButtonVisibility();
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void cc$tickButtonVisibility(CallbackInfo ci) {
        cc$updateInventoryButtonVisibility();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cc$renderInventoryButtonTooltips(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!((CreativeModeInventoryScreen)(Object)this).isInventoryOpen()) return;

        if (this.cc$chipwareBtn != null && this.cc$chipwareBtn.visible) {
            this.cc$chipwareBtn.renderTooltip(graphics, mouseX, mouseY);
        }

        if (this.cc$vampyresBtn != null && this.cc$vampyresBtn.visible) {
            this.cc$vampyresBtn.renderTooltip(graphics, mouseX, mouseY);
        }

        if (this.cc$navigationBtn != null && this.cc$navigationBtn.visible) {
            this.cc$navigationBtn.renderTooltip(graphics, mouseX, mouseY);
        }

        if (this.cc$cyberdeckBtn != null && this.cc$cyberdeckBtn.visible) {
            this.cc$cyberdeckBtn.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Unique
    private void cc$updateInventoryButtonVisibility() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean inventoryOpen = ((CreativeModeInventoryScreen)(Object)this).isInventoryOpen();

        if (this.cc$chipwareBtn != null) {
            this.cc$chipwareBtn.visible = inventoryOpen;
            this.cc$chipwareBtn.active = inventoryOpen;
        }

        if (this.cc$vampyresBtn != null) {
            boolean vampyresInstalled = VampyresItem.isInstalled(minecraft.player);
            boolean visible = inventoryOpen && vampyresInstalled;

            this.cc$vampyresBtn.visible = visible;
            this.cc$vampyresBtn.active = visible;
        }

        if (this.cc$navigationBtn != null) {
            boolean navigationChipInstalled = minecraft.player != null && CyberpunkMinimapRenderer.hasNavigationChip(minecraft.player);
            boolean visible = inventoryOpen && navigationChipInstalled;

            this.cc$navigationBtn.visible = visible;
            this.cc$navigationBtn.active = visible;
        }

        if (this.cc$cyberdeckBtn != null) {
            boolean cyberdeckInstalled = hasCyberdeck(minecraft.player);
            boolean visible = inventoryOpen && cyberdeckInstalled;

            this.cc$cyberdeckBtn.visible = visible;
            this.cc$cyberdeckBtn.active = visible;
        }
    }

    @Unique
    private static boolean hasCyberdeck(LocalPlayer player) {
        if (player == null) return false;
        if (!player.hasData(ModAttachments.CYBERWARE)) return false;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;

        return data.hasSpecificItem(ModItems.BRAINUPGRADES_CYBERDECK.get(), CyberwareSlot.BRAIN);
    }
}