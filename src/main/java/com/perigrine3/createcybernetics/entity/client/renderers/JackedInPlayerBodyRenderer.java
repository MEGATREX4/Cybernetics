package com.perigrine3.createcybernetics.entity.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.perigrine3.createcybernetics.client.render.JackedInPlayerBodyOverlayRenderer;
import com.perigrine3.createcybernetics.entity.custom.JackedInPlayerBodyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

public class JackedInPlayerBodyRenderer extends LivingEntityRenderer<JackedInPlayerBodyEntity, PlayerModel<JackedInPlayerBodyEntity>> {
    private static final int TETHER_SEGMENTS = 24;
    private static final double TETHER_WIDTH = 0.035D;
    private static final double TETHER_HEAD_OFFSET = 0.12D;
    private static final double TETHER_TOWER_FACE_OFFSET = 0.501D;
    private static final double MAX_TETHER_SAG = 1.5D;

    private final PlayerModel<JackedInPlayerBodyEntity> wideModel;
    private final PlayerModel<JackedInPlayerBodyEntity> slimModel;

    public JackedInPlayerBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new BreathingPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        this.wideModel = this.model;
        this.slimModel = new BreathingPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        this.addLayer(new JackedInPlayerBodyOverlayRenderer(this));
    }

    @Override
    public void render(JackedInPlayerBodyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        PlayerSkin skin = resolveSkin(entity);
        this.model = skin.model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        renderTether(entity, partialTick, poseStack, buffer, packedLight);
    }

    private void renderTether(JackedInPlayerBodyEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockPos tetherPos = entity.getTetherPos();
        if (tetherPos == null) return;

        Direction facing = entity.getTetherFacing();

        Vec3 entityPos = entity.getPosition(partialTick);
        Vec3 start = entity.getEyePosition(partialTick).add(0.0D, -TETHER_HEAD_OFFSET, 0.0D);

        Vec3 end = Vec3.atCenterOf(tetherPos).add(
                facing.getStepX() * TETHER_TOWER_FACE_OFFSET,
                0.0D,
                facing.getStepZ() * TETHER_TOWER_FACE_OFFSET
        );

        double distance = start.distanceTo(end);
        double sag = Math.min(MAX_TETHER_SAG, distance * 0.08D);

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i <= TETHER_SEGMENTS; i++) {
            double t = i / (double) TETHER_SEGMENTS;

            Vec3 point = getTetherPoint(start, end, t, sag);

            double previousT = Math.max(0.0D, t - 1.0D / TETHER_SEGMENTS);
            double nextT = Math.min(1.0D, t + 1.0D / TETHER_SEGMENTS);

            Vec3 previous = getTetherPoint(start, end, previousT, sag);
            Vec3 next = getTetherPoint(start, end, nextT, sag);

            Vec3 tangent = next.subtract(previous);

            if (tangent.lengthSqr() < 1.0E-7D) {
                tangent = end.subtract(start);
            }

            if (tangent.lengthSqr() < 1.0E-7D) {
                tangent = new Vec3(0.0D, 1.0D, 0.0D);
            }

            tangent = tangent.normalize();

            Vec3 view = cameraPos.subtract(point);

            if (view.lengthSqr() < 1.0E-7D) {
                view = new Vec3(0.0D, 0.0D, 1.0D);
            }

            Vec3 side = tangent.cross(view);

            if (side.lengthSqr() < 1.0E-7D) {
                side = tangent.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }

            if (side.lengthSqr() < 1.0E-7D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            }

            side = side.normalize().scale(TETHER_WIDTH);

            Vec3 localPoint = point.subtract(entityPos);
            Vec3 first = localPoint.add(side);
            Vec3 second = localPoint.subtract(side);

            vertexConsumer.addVertex(matrix, (float) first.x, (float) first.y, (float) first.z).setColor(0, 0, 0, 255).setLight(packedLight);
            vertexConsumer.addVertex(matrix, (float) second.x, (float) second.y, (float) second.z).setColor(0, 0, 0, 255).setLight(packedLight);
        }
    }

    private static Vec3 getTetherPoint(Vec3 start, Vec3 end, double t, double sag) {
        double x = Mth.lerp(t, start.x, end.x);
        double y = Mth.lerp(t, start.y, end.y);
        double z = Mth.lerp(t, start.z, end.z);

        y -= sag * 4.0D * t * (1.0D - t);

        return new Vec3(x, y, z);
    }

    @Override
    public ResourceLocation getTextureLocation(JackedInPlayerBodyEntity entity) {
        return resolveSkin(entity).texture();
    }

    private PlayerSkin resolveSkin(JackedInPlayerBodyEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID();

        if (ownerUUID == null) {
            return DefaultPlayerSkin.get(entity.getUUID());
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(ownerUUID);

            if (info != null) {
                return info.getSkin();
            }
        }

        return DefaultPlayerSkin.get(ownerUUID);
    }

    private static final class BreathingPlayerModel extends PlayerModel<JackedInPlayerBodyEntity> {
        private BreathingPlayerModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(JackedInPlayerBodyEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            float breathe = Mth.sin(ageInTicks * 0.1F);

            this.body.xRot += breathe * 0.01F;
            this.rightArm.xRot += breathe * 0.015F;
            this.leftArm.xRot += breathe * 0.015F;

            this.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.025F;
            this.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.025F;
        }
    }
}