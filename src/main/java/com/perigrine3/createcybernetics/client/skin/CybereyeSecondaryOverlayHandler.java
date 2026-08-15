package com.perigrine3.createcybernetics.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CybereyeSecondaryOverlayHandler {

    private CybereyeSecondaryOverlayHandler() {}

    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

    private static final class Entry {
        final ResourceLocation combinedId;
        final ResourceLocation primaryId;
        final ResourceLocation secondaryId;

        final DynamicTexture combined;
        final DynamicTexture primary;
        final DynamicTexture secondary;

        int lastHash;

        Entry(ResourceLocation combinedId, ResourceLocation primaryId, ResourceLocation secondaryId, DynamicTexture combined, DynamicTexture primary, DynamicTexture secondary) {
            this.combinedId = combinedId;
            this.primaryId = primaryId;
            this.secondaryId = secondaryId;
            this.combined = combined;
            this.primary = primary;
            this.secondary = secondary;
            this.lastHash = Integer.MIN_VALUE;
        }
    }

    public static ResourceLocation getOrBuildCombinedHighlight(Player player) {
        Entry entry = getOrBuildEntry(player);
        return entry == null ? null : entry.combinedId;
    }

    public static ResourceLocation getOrBuildPrimaryHighlight(Player player) {
        Entry entry = getOrBuildEntry(player);
        return entry == null ? null : entry.primaryId;
    }

    public static ResourceLocation getOrBuildSecondaryOverlay(Player player) {
        Entry entry = getOrBuildEntry(player);
        return entry == null ? null : entry.secondaryId;
    }

    private static Entry getOrBuildEntry(Player player) {
        if (player == null) return null;

        CybereyeVisualConfig.Layout layout = CybereyeOverlayHandler.getLayout(player);

        int hash = 31 * Arrays.hashCode(layout.primary.copyBits()) + Arrays.hashCode(layout.secondary.copyBits());
        UUID playerId = player.getUUID();

        Entry entry = CACHE.get(playerId);

        if (entry != null && entry.lastHash == hash) {
            return entry;
        }

        Minecraft mc = Minecraft.getInstance();

        if (entry == null) {
            ResourceLocation combinedId = dynamicId("combined", playerId);
            ResourceLocation primaryId = dynamicId("primary", playerId);
            ResourceLocation secondaryId = dynamicId("secondary", playerId);

            DynamicTexture combined = new DynamicTexture(64, 64, true);
            DynamicTexture primary = new DynamicTexture(64, 64, true);
            DynamicTexture secondary = new DynamicTexture(64, 64, true);

            combined.setFilter(false, false);
            primary.setFilter(false, false);
            secondary.setFilter(false, false);

            mc.getTextureManager().register(combinedId, combined);
            mc.getTextureManager().register(primaryId, primary);
            mc.getTextureManager().register(secondaryId, secondary);

            entry = new Entry(combinedId, primaryId, secondaryId, combined, primary, secondary);
            CACHE.put(playerId, entry);
        }

        NativeImage combinedImage = entry.combined.getPixels();
        NativeImage primaryImage = entry.primary.getPixels();
        NativeImage secondaryImage = entry.secondary.getPixels();

        if (combinedImage == null || primaryImage == null || secondaryImage == null) return null;

        clear(combinedImage);
        clear(primaryImage);
        clear(secondaryImage);

        for (int v = 0; v < CybereyeVisualConfig.HEAD_UV_HEIGHT; v++) {
            for (int u = 0; u < CybereyeVisualConfig.HEAD_UV_WIDTH; u++) {
                boolean primary = layout.primary.get(u, v);
                boolean secondary = layout.secondary.get(u, v);

                if (primary) {
                    writeWhite(primaryImage, u, v);
                }

                if (secondary) {
                    writeWhite(secondaryImage, u, v);
                }

                if (primary || secondary) {
                    writeWhite(combinedImage, u, v);
                }
            }
        }

        entry.combined.upload();
        entry.primary.upload();
        entry.secondary.upload();

        entry.lastHash = hash;

        return entry;
    }

    public static void invalidate(UUID playerId) {
        if (playerId == null) return;

        Entry entry = CACHE.get(playerId);

        if (entry != null) {
            entry.lastHash = Integer.MIN_VALUE;
        }
    }

    public static void clearAll() {
        CACHE.clear();
    }

    private static ResourceLocation dynamicId(String channel, UUID playerId) {
        return ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "dynamic/cybereyes/" + channel + "/" + playerId);
    }

    private static void writeWhite(NativeImage image, int x, int y) {
        image.setPixelRGBA(x, y, 0xFFFFFFFF);
    }

    private static void clear(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setPixelRGBA(x, y, 0x00000000);
            }
        }
    }
}