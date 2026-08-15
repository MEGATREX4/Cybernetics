package com.perigrine3.createcybernetics.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CybereyeOpticsOverlayHandler {

    private CybereyeOpticsOverlayHandler() {}

    private static final Map<Key, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, NativeImage> SOURCES = new HashMap<>();

    private record Key(UUID playerId, ResourceLocation source, CybereyeVisualConfig.OpticsType type) {}

    private static final class Entry {
        final ResourceLocation textureId;
        final DynamicTexture texture;

        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;

        Entry(ResourceLocation textureId, DynamicTexture texture) {
            this.textureId = textureId;
            this.texture = texture;
        }
    }

    public static ResourceLocation getShifted(Player player, ResourceLocation source, CybereyeVisualConfig.OpticsType type) {
        if (player == null || source == null || type == null) return source;

        CybereyeVisualConfig.Layout layout = CybereyeOverlayHandler.getLayout(player);
        CybereyeVisualConfig.Offset offset = layout.getOffset(type);

        if (offset.x == 0 && offset.y == 0) {
            return source;
        }

        Key key = new Key(player.getUUID(), source, type);
        Entry entry = CACHE.get(key);

        if (entry != null && entry.lastX == offset.x && entry.lastY == offset.y) {
            return entry.textureId;
        }

        NativeImage sourceImage = getSource(source);
        if (sourceImage == null) return source;

        Minecraft mc = Minecraft.getInstance();

        if (entry == null) {
            ResourceLocation dynamicId = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID,
                    "dynamic/optics/" + type.id + "/" + player.getUUID() + "/" + sanitize(source));

            DynamicTexture texture = new DynamicTexture(sourceImage.getWidth(), sourceImage.getHeight(), true);
            texture.setFilter(false, false);

            mc.getTextureManager().register(dynamicId, texture);

            entry = new Entry(dynamicId, texture);
            CACHE.put(key, entry);
        }

        NativeImage output = entry.texture.getPixels();
        if (output == null) return source;

        clear(output);

        for (int y = 0; y < sourceImage.getHeight(); y++) {
            for (int x = 0; x < sourceImage.getWidth(); x++) {
                int rgba = sourceImage.getPixelRGBA(x, y);
                int alpha = (rgba >>> 24) & 0xFF;

                if (alpha == 0) continue;

                int targetX = x + offset.x;
                int targetY = y + offset.y;

                if (targetX < 0 || targetX >= output.getWidth()) continue;
                if (targetY < 0 || targetY >= output.getHeight()) continue;

                output.setPixelRGBA(targetX, targetY, rgba);
            }
        }

        entry.texture.upload();
        entry.lastX = offset.x;
        entry.lastY = offset.y;

        return entry.textureId;
    }

    public static void invalidate(UUID playerId) {
        if (playerId == null) return;

        for (Map.Entry<Key, Entry> cached : CACHE.entrySet()) {
            if (!cached.getKey().playerId().equals(playerId)) continue;

            cached.getValue().lastX = Integer.MIN_VALUE;
            cached.getValue().lastY = Integer.MIN_VALUE;
        }
    }

    public static void clearAll() {
        CACHE.clear();

        for (NativeImage image : SOURCES.values()) {
            image.close();
        }

        SOURCES.clear();
    }

    private static NativeImage getSource(ResourceLocation source) {
        NativeImage cached = SOURCES.get(source);

        if (cached != null) {
            return cached;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            Resource resource = mc.getResourceManager().getResourceOrThrow(source);

            try (InputStream input = resource.open()) {
                NativeImage image = NativeImage.read(input);
                SOURCES.put(source, image);
                return image;
            }
        } catch (Throwable throwable) {
            CreateCybernetics.LOGGER.error("Failed loading optics texture {}", source, throwable);
            return null;
        }
    }

    private static void clear(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setPixelRGBA(x, y, 0x00000000);
            }
        }
    }

    private static String sanitize(ResourceLocation id) {
        return (id.getNamespace() + "_" + id.getPath()).replace('/', '_').replace('.', '_');
    }
}