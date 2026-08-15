package com.perigrine3.createcybernetics.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CybereyeOverlayHandler {

    private CybereyeOverlayHandler() {}

    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, CompoundTag> SYNCED_CONFIGS = new ConcurrentHashMap<>();

    private static final class Entry {
        final ResourceLocation textureId;
        final DynamicTexture texture;
        int lastHash;

        Entry(ResourceLocation textureId, DynamicTexture texture) {
            this.textureId = textureId;
            this.texture = texture;
            this.lastHash = Integer.MIN_VALUE;
        }
    }

    public static CybereyeVisualConfig.Layout getLayout(Player player) {
        if (player == null) return CybereyeVisualConfig.defaults();

        CompoundTag root = SYNCED_CONFIGS.get(player.getUUID());

        if (root == null || root.isEmpty()) {
            root = player.getPersistentData().getCompound(CybereyeVisualConfig.NBT_ROOT);
        }

        return CybereyeVisualConfig.readFromNbt(root);
    }

    public static ResourceLocation getOrBuildOverlay(Player player) {
        if (player == null) return null;

        CybereyeVisualConfig.Layout layout = getLayout(player);

        int hash = 31 * Arrays.hashCode(layout.primary.copyBits()) + Arrays.hashCode(layout.secondary.copyBits());
        UUID playerId = player.getUUID();

        Entry entry = CACHE.get(playerId);

        if (entry != null && entry.lastHash == hash) {
            return entry.textureId;
        }

        Minecraft mc = Minecraft.getInstance();

        if (entry == null) {
            ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "dynamic/cybereyes/base/" + playerId);
            DynamicTexture texture = new DynamicTexture(64, 64, true);
            texture.setFilter(false, false);

            mc.getTextureManager().register(textureId, texture);

            entry = new Entry(textureId, texture);
            CACHE.put(playerId, entry);
        }

        NativeImage image = entry.texture.getPixels();
        if (image == null) return null;

        clear(image);

        for (int v = 0; v < CybereyeVisualConfig.HEAD_UV_HEIGHT; v++) {
            for (int u = 0; u < CybereyeVisualConfig.HEAD_UV_WIDTH; u++) {
                if (!layout.primary.get(u, v) && !layout.secondary.get(u, v)) continue;

                image.setPixelRGBA(u, v, 0xFFFFFFFF);
            }
        }

        entry.texture.upload();
        entry.lastHash = hash;

        return entry.textureId;
    }

    public static void applySyncedConfig(UUID playerId, CompoundTag root) {
        if (playerId == null || root == null) return;

        CompoundTag copy = root.copy();

        SYNCED_CONFIGS.put(playerId, copy);
        invalidate(playerId);

        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(playerId);

            if (player != null) {
                player.getPersistentData().put(CybereyeVisualConfig.NBT_ROOT, copy.copy());
            }
        }
    }

    public static void invalidate(Player player) {
        if (player == null) return;

        invalidate(player.getUUID());
    }

    public static void invalidate(UUID playerId) {
        if (playerId == null) return;

        Entry entry = CACHE.get(playerId);

        if (entry != null) {
            entry.lastHash = Integer.MIN_VALUE;
        }

        CybereyeSecondaryOverlayHandler.invalidate(playerId);
        CybereyeOpticsOverlayHandler.invalidate(playerId);
    }

    public static void clearAll() {
        CACHE.clear();
        SYNCED_CONFIGS.clear();

        CybereyeSecondaryOverlayHandler.clearAll();
        CybereyeOpticsOverlayHandler.clearAll();
    }

    private static void clear(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setPixelRGBA(x, y, 0x00000000);
            }
        }
    }
}