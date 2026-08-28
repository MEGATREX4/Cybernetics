package com.perigrine3.createcybernetics.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.perigrine3.createcybernetics.client.skin.SkinHighlight;
import com.perigrine3.createcybernetics.client.skin.SkinModifier;
import com.perigrine3.createcybernetics.client.skin.SkinModifierManager;
import com.perigrine3.createcybernetics.client.skin.SkinModifierState;
import com.perigrine3.createcybernetics.client.skin.SkinOverlayModelRenderer;
import com.perigrine3.createcybernetics.client.skin.SkinRenderTypes;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.entity.custom.JackedInPlayerBodyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JackedInPlayerBodyOverlayRenderer extends RenderLayer<JackedInPlayerBodyEntity, PlayerModel<JackedInPlayerBodyEntity>> {
    private static final Map<UUID, RemotePlayer> PROXY_PLAYERS = new HashMap<>();


    private static ClientLevel proxyLevel;

    public JackedInPlayerBodyOverlayRenderer(RenderLayerParent<JackedInPlayerBodyEntity, PlayerModel<JackedInPlayerBodyEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, JackedInPlayerBodyEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        RemotePlayer proxy = getProxyPlayer(entity);
        if (proxy == null) return;

        CompoundTag snapshot = entity.getCyberwareSnapshot();
        if (snapshot.isEmpty()) return;

        syncProxy(entity, proxy, snapshot);

        SkinModifierState state = SkinModifierManager.getPlayerSkinState(proxy);
        if (state == null) return;
        if (!state.hasModifiers() && !state.hasHighlights()) return;

        PlayerModel<JackedInPlayerBodyEntity> model = getParentModel();
        PlayerSkin.Model modelType = proxy.getSkin().model();

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

        try {
            applyVisibility(proxy, model);

            renderModifiers(state, modelType, model, poseStack, buffer, packedLight);
            renderHighlights(state, modelType, model, poseStack, buffer, packedLight);
        } finally {
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

    private static RemotePlayer getProxyPlayer(JackedInPlayerBodyEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        if (level == null) return null;

        if (proxyLevel != level) {
            PROXY_PLAYERS.clear();
            proxyLevel = level;
        }

        UUID ownerUUID = entity.getOwnerUUID();
        if (ownerUUID == null) return null;

        String ownerName = entity.hasCustomName() ? entity.getCustomName().getString() : entity.getName().getString();

        RemotePlayer existing = PROXY_PLAYERS.get(ownerUUID);

        if (existing != null && existing.getGameProfile().getName().equals(ownerName)) {
            return existing;
        }

        RemotePlayer created = new RemotePlayer(level, new GameProfile(ownerUUID, ownerName));
        PROXY_PLAYERS.put(ownerUUID, created);
        return created;
    }

    private static void syncProxy(JackedInPlayerBodyEntity entity, RemotePlayer proxy, CompoundTag snapshot) {
        proxy.tickCount = entity.tickCount;

        proxy.setPos(entity.getX(), entity.getY(), entity.getZ());
        proxy.setYRot(entity.getYRot());
        proxy.setXRot(entity.getXRot());
        proxy.yRotO = entity.yRotO;
        proxy.xRotO = entity.xRotO;
        proxy.yBodyRot = entity.yBodyRot;
        proxy.yBodyRotO = entity.yBodyRotO;
        proxy.yHeadRot = entity.yHeadRot;
        proxy.yHeadRotO = entity.yHeadRotO;

        CompoundTag persistentData = proxy.getPersistentData();
        persistentData.putBoolean(PlayerCyberwareData.HOLO_SNAPSHOT_FLAG, true);
        persistentData.put(PlayerCyberwareData.HOLO_SNAPSHOT_CYBERWARE, snapshot.copy());
        persistentData.putBoolean(SkinModifierManager.JACKED_IN_STANDIN_FLAG, true);
    }

    private static void applyVisibility(RemotePlayer proxy, PlayerModel<JackedInPlayerBodyEntity> model) {
        boolean leftArm = CyberwareLimbHider.shouldRenderLeftArm(proxy);
        boolean rightArm = CyberwareLimbHider.shouldRenderRightArm(proxy);
        boolean leftLeg = CyberwareLimbHider.shouldRenderLeftLeg(proxy);
        boolean rightLeg = CyberwareLimbHider.shouldRenderRightLeg(proxy);

        model.head.visible = true;
        model.hat.visible = true;
        model.body.visible = true;
        model.jacket.visible = true;

        model.leftArm.visible = leftArm;
        model.leftSleeve.visible = leftArm;
        model.rightArm.visible = rightArm;
        model.rightSleeve.visible = rightArm;

        model.leftLeg.visible = leftLeg;
        model.leftPants.visible = leftLeg;
        model.rightLeg.visible = rightLeg;
        model.rightPants.visible = rightLeg;
    }

    private static void renderModifiers(SkinModifierState state, PlayerSkin.Model modelType, PlayerModel<JackedInPlayerBodyEntity> model, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!state.hasModifiers()) return;

        for (SkinModifier modifier : state.getModifiers()) {
            if (modifier == null) continue;

            poseStack.pushPose();

            try {
                ResourceLocation texture = modifier.getTexture(modelType);
                int color = modifier.getColor();

                var baseVc = buffer.getBuffer(RenderType.entityTranslucent(texture));
                SkinOverlayModelRenderer.renderModifier(model, modifier, poseStack, baseVc, packedLight, OverlayTexture.NO_OVERLAY, color);

                if (modifier.hasGlint()) {
                    var glintVc = buffer.getBuffer(SkinRenderTypes.translucentGlintOverlay(texture));
                    SkinOverlayModelRenderer.renderModifier(model, modifier, poseStack, glintVc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                }
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static void renderHighlights(SkinModifierState state, PlayerSkin.Model modelType, PlayerModel<JackedInPlayerBodyEntity> model, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!state.hasHighlights()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        try {
            for (SkinHighlight highlight : state.getHighlights()) {
                if (highlight == null) continue;

                ResourceLocation texture = highlight.getTexture(modelType);

                RenderType renderType;
                int light;
                int color;

                if (highlight.isEmissive()) {
                    light = 0x00F000F0;

                    if (highlight.tintOnEmissive()) {
                        renderType = SkinRenderTypes.emissiveTinted(texture);
                        color = highlight.getColor();
                    } else {
                        renderType = RenderType.entityTranslucent(texture);
                        color = 0xFFFFFFFF;
                    }
                } else {
                    light = packedLight;
                    renderType = RenderType.entityTranslucent(texture);
                    color = highlight.getColor();
                }

                var vc = buffer.getBuffer(renderType);
                model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, color);
            }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }
}