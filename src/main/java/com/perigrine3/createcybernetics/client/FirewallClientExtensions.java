package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.renderer.item.MaterializedFirewallRenderer;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FirewallClientExtensions {
    private FirewallClientExtensions() {}

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private MaterializedFirewallRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MaterializedFirewallRenderer();
                }

                return renderer;
            }
        }, ModItems.MATERIALIZED_FIREWALL.get());
    }
}