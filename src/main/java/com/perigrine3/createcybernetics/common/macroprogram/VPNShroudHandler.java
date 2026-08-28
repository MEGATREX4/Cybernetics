package com.perigrine3.createcybernetics.common.macroprogram;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class VPNShroudHandler {
    private static final int REFRESH_DURATION_TICKS = 10;

    private VPNShroudHandler() {}

    public static void tick(ServerPlayer player) {
        if (!MacroprogramExecutionManager.isActive(player, MacroprogramRegistry.VPN_SHROUD.id())) return;

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, REFRESH_DURATION_TICKS, 0, false, false, false));
    }
}