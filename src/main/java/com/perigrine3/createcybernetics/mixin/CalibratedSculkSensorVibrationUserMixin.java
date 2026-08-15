package com.perigrine3.createcybernetics.mixin;

import com.perigrine3.createcybernetics.util.SculkedConversionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity$VibrationUser")
public abstract class CalibratedSculkSensorVibrationUserMixin {

    @Inject(method = "canReceiveVibration", at = @At("HEAD"), cancellable = true)
    private void createcybernetics$ignoreSculkedPlayers(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, GameEvent.Context context, CallbackInfoReturnable<Boolean> cir) {
        if (!(context.sourceEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (SculkedConversionHelper.hasAnyQualifyingSculkOrgan(player)) {
            cir.setReturnValue(false);
        }
    }
}