package com.perigrine3.createcybernetics.client;

import com.google.gson.*;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.skin.CybereyeOverlayHandler;
import com.perigrine3.createcybernetics.common.cybereyes.CybereyeVisualConfig;
import com.perigrine3.createcybernetics.network.payload.CybereyeIrisSyncC2SPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CybereyeIrisConfigClient {

    private CybereyeIrisConfigClient() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CONFIG_VERSION = 2;

    private static final Map<UUID, CybereyeVisualConfig.Layout> CACHE = new ConcurrentHashMap<>();

    public static CybereyeVisualConfig.Layout get(UUID playerId) {
        return CACHE.computeIfAbsent(playerId, CybereyeIrisConfigClient::loadOrDefault);
    }

    public static void reload(UUID playerId) {
        CACHE.put(playerId, loadOrDefault(playerId));
    }

    public static void invalidate(UUID playerId) {
        CACHE.remove(playerId);
    }

    public static void reset(UUID playerId) {
        try {
            Files.deleteIfExists(fileFor(playerId));
        } catch (Throwable ignored) {
        }

        CACHE.put(playerId, CybereyeVisualConfig.defaults());
    }

    public static void save(UUID playerId, CybereyeVisualConfig.Layout layout) {
        if (playerId == null || layout == null) return;

        CybereyeVisualConfig.Layout copy = layout.copy();
        CACHE.put(playerId, copy);

        Path file = fileFor(playerId);

        try {
            Files.createDirectories(file.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("version", CONFIG_VERSION);
            root.add("primaryPixels", writePixels(copy.primary));
            root.add("secondaryPixels", writePixels(copy.secondary));

            JsonObject optics = new JsonObject();

            for (CybereyeVisualConfig.OpticsType type : CybereyeVisualConfig.OpticsType.values()) {
                CybereyeVisualConfig.Offset offset = copy.getOffset(type);

                JsonObject entry = new JsonObject();
                entry.addProperty("x", offset.x);
                entry.addProperty("y", offset.y);

                optics.add(type.id, entry);
            }

            root.add("optics", optics);

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (Throwable throwable) {
            CreateCybernetics.LOGGER.error("Failed to save cybereye visual configuration for {}", playerId, throwable);
        }
    }

    public static void applyLocalPreview(LocalPlayer player, CybereyeVisualConfig.Layout layout) {
        if (player == null || layout == null) return;

        CompoundTag root = CybereyeVisualConfig.writeToNbt(layout);
        player.getPersistentData().put(CybereyeVisualConfig.NBT_ROOT, root);
        CybereyeOverlayHandler.invalidate(player);
    }

    public static void applyCachedConfigToPlayer(LocalPlayer player) {
        if (player == null) return;

        applyLocalPreview(player, get(player.getUUID()));
    }

    public static void saveAndSync(LocalPlayer player, CybereyeVisualConfig.Layout layout) {
        if (player == null || layout == null) return;

        save(player.getUUID(), layout);
        applyLocalPreview(player, layout);
        PacketDistributor.sendToServer(CybereyeIrisSyncC2SPayload.from(layout));
    }

    private static CybereyeVisualConfig.Layout loadOrDefault(UUID playerId) {
        Path file = fileFor(playerId);

        if (!Files.exists(file)) {
            return CybereyeVisualConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return CybereyeVisualConfig.defaults();

            if (root.has("primaryPixels") || root.has("secondaryPixels") || root.has("optics")) {
                return readModern(root);
            }

            CybereyeVisualConfig.Layout migrated = readLegacy(root);
            save(playerId, migrated);

            return migrated;
        } catch (Throwable throwable) {
            CreateCybernetics.LOGGER.error("Failed to load cybereye visual configuration for {}", playerId, throwable);
            return CybereyeVisualConfig.defaults();
        }
    }

    private static CybereyeVisualConfig.Layout readModern(JsonObject root) {
        CybereyeVisualConfig.Layout layout = new CybereyeVisualConfig.Layout();

        if (root.has("primaryPixels") && root.get("primaryPixels").isJsonArray()) {
            readPixels(root.getAsJsonArray("primaryPixels"), layout.primary);
        }

        if (root.has("secondaryPixels") && root.get("secondaryPixels").isJsonArray()) {
            readPixels(root.getAsJsonArray("secondaryPixels"), layout.secondary);
        }

        if (root.has("optics") && root.get("optics").isJsonObject()) {
            JsonObject optics = root.getAsJsonObject("optics");

            for (CybereyeVisualConfig.OpticsType type : CybereyeVisualConfig.OpticsType.values()) {
                if (!optics.has(type.id) || !optics.get(type.id).isJsonObject()) continue;

                JsonObject entry = optics.getAsJsonObject(type.id);

                int x = entry.has("x") ? entry.get("x").getAsInt() : 0;
                int y = entry.has("y") ? entry.get("y").getAsInt() : 0;

                layout.optics.put(type, new CybereyeVisualConfig.Offset(x, y));
            }
        }

        return layout;
    }

    private static CybereyeVisualConfig.Layout readLegacy(JsonObject root) {
        CybereyeVisualConfig.Layout layout = new CybereyeVisualConfig.Layout();

        migrateLegacyEye(root, "left", layout.primary, 2, 4);
        migrateLegacyEye(root, "right", layout.secondary, 5, 4);

        return layout;
    }

    private static void migrateLegacyEye(JsonObject root, String key, CybereyeVisualConfig.PixelMask target, int defaultLocalX, int defaultLocalY) {
        int x = defaultLocalX;
        int y = defaultLocalY;
        int width = 1;
        int height = 1;

        if (root.has(key) && root.get(key).isJsonObject()) {
            JsonObject eye = root.getAsJsonObject(key);

            x = eye.has("x") ? eye.get("x").getAsInt() : defaultLocalX;
            y = eye.has("y") ? eye.get("y").getAsInt() : defaultLocalY;

            String size = eye.has("size") ? eye.get("size").getAsString() : "1x1";

            if ("1x2".equals(size)) {
                height = 2;
            } else if ("2x2".equals(size)) {
                width = 2;
                height = 2;
            }
        }

        int textureX = 8 + x;
        int textureY = 8 + y;

        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                target.set(textureX + dx, textureY + dy, true);
            }
        }
    }

    private static JsonArray writePixels(CybereyeVisualConfig.PixelMask mask) {
        JsonArray array = new JsonArray();

        for (int v = 0; v < CybereyeVisualConfig.HEAD_UV_HEIGHT; v++) {
            for (int u = 0; u < CybereyeVisualConfig.HEAD_UV_WIDTH; u++) {
                if (!mask.get(u, v)) continue;

                JsonArray pixel = new JsonArray();
                pixel.add(u);
                pixel.add(v);

                array.add(pixel);
            }
        }

        return array;
    }

    private static void readPixels(JsonArray array, CybereyeVisualConfig.PixelMask target) {
        if (array == null) return;

        for (JsonElement element : array) {
            if (!element.isJsonArray()) continue;

            JsonArray pixel = element.getAsJsonArray();
            if (pixel.size() < 2) continue;

            target.set(pixel.get(0).getAsInt(), pixel.get(1).getAsInt(), true);
        }
    }

    private static Path fileFor(UUID playerId) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(CreateCybernetics.MODID);
        return dir.resolve("cybereye_iris_" + playerId + ".json");
    }
}