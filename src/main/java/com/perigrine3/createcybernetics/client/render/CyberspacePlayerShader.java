package com.perigrine3.createcybernetics.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CyberspacePlayerShader {
    private static ShaderInstance shader;

    private CyberspacePlayerShader() {
    }

    public static ShaderInstance getShader() {
        return shader;
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace_player"),
                        DefaultVertexFormat.NEW_ENTITY
                ),
                loadedShader -> shader = loadedShader
        );
    }
}