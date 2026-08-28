package com.perigrine3.createcybernetics.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MaterializedFirewallRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/item/materialized_firewall.png");

    private ShieldModel shieldModel;

    public MaterializedFirewallRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (shieldModel == null) {
            shieldModel = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        }

        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(bufferSource, RenderType.entityCutoutNoCull(TEXTURE), false, stack.hasFoil());
        shieldModel.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF);

        poseStack.popPose();
    }
}