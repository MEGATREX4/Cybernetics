package com.perigrine3.createcybernetics.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.perigrine3.createcybernetics.client.animation.HeavyKnucklesClientAnimation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererHeavyKnucklesMixin {

    @Inject(method = "renderHand", at = @At(
            value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
            ordinal = 0, shift = At.Shift.BEFORE
            )
    )
    private void createcybernetics$applyHeavyKnucklesFirstPersonArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        PlayerRenderer renderer = (PlayerRenderer)(Object)this;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();

        HumanoidArm side = arm == model.rightArm ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        HeavyKnucklesClientAnimation.applyFirstPersonArm(player, arm, side);
    }

    @Inject(method = "renderHand", at = @At(
            value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
            ordinal = 1, shift = At.Shift.BEFORE
            )
    )
    private void createcybernetics$applyHeavyKnucklesFirstPersonSleeve(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        PlayerRenderer renderer = (PlayerRenderer)(Object)this;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();

        HumanoidArm side = arm == model.rightArm ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        HeavyKnucklesClientAnimation.applyFirstPersonArm(player, sleeve, side);
    }
}