package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MacroprogramItemProperties {
    private static final ResourceLocation PULLING = ResourceLocation.withDefaultNamespace("pulling");
    private static final ResourceLocation PULL = ResourceLocation.withDefaultNamespace("pull");
    private static final ResourceLocation BLOCKING = ResourceLocation.withDefaultNamespace("blocking");

    private MacroprogramItemProperties() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowProperties(ModItems.MATERIALIZED_PING.get());
            registerBowProperties(ModItems.MATERIALIZED_DDOS.get());

            ItemProperties.register(ModItems.MATERIALIZED_FIREWALL.get(), BLOCKING, (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

    private static void registerBowProperties(Item item) {
        ItemProperties.register(item, PULLING, (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);

        ItemProperties.register(item, PULL, (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (entity.getUseItem() != stack) return 0.0F;

            return (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
        });
    }
}