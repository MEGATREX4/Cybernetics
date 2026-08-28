package com.perigrine3.createcybernetics.client.cyberspace;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CyberspaceDimensionEffects {

    public static final ResourceLocation EFFECTS_ID =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace");

    private CyberspaceDimensionEffects() {}

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(EFFECTS_ID, new DimensionSpecialEffects(
                Float.NaN,
                false,
                DimensionSpecialEffects.SkyType.NONE,
                false,
                false
        ) {
            @Override
            public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
                return color;
            }

            @Override
            public boolean isFoggyAt(int x, int z) {
                return false;
            }
        });
    }
}