package com.perigrine3.createcybernetics.client.cyberspace;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT)
public final class CyberspaceShaderEvents {

    private static final ResourceLocation CYBERSPACE_POST =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "shaders/post/cyberspace.json");

    private static PostChain cyberspacePostChain;
    private static int cyberspacePostWidth = -1;
    private static int cyberspacePostHeight = -1;

    private CyberspaceShaderEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (!isInCyberspace(mc)) {
            closeCyberspacePostChain();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (!isInCyberspace(mc)) {
            return;
        }

        if (!ensureCyberspacePostChain(mc)) {
            return;
        }

        updateCyberspaceUniforms(mc);
        processCyberspacePass(mc);
    }

    private static void updateCyberspaceUniforms(Minecraft mc) {
        PostChain chain = cyberspacePostChain;

        if (chain == null) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        if (width != cyberspacePostWidth || height != cyberspacePostHeight) {
            chain.resize(width, height);
            cyberspacePostWidth = width;
            cyberspacePostHeight = height;
        }

        chain.setUniform("PosterizeSteps", 16.0F);
        chain.setUniform("Contrast", 1.08F);
        chain.setUniform("ShadowGamma", 1.10F);
        chain.setUniform("DarkTintStrength", 0.35F);
        chain.setUniform("ScanlineStrength", 0.25F);
    }

    private static void processCyberspacePass(Minecraft mc) {
        PostChain chain = cyberspacePostChain;

        if (chain == null) {
            return;
        }

        try {
            chain.process(0.0F);
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Throwable ignored) {
            closeCyberspacePostChain();
            mc.getMainRenderTarget().bindWrite(false);
        }
    }

    private static boolean ensureCyberspacePostChain(Minecraft mc) {
        if (cyberspacePostChain != null) {
            return true;
        }

        try {
            cyberspacePostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CYBERSPACE_POST);

            cyberspacePostWidth = mc.getWindow().getWidth();
            cyberspacePostHeight = mc.getWindow().getHeight();

            cyberspacePostChain.resize(cyberspacePostWidth, cyberspacePostHeight);

            return true;
        } catch (Throwable ignored) {
            closeCyberspacePostChain();
            return false;
        }
    }

    private static void closeCyberspacePostChain() {
        if (cyberspacePostChain != null) {
            try {
                cyberspacePostChain.close();
            } catch (Throwable ignored) {
            }
        }

        cyberspacePostChain = null;
        cyberspacePostWidth = -1;
        cyberspacePostHeight = -1;
    }

    private static boolean isInCyberspace(Minecraft mc) {
        return mc.player != null
                && mc.level != null
                && mc.level.dimension().equals(ModDimensions.CYBERSPACE_LEVEL);
    }
}