package com.perigrine3.createcybernetics.client.render.rejection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.client.skin.CybereyeOpticsOverlayHandler;
import com.perigrine3.createcybernetics.client.skin.CybereyeSecondaryOverlayHandler;
import com.perigrine3.createcybernetics.client.skin.CybereyeUndyedGlitchOverlayHandler;
import com.perigrine3.createcybernetics.client.skin.SkinRenderTypes;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CyberwareRejectionEyeGlitchRenderer {

    private static final ResourceLocation MONOVISION = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/monovision.png");
    private static final ResourceLocation MONOVISION_DYED = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/monovision_dyed.png");
    private static final ResourceLocation MONOVISION_HIGHLIGHT = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/monovision_highlight.png");

    private static final ResourceLocation MULTIOPTICS1 = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics1.png");
    private static final ResourceLocation MULTIOPTICS1_DYED = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics1_dyed.png");
    private static final ResourceLocation MULTIOPTICS1_HIGHLIGHT = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics1_highlight.png");

    private static final ResourceLocation MULTIOPTICS2 = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics2.png");
    private static final ResourceLocation MULTIOPTICS2_DYED = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics2_dyed.png");
    private static final ResourceLocation MULTIOPTICS2_HIGHLIGHT = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics2_highlight.png");

    private static final ResourceLocation MULTIOPTICS3 = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics3.png");
    private static final ResourceLocation MULTIOPTICS3_DYED = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics3_dyed.png");
    private static final ResourceLocation MULTIOPTICS3_HIGHLIGHT = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics3_highlight.png");

    private static final ResourceLocation MULTIOPTICS4 = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics4.png");
    private static final ResourceLocation MULTIOPTICS4_DYED = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics4_dyed.png");
    private static final ResourceLocation MULTIOPTICS4_HIGHLIGHT = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/multioptics4_highlight.png");

    private static final int FULL_BRIGHT = 0x00F000F0;

    private static final int LEVEL_1_MIN_ACTIVE_TICKS = 40;
    private static final int LEVEL_1_MAX_ACTIVE_TICKS = 65;
    private static final int LEVEL_1_MIN_CLEAR_TICKS = 180;
    private static final int LEVEL_1_MAX_CLEAR_TICKS = 360;

    private static final int LEVEL_2_MIN_ACTIVE_TICKS = 50;
    private static final int LEVEL_2_MAX_ACTIVE_TICKS = 85;
    private static final int LEVEL_2_MIN_CLEAR_TICKS = 80;
    private static final int LEVEL_2_MAX_CLEAR_TICKS = 180;

    private static final int LEVEL_3_MIN_ACTIVE_TICKS = 60;
    private static final int LEVEL_3_MAX_ACTIVE_TICKS = 110;
    private static final int LEVEL_3_MIN_CLEAR_TICKS = 15;
    private static final int LEVEL_3_MAX_CLEAR_TICKS = 70;

    private static final float LEVEL_1_VIBRATION_DISTANCE = 0.006F;
    private static final float LEVEL_2_VIBRATION_DISTANCE = 0.014F;
    private static final float LEVEL_3_VIBRATION_DISTANCE = 0.027F;
    private static final float FUGUE_VIBRATION_DISTANCE = 0.040F;

    private static final float LEVEL_1_VIBRATION_SPEED = 0.75F;
    private static final float LEVEL_2_VIBRATION_SPEED = 1.50F;
    private static final float LEVEL_3_VIBRATION_SPEED = 2.75F;
    private static final float FUGUE_VIBRATION_SPEED = 4.00F;

    private static final Map<UUID, GlitchState> STATES = new HashMap<>();

    private CyberwareRejectionEyeGlitchRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, int level, boolean fugue, float partialTick) {
        if (player == null || level <= 0) return;
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        updateState(player, level, fugue);

        if (!fugue && !isGlitchActive(player)) return;

        List<EyeLayer> layers = getEyeLayers(player, data);
        if (layers.isEmpty()) return;

        float time = player.tickCount + partialTick;
        float distance = getVibrationDistance(level, fugue);
        float speed = getVibrationSpeed(level, fugue);

        int phase = Mth.floor(time * speed);

        float offsetX = (hash(player.getId() + 1301, phase) - 0.5F) * 2.0F * distance;
        float offsetY = (hash(player.getId() + 1429, phase) - 0.5F) * 2.0F * distance * 0.60F;
        float offsetZ = (hash(player.getId() + 1553, phase) - 0.5F) * 2.0F * distance * 0.18F;

        renderEyeLayers(poseStack, bufferSource, model, layers, offsetX, offsetY, offsetZ);
    }

    private static void updateState(AbstractClientPlayer player, int level, boolean fugue) {
        UUID playerId = player.getUUID();
        GlitchState state = STATES.computeIfAbsent(playerId, id -> new GlitchState());
        int now = player.tickCount;

        if (state.lastTick == now && state.level == level && state.fugue == fugue) return;

        state.lastTick = now;

        if (state.level != level || state.fugue != fugue) {
            state.level = level;
            state.fugue = fugue;
            state.active = false;
            state.activeUntilTick = -1;
            state.nextActiveTick = fugue ? now : now + getClearDuration(player, level, state.episodeIndex++);
        }

        if (fugue) {
            state.active = true;
            state.activeUntilTick = Integer.MAX_VALUE;
            state.nextActiveTick = -1;
            return;
        }

        if (state.active) {
            if (now >= state.activeUntilTick) {
                state.active = false;
                state.activeUntilTick = -1;
                state.nextActiveTick = now + getClearDuration(player, level, state.episodeIndex++);
            }

            return;
        }

        if (state.nextActiveTick < 0) {
            state.nextActiveTick = now + getClearDuration(player, level, state.episodeIndex++);
            return;
        }

        if (now >= state.nextActiveTick) {
            state.active = true;
            state.activeUntilTick = now + getActiveDuration(player, level, state.episodeIndex++);
            state.nextActiveTick = -1;
        }
    }

    private static boolean isGlitchActive(AbstractClientPlayer player) {
        GlitchState state = STATES.get(player.getUUID());
        return state != null && state.active;
    }

    private static int getActiveDuration(AbstractClientPlayer player, int level, int episode) {
        int minimum;
        int maximum;

        if (level <= 1) {
            minimum = LEVEL_1_MIN_ACTIVE_TICKS;
            maximum = LEVEL_1_MAX_ACTIVE_TICKS;
        } else if (level == 2) {
            minimum = LEVEL_2_MIN_ACTIVE_TICKS;
            maximum = LEVEL_2_MAX_ACTIVE_TICKS;
        } else {
            minimum = LEVEL_3_MIN_ACTIVE_TICKS;
            maximum = LEVEL_3_MAX_ACTIVE_TICKS;
        }

        return randomRange(player.getId() + 1699, episode, minimum, maximum);
    }

    private static int getClearDuration(AbstractClientPlayer player, int level, int episode) {
        int minimum;
        int maximum;

        if (level <= 1) {
            minimum = LEVEL_1_MIN_CLEAR_TICKS;
            maximum = LEVEL_1_MAX_CLEAR_TICKS;
        } else if (level == 2) {
            minimum = LEVEL_2_MIN_CLEAR_TICKS;
            maximum = LEVEL_2_MAX_CLEAR_TICKS;
        } else {
            minimum = LEVEL_3_MIN_CLEAR_TICKS;
            maximum = LEVEL_3_MAX_CLEAR_TICKS;
        }

        return randomRange(player.getId() + 1877, episode, minimum, maximum);
    }

    private static float getVibrationDistance(int level, boolean fugue) {
        if (fugue) return FUGUE_VIBRATION_DISTANCE;

        return switch (level) {
            case 1 -> LEVEL_1_VIBRATION_DISTANCE;
            case 2 -> LEVEL_2_VIBRATION_DISTANCE;
            default -> LEVEL_3_VIBRATION_DISTANCE;
        };
    }

    private static float getVibrationSpeed(int level, boolean fugue) {
        if (fugue) return FUGUE_VIBRATION_SPEED;

        return switch (level) {
            case 1 -> LEVEL_1_VIBRATION_SPEED;
            case 2 -> LEVEL_2_VIBRATION_SPEED;
            default -> LEVEL_3_VIBRATION_SPEED;
        };
    }

    private static List<EyeLayer> getEyeLayers(AbstractClientPlayer player, PlayerCyberwareData data) {
        if (data.hasSpecificItem(ModItems.EYEUPGRADES_MONOVISION.get(), CyberwareSlot.EYES)) {
            return getOpticsLayers(player, data, ModItems.EYEUPGRADES_MONOVISION.get(), CybereyeVisualConfig.OpticsType.MONOVISION, MONOVISION, MONOVISION_DYED, MONOVISION_HIGHLIGHT, 0xFFFFFFFF);
        }

        if (data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS1.get(), CyberwareSlot.EYES)) {
            return getOpticsLayers(player, data, ModItems.EYEUPGRADES_MULTIOPTICS1.get(), CybereyeVisualConfig.OpticsType.MULTIOPTICS_1, MULTIOPTICS1, MULTIOPTICS1_DYED, MULTIOPTICS1_HIGHLIGHT, 0xFFB00000);
        }

        if (data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS2.get(), CyberwareSlot.EYES)) {
            return getOpticsLayers(player, data, ModItems.EYEUPGRADES_MULTIOPTICS2.get(), CybereyeVisualConfig.OpticsType.MULTIOPTICS_2, MULTIOPTICS2, MULTIOPTICS2_DYED, MULTIOPTICS2_HIGHLIGHT, 0xFFB00000);
        }

        if (data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS3.get(), CyberwareSlot.EYES)) {
            return getOpticsLayers(player, data, ModItems.EYEUPGRADES_MULTIOPTICS3.get(), CybereyeVisualConfig.OpticsType.MULTIOPTICS_3, MULTIOPTICS3, MULTIOPTICS3_DYED, MULTIOPTICS3_HIGHLIGHT, 0xFFB00000);
        }

        if (data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS4.get(), CyberwareSlot.EYES)) {
            return getOpticsLayers(player, data, ModItems.EYEUPGRADES_MULTIOPTICS4.get(), CybereyeVisualConfig.OpticsType.MULTIOPTICS_4, MULTIOPTICS4, MULTIOPTICS4_DYED, MULTIOPTICS4_HIGHLIGHT, 0xFFB00000);
        }

        if (data.hasSpecificItem(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES)) {
            return getCybereyeLayers(player, data);
        }

        return List.of();
    }

    private static List<EyeLayer> getCybereyeLayers(AbstractClientPlayer player, PlayerCyberwareData data) {
        boolean hasPrimaryDye = data.isDyed(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES);
        boolean hasSecondaryDye = data.isSecondaryDyed(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES);

        if (!hasPrimaryDye && !hasSecondaryDye) {
            ResourceLocation undyedEyes = CybereyeUndyedGlitchOverlayHandler.getOrBuild(player);

            if (undyedEyes == null) {
                return List.of();
            }

            return List.of(new EyeLayer(undyedEyes, 0xFFFFFFFF, false));
        }

        List<EyeLayer> layers = new ArrayList<>();

        if (hasPrimaryDye && hasSecondaryDye) {
            int primaryTint = data.dyeColor(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES);
            int secondaryTint = data.secondaryDyeColor(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES);

            ResourceLocation primaryOverlay = CybereyeSecondaryOverlayHandler.getOrBuildPrimaryHighlight(player);
            ResourceLocation secondaryOverlay = CybereyeSecondaryOverlayHandler.getOrBuildSecondaryOverlay(player);

            addModifierAndHighlight(layers, primaryOverlay, primaryTint);
            addModifierAndHighlight(layers, secondaryOverlay, secondaryTint);

            return layers;
        }

        if (hasPrimaryDye) {
            int primaryTint = data.dyeColor(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES);
            ResourceLocation combinedOverlay = CybereyeSecondaryOverlayHandler.getOrBuildCombinedHighlight(player);

            addModifierAndHighlight(layers, combinedOverlay, primaryTint);

            return layers;
        }

        int secondaryTint = data.secondaryDyeColor(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES);
        ResourceLocation secondaryOverlay = CybereyeSecondaryOverlayHandler.getOrBuildSecondaryOverlay(player);

        addModifierAndHighlight(layers, secondaryOverlay, secondaryTint);

        return layers;
    }

    private static List<EyeLayer> getOpticsLayers(AbstractClientPlayer player, PlayerCyberwareData data, Item opticsItem, CybereyeVisualConfig.OpticsType type, ResourceLocation baseTexture, ResourceLocation dyedTexture, ResourceLocation highlightTexture, int defaultHighlightColor) {
        List<EyeLayer> layers = new ArrayList<>();

        ResourceLocation shiftedBase = CybereyeOpticsOverlayHandler.getShifted(player, baseTexture, type);
        ResourceLocation shiftedHighlight = CybereyeOpticsOverlayHandler.getShifted(player, highlightTexture, type);

        layers.add(new EyeLayer(shiftedBase, 0xFFFFFFFF, false));

        if (data.isDyed(opticsItem, CyberwareSlot.EYES)) {
            int tint = data.dyeColor(opticsItem, CyberwareSlot.EYES);
            ResourceLocation shiftedDyed = CybereyeOpticsOverlayHandler.getShifted(player, dyedTexture, type);

            layers.add(new EyeLayer(shiftedDyed, tint, false));
            layers.add(new EyeLayer(shiftedHighlight, tint, true));
        } else {
            layers.add(new EyeLayer(shiftedHighlight, defaultHighlightColor, true));
        }

        return layers;
    }

    private static void addModifierAndHighlight(List<EyeLayer> layers, ResourceLocation texture, int color) {
        if (texture == null) return;

        layers.add(new EyeLayer(texture, color, false));
        layers.add(new EyeLayer(texture, color, true));
    }

    private static void renderEyeLayers(PoseStack poseStack, MultiBufferSource bufferSource, PlayerModel<AbstractClientPlayer> model, List<EyeLayer> layers, float offsetX, float offsetY, float offsetZ) {
        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);

        for (EyeLayer layer : layers) {
            RenderType renderType = layer.highlight() ? SkinRenderTypes.emissiveTinted(layer.texture()) : RenderType.entityTranslucent(layer.texture());
            VertexConsumer consumer = bufferSource.getBuffer(renderType);

            model.head.render(poseStack, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, layer.color());
        }

        poseStack.popPose();
    }

    private static int randomRange(int seed, int phase, int minimum, int maximum) {
        if (maximum <= minimum) return minimum;

        float roll = hash(seed, phase);
        return minimum + Mth.floor(roll * (maximum - minimum + 1));
    }

    private static float hash(int seed, int phase) {
        int value = seed;
        value = value * 31 + phase;
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        value ^= value >>> 16;

        return (value & 0x7FFFFFFF) / (float) Integer.MAX_VALUE;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }

    private static final class GlitchState {
        int level = 0;
        boolean fugue = false;
        boolean active = false;
        int activeUntilTick = -1;
        int nextActiveTick = -1;
        int episodeIndex = 0;
        int lastTick = -1;
    }

    private record EyeLayer(ResourceLocation texture, int color, boolean highlight) {
    }
}