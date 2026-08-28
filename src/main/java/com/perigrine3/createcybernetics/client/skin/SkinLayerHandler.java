package com.perigrine3.createcybernetics.client.skin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.perigrine3.createcybernetics.client.CyberspaceColorConfigClient;
import com.perigrine3.createcybernetics.client.render.CyberwareLimbHider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class SkinLayerHandler extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public SkinLayerHandler(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static boolean shouldRenderOverlaysFor(AbstractClientPlayer target) {
        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity();

        if (cam instanceof Player viewer) {
            return !target.isInvisibleTo(viewer);
        }

        return !target.isInvisible();
    }

    private static void applyOverlayVisibility(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model) {
        boolean hasLeftArm = CyberwareLimbHider.shouldRenderLeftArm(player);
        boolean hasRightArm = CyberwareLimbHider.shouldRenderRightArm(player);
        boolean hasLeftLeg = CyberwareLimbHider.shouldRenderLeftLeg(player);
        boolean hasRightLeg = CyberwareLimbHider.shouldRenderRightLeg(player);

        model.head.visible = true;
        model.body.visible = true;

        model.leftArm.visible = hasLeftArm;
        model.rightArm.visible = hasRightArm;
        model.leftLeg.visible = hasLeftLeg;
        model.rightLeg.visible = hasRightLeg;

        model.hat.visible = SkinVanillaWearVisibility.isModelPartShownNow(player, PlayerModelPart.HAT);
        model.jacket.visible = SkinVanillaWearVisibility.isModelPartShownNow(player, PlayerModelPart.JACKET);

        model.leftSleeve.visible = hasLeftArm
                && SkinVanillaWearVisibility.isModelPartShownNow(player, PlayerModelPart.LEFT_SLEEVE);

        model.rightSleeve.visible = hasRightArm
                && SkinVanillaWearVisibility.isModelPartShownNow(player, PlayerModelPart.RIGHT_SLEEVE);

        model.leftPants.visible = hasLeftLeg
                && SkinVanillaWearVisibility.isModelPartShownNow(player, PlayerModelPart.LEFT_PANTS_LEG);

        model.rightPants.visible = hasRightLeg
                && SkinVanillaWearVisibility.isModelPartShownNow(player, PlayerModelPart.RIGHT_PANTS_LEG);
    }

    private static void renderCyberspaceBaseSkin(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int cyberspaceColor) {
        ResourceLocation texture = player.getSkin().texture();
        var vc = buffer.getBuffer(SkinRenderTypes.cyberspacePlayer(texture));

        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, cyberspaceColor);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!shouldRenderOverlaysFor(player)) return;

        boolean cyberspaceAvatar = SkinModifierManager.isCyberspaceAvatar(player);

        SkinModifierState state = SkinModifierManager.getPlayerSkinState(player);

        if (!cyberspaceAvatar && (state == null || !state.hasModifiers())) return;

        int cyberspaceColor = CyberspaceColorConfigClient.getColor(player.getUUID());

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();

        boolean prevHead = model.head.visible;
        boolean prevHat = model.hat.visible;
        boolean prevBody = model.body.visible;
        boolean prevJacket = model.jacket.visible;
        boolean prevLeftArm = model.leftArm.visible;
        boolean prevLeftSleeve = model.leftSleeve.visible;
        boolean prevRightArm = model.rightArm.visible;
        boolean prevRightSleeve = model.rightSleeve.visible;
        boolean prevLeftLeg = model.leftLeg.visible;
        boolean prevLeftPants = model.leftPants.visible;
        boolean prevRightLeg = model.rightLeg.visible;
        boolean prevRightPants = model.rightPants.visible;

        SkinVanillaWearVisibility.pushSuppress();

        try {
            applyOverlayVisibility(player, model);

            if (cyberspaceAvatar) {
                renderCyberspaceBaseSkin(player, model, poseStack, buffer, packedLight, cyberspaceColor);
            }

            if (state == null || !state.hasModifiers()) return;

            PlayerSkin.Model modelType = player.getSkin().model();

            for (SkinModifier modifier : state.getModifiers()) {
                if (modifier == null) continue;

                poseStack.pushPose();

                try {
                    ResourceLocation texture = modifier.getTexture(modelType);

                    RenderType renderType = cyberspaceAvatar
                            ? SkinRenderTypes.cyberspacePlayer(texture)
                            : RenderType.entityTranslucent(texture);

                    int color = cyberspaceAvatar
                            ? cyberspaceColor
                            : modifier.getColor();

                    var baseVc = buffer.getBuffer(renderType);

                    SkinOverlayModelRenderer.renderModifier(
                            model,
                            modifier,
                            poseStack,
                            baseVc,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            color
                    );

                    if (modifier.hasGlint()) {
                        var glintVc = buffer.getBuffer(SkinRenderTypes.translucentGlintOverlay(texture));

                        SkinOverlayModelRenderer.renderModifier(
                                model,
                                modifier,
                                poseStack,
                                glintVc,
                                packedLight,
                                OverlayTexture.NO_OVERLAY,
                                0xFFFFFFFF
                        );
                    }
                } finally {
                    poseStack.popPose();
                }
            }
        } finally {
            SkinVanillaWearVisibility.popSuppress();

            model.head.visible = prevHead;
            model.hat.visible = prevHat;
            model.body.visible = prevBody;
            model.jacket.visible = prevJacket;
            model.leftArm.visible = prevLeftArm;
            model.leftSleeve.visible = prevLeftSleeve;
            model.rightArm.visible = prevRightArm;
            model.rightSleeve.visible = prevRightSleeve;
            model.leftLeg.visible = prevLeftLeg;
            model.leftPants.visible = prevLeftPants;
            model.rightLeg.visible = prevRightLeg;
            model.rightPants.visible = prevRightPants;
        }
    }
}