package com.perigrine3.createcybernetics.entity.ai.goal;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.entity.custom.JackedInPlayerBodyEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class JackedInPlayerBodyTargeting {
    private JackedInPlayerBodyTargeting() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob instanceof Enemy)) return;

        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, JackedInPlayerBodyEntity.class, true));
    }
}