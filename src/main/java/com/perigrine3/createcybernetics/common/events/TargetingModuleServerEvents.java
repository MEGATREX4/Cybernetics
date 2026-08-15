package com.perigrine3.createcybernetics.common.events;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.cyberware.CyberwareVisualInterface;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.network.payload.TargetingHighlightPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TargetingModuleServerEvents {

    private static final int DURATION_TICKS = 45 * 20;

    private TargetingModuleServerEvents() {}

    @SubscribeEvent
    public static void onDamageDealt(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        var source = event.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (!CyberwareVisualInterface.isInstalledAndEnabled(data, ModItems.EYEUPGRADES_TARGETING.get(), CyberwareSlot.EYES)) return;
        if (!CyberwareVisualInterface.hasWorkingVisualInterface(data)) return;

        PacketDistributor.sendToPlayer(player, new TargetingHighlightPayload(target.getId(), DURATION_TICKS));
    }
}