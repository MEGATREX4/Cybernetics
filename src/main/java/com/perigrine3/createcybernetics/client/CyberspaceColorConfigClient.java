package com.perigrine3.createcybernetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.cyberspace.CyberspaceColorData;
import com.perigrine3.createcybernetics.network.payload.CyberspaceColorSyncC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CyberspaceColorConfigClient {

    public static final int DEFAULT_COLOR = CyberspaceColorData.DEFAULT_COLOR;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CONFIG_VERSION = 1;

    private static final Map<UUID, Integer> LOCAL_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SYNCED_COLORS = new ConcurrentHashMap<>();

    private CyberspaceColorConfigClient() {}

    public static int getColor(UUID playerId) {
        if (playerId == null) return DEFAULT_COLOR;

        LocalPlayer localPlayer = Minecraft.getInstance().player;

        if (localPlayer != null && playerId.equals(localPlayer.getUUID())) {
            return getLocalColor(playerId);
        }

        return SYNCED_COLORS.getOrDefault(playerId, DEFAULT_COLOR);
    }

    public static int getLocalColor(UUID playerId) {
        if (playerId == null) return DEFAULT_COLOR;

        return LOCAL_CACHE.computeIfAbsent(playerId, CyberspaceColorConfigClient::loadOrDefault);
    }

    public static void applySyncedColor(UUID playerId, int color) {
        if (playerId == null) return;

        SYNCED_COLORS.put(playerId, CyberspaceColorData.sanitize(color));
    }

    public static void reload(UUID playerId) {
        if (playerId == null) return;

        LOCAL_CACHE.put(playerId, loadOrDefault(playerId));
    }

    public static void invalidate(UUID playerId) {
        if (playerId == null) return;

        LOCAL_CACHE.remove(playerId);
        SYNCED_COLORS.remove(playerId);
    }

    public static void clearSyncedColors() {
        SYNCED_COLORS.clear();
    }

    public static void reset(UUID playerId) {
        if (playerId == null) return;

        try {
            Files.deleteIfExists(fileFor(playerId));
        } catch (Throwable ignored) {
        }

        LOCAL_CACHE.put(playerId, DEFAULT_COLOR);
    }

    public static void save(UUID playerId, int color) {
        if (playerId == null) return;

        int safeColor = CyberspaceColorData.sanitize(color);
        LOCAL_CACHE.put(playerId, safeColor);

        Path file = fileFor(playerId);

        try {
            Files.createDirectories(file.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("version", CONFIG_VERSION);
            root.addProperty("color", String.format("#%06X", safeColor & 0x00FFFFFF));

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (Throwable throwable) {
            CreateCybernetics.LOGGER.error("Failed to save Cyberspace color configuration for {}", playerId, throwable);
        }
    }

    public static void saveAndSync(LocalPlayer player, int color) {
        if (player == null) return;

        int safeColor = CyberspaceColorData.sanitize(color);

        save(player.getUUID(), safeColor);
        SYNCED_COLORS.put(player.getUUID(), safeColor);

        PacketDistributor.sendToServer(new CyberspaceColorSyncC2SPayload(safeColor));
    }

    public static void syncCurrentColor(LocalPlayer player) {
        if (player == null) return;

        int color = getLocalColor(player.getUUID());

        SYNCED_COLORS.put(player.getUUID(), color);
        PacketDistributor.sendToServer(new CyberspaceColorSyncC2SPayload(color));
    }

    private static int loadOrDefault(UUID playerId) {
        Path file = fileFor(playerId);

        if (!Files.exists(file)) {
            return DEFAULT_COLOR;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("color")) return DEFAULT_COLOR;

            String value = root.get("color").getAsString();

            if (value.startsWith("#")) {
                value = value.substring(1);
            }

            return CyberspaceColorData.sanitize(Integer.parseInt(value, 16));
        } catch (Throwable throwable) {
            CreateCybernetics.LOGGER.error("Failed to load Cyberspace color configuration for {}", playerId, throwable);
            return DEFAULT_COLOR;
        }
    }

    private static Path fileFor(UUID playerId) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(CreateCybernetics.MODID);
        return dir.resolve("cyberspace_color_" + playerId + ".json");
    }
}