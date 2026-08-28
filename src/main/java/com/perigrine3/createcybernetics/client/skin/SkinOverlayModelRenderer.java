package com.perigrine3.createcybernetics.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;

public final class SkinOverlayModelRenderer {

    private SkinOverlayModelRenderer() {}

    public static <T extends LivingEntity> void renderModifier(
            PlayerModel<T> model,
            SkinModifier modifier,
            PoseStack poseStack,
            VertexConsumer vc,
            int light,
            int overlay,
            int color
    ) {
        if (modifier == null) return;

        if (model.head.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.HEAD)) {
            model.head.render(poseStack, vc, light, overlay, color);
        }

        if (model.hat.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.HAT)) {
            model.hat.render(poseStack, vc, light, overlay, color);
        }

        if (model.body.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.BODY)) {
            model.body.render(poseStack, vc, light, overlay, color);
        }

        if (model.jacket.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.JACKET)) {
            model.jacket.render(poseStack, vc, light, overlay, color);
        }

        if (model.rightArm.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.RIGHT_ARM)) {
            model.rightArm.render(poseStack, vc, light, overlay, color);
        }

        if (model.rightSleeve.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.RIGHT_SLEEVE)) {
            model.rightSleeve.render(poseStack, vc, light, overlay, color);
        }

        if (model.leftArm.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.LEFT_ARM)) {
            model.leftArm.render(poseStack, vc, light, overlay, color);
        }

        if (model.leftSleeve.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.LEFT_SLEEVE)) {
            model.leftSleeve.render(poseStack, vc, light, overlay, color);
        }

        if (model.rightLeg.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.RIGHT_LEG)) {
            model.rightLeg.render(poseStack, vc, light, overlay, color);
        }

        if (model.rightPants.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.RIGHT_PANTS)) {
            model.rightPants.render(poseStack, vc, light, overlay, color);
        }

        if (model.leftLeg.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.LEFT_LEG)) {
            model.leftLeg.render(poseStack, vc, light, overlay, color);
        }

        if (model.leftPants.visible && modifier.rendersOverlayPart(SkinModifier.OverlayPart.LEFT_PANTS)) {
            model.leftPants.render(poseStack, vc, light, overlay, color);
        }
    }
}