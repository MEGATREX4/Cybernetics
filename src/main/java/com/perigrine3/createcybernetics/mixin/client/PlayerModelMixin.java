package com.perigrine3.createcybernetics.mixin.client;

import com.perigrine3.createcybernetics.client.animation.HeavyKnucklesClientAnimation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void createcybernetics$applyHeavyKnucklesAnimation(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        HeavyKnucklesClientAnimation.applyThirdPerson(model, player, ageInTicks);
    }
}