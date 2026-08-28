package com.perigrine3.createcybernetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.navigation.SharedNavigationClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MinimapWaypointClient {

    private MinimapWaypointClient() {}

    public static final int EXPLORATION_TILE_SIZE = 64;

    public enum WaypointType {
        BANNER,
        MAP_BANNER,
        EXPLORER
    }

    public record Waypoint(String id, String dimension, int x, int y, int z, String name, int color, WaypointType type, boolean autoClear) {}

    public record NavigationTileSnapshot(String dimension, int tileX, int tileZ, int[] colors, long[] explored) {}

    public record NavigationWaypointSnapshot(String id, String dimension, int x, int y, int z, String name, int color, String type) {}

    public record ImportedMap(String id, String dimension, int centerX, int centerZ, byte scale, byte[] colors) {
        public int blocksPerPixel() {
            return 1 << scale;
        }

        public int minX() {
            return centerX - 64 * blocksPerPixel();
        }

        public int minZ() {
            return centerZ - 64 * blocksPerPixel();
        }

        public int maxX() {
            return minX() + 128 * blocksPerPixel();
        }

        public int maxZ() {
            return minZ() + 128 * blocksPerPixel();
        }
    }

    public static final class ExploredTile {
        private final String dimension;
        private final int tileX;
        private final int tileZ;
        private final int[] colors;
        private final BitSet explored;
        private long revision;

        private ExploredTile(String dimension, int tileX, int tileZ) {
            this.dimension = dimension;
            this.tileX = tileX;
            this.tileZ = tileZ;
            this.colors = new int[EXPLORATION_TILE_SIZE * EXPLORATION_TILE_SIZE];
            this.explored = new BitSet(EXPLORATION_TILE_SIZE * EXPLORATION_TILE_SIZE);
            this.revision = 0L;
        }

        ExploredTile(String dimension, int tileX, int tileZ, int[] colors, BitSet explored) {
            this.dimension = dimension;
            this.tileX = tileX;
            this.tileZ = tileZ;
            this.colors = colors;
            this.explored = explored;
            this.revision = 0L;
        }

        public String dimension() {
            return dimension;
        }

        public int tileX() {
            return tileX;
        }

        public int tileZ() {
            return tileZ;
        }

        public int minWorldX() {
            return tileX * EXPLORATION_TILE_SIZE;
        }

        public int minWorldZ() {
            return tileZ * EXPLORATION_TILE_SIZE;
        }

        public long revision() {
            return revision;
        }

        public boolean isExplored(int localX, int localZ) {
            if (localX < 0 || localX >= EXPLORATION_TILE_SIZE || localZ < 0 || localZ >= EXPLORATION_TILE_SIZE) return false;
            return explored.get(localZ * EXPLORATION_TILE_SIZE + localX);
        }

        public int color(int localX, int localZ) {
            if (localX < 0 || localX >= EXPLORATION_TILE_SIZE || localZ < 0 || localZ >= EXPLORATION_TILE_SIZE) return 0;
            return colors[localZ * EXPLORATION_TILE_SIZE + localX];
        }

        private boolean setColor(int localX, int localZ, int color, boolean overwrite) {
            if (localX < 0 || localX >= EXPLORATION_TILE_SIZE || localZ < 0 || localZ >= EXPLORATION_TILE_SIZE) return false;

            int index = localZ * EXPLORATION_TILE_SIZE + localX;

            if (!overwrite && explored.get(index)) {
                return false;
            }

            boolean changed = !explored.get(index) || colors[index] != color;

            if (!changed) {
                return false;
            }

            colors[index] = color;
            explored.set(index);
            revision++;
            return true;
        }
    }

    private record NavigationStorageKey(UUID playerId, String worldId) {}

    private static final class NavigationData {
        private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();
        private final List<ImportedMap> maps = new ArrayList<>();
        private final Map<String, ExploredTile> exploredTiles = new HashMap<>();
        private final Set<String> dirtyTileKeys = new LinkedHashSet<>();
        private final Set<String> dirtyWaypointIds = new LinkedHashSet<>();
        private String activeWaypointId;
        private boolean dirty;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<NavigationStorageKey, NavigationData> CACHE = new ConcurrentHashMap<>();

    private static String cachedWorldId;

    public static ExploredTile createEmptySharedTile(String dimension, int tileX, int tileZ) {
        return new ExploredTile(dimension, tileX, tileZ, new int[EXPLORATION_TILE_SIZE * EXPLORATION_TILE_SIZE], new BitSet(EXPLORATION_TILE_SIZE * EXPLORATION_TILE_SIZE));
    }

    public static boolean setSharedTileColor(ExploredTile tile, int localX, int localZ, int color) {
        if (tile == null) return false;
        return tile.setColor(localX, localZ, color, false);
    }

    public static List<Waypoint> getWaypoints(LocalPlayer player) {
        if (player == null) return Collections.emptyList();

        Map<String, Waypoint> combined = new LinkedHashMap<>();

        for (Waypoint waypoint : data(player.getUUID()).waypoints.values()) {
            combined.put(waypoint.id(), waypoint);
        }

        for (Waypoint waypoint : SharedNavigationClientState.getWaypointValues()) {
            combined.putIfAbsent(waypoint.id(), waypoint);
        }

        return Collections.unmodifiableList(new ArrayList<>(combined.values()));
    }

    public static List<ImportedMap> getImportedMaps(LocalPlayer player) {
        if (player == null) return Collections.emptyList();
        return Collections.unmodifiableList(data(player.getUUID()).maps);
    }

    public static List<ExploredTile> getExploredTiles(LocalPlayer player) {
        if (player == null) return Collections.emptyList();

        List<ExploredTile> combined = new ArrayList<>(data(player.getUUID()).exploredTiles.values());
        combined.addAll(SharedNavigationClientState.getTileValues());
        return Collections.unmodifiableList(combined);
    }

    public static List<NavigationTileSnapshot> createTileSnapshot(LocalPlayer player) {
        if (player == null) return Collections.emptyList();

        List<NavigationTileSnapshot> snapshots = new ArrayList<>();

        for (ExploredTile tile : data(player.getUUID()).exploredTiles.values()) {
            snapshots.add(new NavigationTileSnapshot(tile.dimension, tile.tileX, tile.tileZ, tile.colors.clone(), tile.explored.toLongArray()));
        }

        return snapshots;
    }

    public static List<NavigationWaypointSnapshot> createWaypointSnapshot(LocalPlayer player) {
        if (player == null) return Collections.emptyList();

        List<NavigationWaypointSnapshot> snapshots = new ArrayList<>();

        for (Waypoint waypoint : data(player.getUUID()).waypoints.values()) {
            snapshots.add(new NavigationWaypointSnapshot(waypoint.id(), waypoint.dimension(), waypoint.x(), waypoint.y(), waypoint.z(), waypoint.name(), waypoint.color(), waypoint.type().name()));
        }

        return snapshots;
    }

    public static List<NavigationTileSnapshot> drainDirtyTileSnapshots(LocalPlayer player, int maxCount) {
        if (player == null || maxCount <= 0) return Collections.emptyList();

        NavigationData data = data(player.getUUID());

        if (data.dirtyTileKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<NavigationTileSnapshot> snapshots = new ArrayList<>(Math.min(maxCount, data.dirtyTileKeys.size()));
        Iterator<String> iterator = data.dirtyTileKeys.iterator();

        while (iterator.hasNext() && snapshots.size() < maxCount) {
            String key = iterator.next();
            iterator.remove();

            ExploredTile tile = data.exploredTiles.get(key);

            if (tile == null) continue;

            snapshots.add(new NavigationTileSnapshot(tile.dimension, tile.tileX, tile.tileZ, tile.colors.clone(), tile.explored.toLongArray()));
        }

        return snapshots;
    }

    public static List<NavigationWaypointSnapshot> drainDirtyWaypointSnapshots(LocalPlayer player, int maxCount) {
        if (player == null || maxCount <= 0) return Collections.emptyList();

        NavigationData data = data(player.getUUID());

        if (data.dirtyWaypointIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<NavigationWaypointSnapshot> snapshots = new ArrayList<>(Math.min(maxCount, data.dirtyWaypointIds.size()));
        Iterator<String> iterator = data.dirtyWaypointIds.iterator();

        while (iterator.hasNext() && snapshots.size() < maxCount) {
            String id = iterator.next();
            iterator.remove();

            Waypoint waypoint = data.waypoints.get(id);

            if (waypoint == null) continue;

            snapshots.add(new NavigationWaypointSnapshot(waypoint.id(), waypoint.dimension(), waypoint.x(), waypoint.y(), waypoint.z(), waypoint.name(), waypoint.color(), waypoint.type().name()));
        }

        return snapshots;
    }

    public static void markAllSharedSyncClean(LocalPlayer player) {
        if (player == null) return;

        NavigationData data = data(player.getUUID());
        data.dirtyTileKeys.clear();
        data.dirtyWaypointIds.clear();
    }

    public static Waypoint getActiveWaypoint(LocalPlayer player) {
        if (player == null) return null;

        NavigationStorageKey key = currentStorageKey(player.getUUID());
        NavigationData data = data(key);

        if (data.activeWaypointId == null) {
            return null;
        }

        Waypoint waypoint = data.waypoints.get(data.activeWaypointId);

        if (waypoint == null) {
            waypoint = SharedNavigationClientState.getWaypoint(data.activeWaypointId);
        }

        if (waypoint != null) {
            return waypoint;
        }

        data.activeWaypointId = null;
        data.dirty = true;
        saveIfDirty(key);
        return null;
    }

    public static void setActiveWaypoint(LocalPlayer player, Waypoint waypoint) {
        if (player == null) return;

        NavigationStorageKey key = currentStorageKey(player.getUUID());
        NavigationData data = data(key);
        data.activeWaypointId = waypoint != null ? waypoint.id() : null;
        data.dirty = true;
        saveIfDirty(key);
    }

    public static void clearActiveWaypoint(LocalPlayer player) {
        setActiveWaypoint(player, null);
    }

    public static Component getWaypointDisplayName(Waypoint waypoint) {
        if (waypoint == null) {
            return Component.translatable("gui.createcybernetics.navigation.default_waypoint");
        }

        if (waypoint.type() != WaypointType.EXPLORER) {
            return Component.literal(waypoint.name());
        }

        ResourceLocation typeId = ResourceLocation.tryParse(waypoint.name());

        if (typeId == null) {
            return Component.translatable("gui.createcybernetics.navigation.explorer.unknown");
        }

        if (typeId.getNamespace().equals("minecraft")) {
            return switch (typeId.getPath()) {
                case "red_x" -> Component.translatable("gui.createcybernetics.navigation.explorer.buried_treasure");
                case "mansion" -> Component.translatable("gui.createcybernetics.navigation.explorer.woodland_mansion");
                case "monument" -> Component.translatable("gui.createcybernetics.navigation.explorer.ocean_monument");
                case "trial_chambers" -> Component.translatable("gui.createcybernetics.navigation.explorer.trial_chambers");
                default -> Component.translatable("gui.createcybernetics.navigation.explorer.unknown");
            };
        }

        return Component.literal(formatExplorerName(typeId.getPath()));
    }

    public static boolean toggleBannerWaypoint(LocalPlayer player, BlockPos pos, String name, int color) {
        if (player == null || pos == null) return false;

        NavigationStorageKey key = currentStorageKey(player.getUUID());
        String dimension = player.level().dimension().location().toString();
        String id = bannerWaypointId(dimension, pos);
        NavigationData data = data(key);
        Waypoint existing = findWaypoint(data, id);

        if (existing != null) {
            removeWaypointInternal(data, existing);
            data.dirty = true;
            saveIfDirty(key);
            return false;
        }

        data.waypoints.put(id, new Waypoint(id, dimension, pos.getX(), pos.getY(), pos.getZ(), sanitizeName(name), color, WaypointType.BANNER, false));
        data.dirtyWaypointIds.add(id);
        data.dirty = true;
        saveIfDirty(key);
        return true;
    }

    public static boolean addMapBannerWaypoint(LocalPlayer player, String dimension, BlockPos pos, String name, int color) {
        if (player == null || pos == null) return false;

        NavigationData data = data(player.getUUID());
        String id = mapBannerWaypointId(dimension, pos);

        if (findWaypoint(data, id) != null) {
            return false;
        }

        data.waypoints.put(id, new Waypoint(id, dimension, pos.getX(), pos.getY(), pos.getZ(), sanitizeName(name), color, WaypointType.MAP_BANNER, false));
        data.dirtyWaypointIds.add(id);
        data.dirty = true;
        return true;
    }

    public static boolean addExplorerWaypoint(LocalPlayer player, String dimension, int x, int z, String sourceId, String typeId, int color) {
        if (player == null) return false;

        NavigationData data = data(player.getUUID());
        String id = explorerWaypointId(dimension, x, z, sourceId, typeId);

        if (findWaypoint(data, id) != null) {
            return false;
        }

        data.waypoints.put(id, new Waypoint(id, dimension, x, 0, z, typeId, color, WaypointType.EXPLORER, false));
        data.dirtyWaypointIds.add(id);
        data.dirty = true;
        return true;
    }

    public static boolean removeWaypoint(LocalPlayer player, Waypoint waypoint) {
        if (player == null || waypoint == null) return false;
        if (SharedNavigationClientState.isSharedWaypoint(waypoint.id())) return false;

        NavigationStorageKey key = currentStorageKey(player.getUUID());
        NavigationData data = data(key);

        if (!removeWaypointInternal(data, waypoint)) {
            return false;
        }

        data.dirty = true;
        saveIfDirty(key);
        return true;
    }

    public static boolean removeWaypointById(LocalPlayer player, String id) {
        if (player == null || id == null) return false;
        if (SharedNavigationClientState.isSharedWaypoint(id)) return false;

        NavigationStorageKey key = currentStorageKey(player.getUUID());
        NavigationData data = data(key);
        Waypoint waypoint = findWaypoint(data, id);

        if (waypoint == null) {
            return false;
        }

        removeWaypointInternal(data, waypoint);
        data.dirty = true;
        saveIfDirty(key);
        return true;
    }

    public static boolean importMap(LocalPlayer player, String mapId, String dimension, int centerX, int centerZ, byte scale, byte[] colors) {
        if (player == null || mapId == null || colors == null || colors.length != 128 * 128) return false;

        NavigationStorageKey key = currentStorageKey(player.getUUID());
        NavigationData data = data(key);
        String id = importedMapId(mapId, dimension);
        boolean added = true;

        for (int index = 0; index < data.maps.size(); index++) {
            ImportedMap existing = data.maps.get(index);

            if (existing.id().equals(id)) {
                data.maps.set(index, new ImportedMap(id, dimension, centerX, centerZ, scale, colors.clone()));
                added = false;
                break;
            }
        }

        if (added) {
            data.maps.add(new ImportedMap(id, dimension, centerX, centerZ, scale, colors.clone()));
        }

        stampImportedMap(data, dimension, centerX, centerZ, scale, colors);
        data.dirty = true;
        saveIfDirty(key);
        return added;
    }

    public static void updateExploration(LocalPlayer player, int radius) {
        if (player == null) return;

        NavigationData data = data(player.getUUID());
        Level level = player.level();
        String dimension = level.dimension().location().toString();
        int centerX = Mth.floor(player.getX());
        int centerZ = Mth.floor(player.getZ());
        int radiusSqr = radius * radius;
        boolean changed = false;

        for (int worldZ = centerZ - radius; worldZ <= centerZ + radius; worldZ++) {
            int dz = worldZ - centerZ;

            for (int worldX = centerX - radius; worldX <= centerX + radius; worldX++) {
                int dx = worldX - centerX;

                if (dx * dx + dz * dz > radiusSqr) continue;

                ExploredTile tile = getOrCreateTile(data, dimension, worldX, worldZ);
                int localX = Math.floorMod(worldX, EXPLORATION_TILE_SIZE);
                int localZ = Math.floorMod(worldZ, EXPLORATION_TILE_SIZE);

                if (tile.isExplored(localX, localZ)) continue;

                int color = sampleTerrainColor(level, worldX, worldZ);

                if (tile.setColor(localX, localZ, color, false)) {
                    data.dirtyTileKeys.add(tileKey(dimension, tile.tileX(), tile.tileZ()));
                    changed = true;
                }
            }
        }

        if (changed) {
            data.dirty = true;
        }
    }

    public static void refreshExploration(LocalPlayer player, int radius) {
        if (player == null) return;

        NavigationData data = data(player.getUUID());
        Level level = player.level();
        String dimension = level.dimension().location().toString();
        int centerX = Mth.floor(player.getX());
        int centerZ = Mth.floor(player.getZ());
        int radiusSqr = radius * radius;
        boolean changed = false;

        for (int worldZ = centerZ - radius; worldZ <= centerZ + radius; worldZ++) {
            int dz = worldZ - centerZ;

            for (int worldX = centerX - radius; worldX <= centerX + radius; worldX++) {
                int dx = worldX - centerX;

                if (dx * dx + dz * dz > radiusSqr) continue;

                ExploredTile tile = getOrCreateTile(data, dimension, worldX, worldZ);
                int localX = Math.floorMod(worldX, EXPLORATION_TILE_SIZE);
                int localZ = Math.floorMod(worldZ, EXPLORATION_TILE_SIZE);
                int color = sampleTerrainColor(level, worldX, worldZ);

                if (tile.setColor(localX, localZ, color, true)) {
                    data.dirtyTileKeys.add(tileKey(dimension, tile.tileX(), tile.tileZ()));
                    changed = true;
                }
            }
        }

        if (changed) {
            data.dirty = true;
        }
    }

    public static void saveIfDirty(UUID playerId) {
        if (playerId == null) return;
        saveIfDirty(currentStorageKey(playerId));
    }

    public static void reload(UUID playerId) {
        if (playerId == null) return;

        NavigationStorageKey key = currentStorageKey(playerId);
        NavigationData existing = CACHE.remove(key);

        if (existing != null && existing.dirty) {
            save(key, existing);
        }

        CACHE.put(key, load(key));
    }

    public static void invalidate(UUID playerId) {
        if (playerId == null) return;

        Iterator<Map.Entry<NavigationStorageKey, NavigationData>> iterator = CACHE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<NavigationStorageKey, NavigationData> entry = iterator.next();

            if (!entry.getKey().playerId().equals(playerId)) continue;

            if (entry.getValue().dirty) {
                save(entry.getKey(), entry.getValue());
            }

            iterator.remove();
        }
    }

    private static NavigationData data(UUID playerId) {
        return data(currentStorageKey(playerId));
    }

    private static NavigationData data(NavigationStorageKey key) {
        return CACHE.computeIfAbsent(key, MinimapWaypointClient::load);
    }

    private static void saveIfDirty(NavigationStorageKey key) {
        NavigationData data = CACHE.get(key);

        if (data == null || !data.dirty) return;

        save(key, data);
    }

    private static Waypoint findWaypoint(NavigationData data, String id) {
        return id == null ? null : data.waypoints.get(id);
    }

    private static boolean removeWaypointInternal(NavigationData data, Waypoint waypoint) {
        if (waypoint == null) return false;

        boolean removed = data.waypoints.remove(waypoint.id()) != null;
        data.dirtyWaypointIds.remove(waypoint.id());

        if (removed && waypoint.id().equals(data.activeWaypointId)) {
            data.activeWaypointId = null;
        }

        return removed;
    }

    private static ExploredTile getOrCreateTile(NavigationData data, String dimension, int worldX, int worldZ) {
        int tileX = Math.floorDiv(worldX, EXPLORATION_TILE_SIZE);
        int tileZ = Math.floorDiv(worldZ, EXPLORATION_TILE_SIZE);
        String key = tileKey(dimension, tileX, tileZ);
        return data.exploredTiles.computeIfAbsent(key, ignored -> new ExploredTile(dimension, tileX, tileZ));
    }

    private static void stampImportedMap(NavigationData data, String dimension, int centerX, int centerZ, byte scale, byte[] colors) {
        int blocksPerPixel = 1 << scale;
        int minWorldX = centerX - 64 * blocksPerPixel;
        int minWorldZ = centerZ - 64 * blocksPerPixel;

        for (int mapZ = 0; mapZ < 128; mapZ++) {
            for (int mapX = 0; mapX < 128; mapX++) {
                byte packedColor = colors[mapZ * 128 + mapX];

                if (packedColor == 0) continue;

                int color = 0xFF000000 | net.minecraft.world.level.material.MapColor.getColorFromPackedId(packedColor);
                int baseWorldX = minWorldX + mapX * blocksPerPixel;
                int baseWorldZ = minWorldZ + mapZ * blocksPerPixel;

                for (int offsetZ = 0; offsetZ < blocksPerPixel; offsetZ++) {
                    for (int offsetX = 0; offsetX < blocksPerPixel; offsetX++) {
                        int worldX = baseWorldX + offsetX;
                        int worldZ = baseWorldZ + offsetZ;
                        ExploredTile tile = getOrCreateTile(data, dimension, worldX, worldZ);
                        int localX = Math.floorMod(worldX, EXPLORATION_TILE_SIZE);
                        int localZ = Math.floorMod(worldZ, EXPLORATION_TILE_SIZE);
                        if (tile.setColor(localX, localZ, color, false)) {
                            data.dirtyTileKeys.add(tileKey(dimension, tile.tileX(), tile.tileZ()));
                        }
                    }
                }
            }
        }
    }

    private static int sampleTerrainColor(Level level, int worldX, int worldZ) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(worldX, surfaceY, worldZ);
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = state.getFluidState();

        while (surfaceY > level.getMinBuildHeight() && state.isAir()) {
            surfaceY--;
            pos.set(worldX, surfaceY, worldZ);
            state = level.getBlockState(pos);
            fluidState = state.getFluidState();
        }

        int rgb = state.getMapColor(level, pos).col;

        if (!fluidState.isEmpty()) {
            rgb = fluidState.createLegacyBlock().getMapColor(level, pos).col;
        }

        int brightness = terrainBrightness(level, worldX, surfaceY, worldZ);
        int red = Mth.clamp(((rgb >> 16) & 0xFF) * brightness / 255, 0, 255);
        int green = Mth.clamp(((rgb >> 8) & 0xFF) * brightness / 255, 0, 255);
        int blue = Mth.clamp((rgb & 0xFF) * brightness / 255, 0, 255);

        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int terrainBrightness(Level level, int worldX, int surfaceY, int worldZ) {
        int northHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ - 1) - 1;
        int westHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX - 1, worldZ) - 1;
        int difference = surfaceY - northHeight + surfaceY - westHeight;

        if (difference >= 4) return 310;
        if (difference >= 2) return 285;
        if (difference >= 1) return 270;
        if (difference <= -4) return 160;
        if (difference <= -2) return 185;
        if (difference <= -1) return 210;

        return 240;
    }

    private static NavigationStorageKey currentStorageKey(UUID playerId) {
        return new NavigationStorageKey(playerId, resolveWorldId());
    }

    public static void clearWorldIdentityCache() {
        cachedWorldId = null;
    }

    private static String resolveWorldId() {
        if (cachedWorldId != null) {
            return cachedWorldId;
        }

        Minecraft mc = Minecraft.getInstance();
        MinecraftServer singleplayerServer = mc.getSingleplayerServer();

        if (singleplayerServer != null) {
            try {
                Path worldPath = singleplayerServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                return cachedWorldId = hashWorldIdentity("singleplayer|" + worldPath);
            } catch (Throwable ignored) {
            }
        }

        ServerData serverData = mc.getCurrentServer();

        if (serverData != null && serverData.ip != null && !serverData.ip.isBlank()) {
            return cachedWorldId = hashWorldIdentity("multiplayer|" + serverData.ip.trim().toLowerCase());
        }

        if (mc.getConnection() != null) {
            try {
                SocketAddress remoteAddress = mc.getConnection().getConnection().getRemoteAddress();

                if (remoteAddress != null) {
                    return cachedWorldId = hashWorldIdentity("connection|" + remoteAddress);
                }
            } catch (Throwable ignored) {
            }
        }

        return cachedWorldId = hashWorldIdentity("unknown_world");
    }

    private static String hashWorldIdentity(String identity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(identity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (Throwable ignored) {
            return Integer.toUnsignedString(identity.hashCode(), 16);
        }
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) return Component.translatable("gui.createcybernetics.navigation.default_waypoint").getString();

        String sanitized = name.trim();

        if (sanitized.length() > 48) {
            sanitized = sanitized.substring(0, 48);
        }

        return sanitized;
    }

    private static String formatExplorerName(String path) {
        String[] words = path.replace('/', '_').split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) continue;

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(word.charAt(0)));

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.isEmpty() ? Component.translatable("gui.createcybernetics.navigation.explorer.unknown").getString() : result.toString();
    }

    private static String bannerWaypointId(String dimension, BlockPos pos) {
        return "banner|" + dimension + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static String mapBannerWaypointId(String dimension, BlockPos pos) {
        return "map_banner|" + dimension + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static String explorerWaypointId(String dimension, int x, int z, String sourceId, String typeId) {
        return "explorer|" + dimension + "|" + x + "|" + z + "|" + sourceId + "|" + typeId;
    }

    private static String importedMapId(String mapId, String dimension) {
        return "map|" + dimension + "|" + mapId;
    }

    private static String tileKey(String dimension, int tileX, int tileZ) {
        return dimension + "|" + tileX + "|" + tileZ;
    }

    private static Path fileFor(NavigationStorageKey key) {
        Path directory = FMLPaths.CONFIGDIR.get().resolve(CreateCybernetics.MODID).resolve("navigation").resolve(key.worldId());
        return directory.resolve("navigation_data_" + key.playerId() + ".json");
    }

    private static NavigationData load(NavigationStorageKey key) {
        Path file = fileFor(key);

        if (!Files.exists(file)) {
            return new NavigationData();
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            if (root == null) {
                return new NavigationData();
            }

            NavigationData data = new NavigationData();

            if (root.has("activeWaypointId") && !root.get("activeWaypointId").isJsonNull()) {
                data.activeWaypointId = root.get("activeWaypointId").getAsString();
            }

            if (root.has("waypoints") && root.get("waypoints").isJsonArray()) {
                JsonArray waypoints = root.getAsJsonArray("waypoints");

                for (JsonElement element : waypoints) {
                    if (!element.isJsonObject()) continue;

                    JsonObject object = element.getAsJsonObject();

                    try {
                        String id = object.get("id").getAsString();
                        String dimension = object.get("dimension").getAsString();
                        int x = object.get("x").getAsInt();
                        int y = object.get("y").getAsInt();
                        int z = object.get("z").getAsInt();
                        String name = object.get("name").getAsString();
                        int color = object.get("color").getAsInt();
                        String typeName = object.get("type").getAsString();
                        WaypointType type = typeName.equals("TREASURE") ? WaypointType.EXPLORER : WaypointType.valueOf(typeName);
                        boolean autoClear = object.get("autoClear").getAsBoolean();

                        if (typeName.equals("TREASURE") && ResourceLocation.tryParse(name) == null) {
                            name = "minecraft:red_x";
                        }

                        data.waypoints.put(id, new Waypoint(id, dimension, x, y, z, sanitizeName(name), color, type, autoClear));
                    } catch (Throwable ignored) {
                    }
                }
            }

            if (root.has("maps") && root.get("maps").isJsonArray()) {
                JsonArray maps = root.getAsJsonArray("maps");

                for (JsonElement element : maps) {
                    if (!element.isJsonObject()) continue;

                    JsonObject object = element.getAsJsonObject();

                    try {
                        String id = object.get("id").getAsString();
                        String dimension = object.get("dimension").getAsString();
                        int centerX = object.get("centerX").getAsInt();
                        int centerZ = object.get("centerZ").getAsInt();
                        byte scale = object.get("scale").getAsByte();
                        byte[] colors = Base64.getDecoder().decode(object.get("colors").getAsString());

                        if (colors.length == 128 * 128) {
                            data.maps.add(new ImportedMap(id, dimension, centerX, centerZ, scale, colors));
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            if (root.has("exploredTiles") && root.get("exploredTiles").isJsonArray()) {
                JsonArray exploredTiles = root.getAsJsonArray("exploredTiles");

                for (JsonElement element : exploredTiles) {
                    if (!element.isJsonObject()) continue;

                    JsonObject object = element.getAsJsonObject();

                    try {
                        String dimension = object.get("dimension").getAsString();
                        int tileX = object.get("tileX").getAsInt();
                        int tileZ = object.get("tileZ").getAsInt();
                        int[] colors = decodeColors(object.get("colors").getAsString());
                        BitSet explored = BitSet.valueOf(Base64.getDecoder().decode(object.get("explored").getAsString()));

                        if (colors.length == EXPLORATION_TILE_SIZE * EXPLORATION_TILE_SIZE) {
                            data.exploredTiles.put(tileKey(dimension, tileX, tileZ), new ExploredTile(dimension, tileX, tileZ, colors, explored));
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            data.dirty = false;
            return data;
        } catch (Throwable ignored) {
            return new NavigationData();
        }
    }

    private static void save(NavigationStorageKey key, NavigationData data) {
        Path file = fileFor(key);

        try {
            Files.createDirectories(file.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("worldId", key.worldId());
            root.addProperty("playerId", key.playerId().toString());

            if (data.activeWaypointId != null) {
                root.addProperty("activeWaypointId", data.activeWaypointId);
            }

            JsonArray waypoints = new JsonArray();

            for (Waypoint waypoint : data.waypoints.values()) {
                JsonObject object = new JsonObject();

                object.addProperty("id", waypoint.id());
                object.addProperty("dimension", waypoint.dimension());
                object.addProperty("x", waypoint.x());
                object.addProperty("y", waypoint.y());
                object.addProperty("z", waypoint.z());
                object.addProperty("name", waypoint.name());
                object.addProperty("color", waypoint.color());
                object.addProperty("type", waypoint.type().name());
                object.addProperty("autoClear", waypoint.autoClear());

                waypoints.add(object);
            }

            root.add("waypoints", waypoints);

            JsonArray maps = new JsonArray();

            for (ImportedMap importedMap : data.maps) {
                JsonObject object = new JsonObject();

                object.addProperty("id", importedMap.id());
                object.addProperty("dimension", importedMap.dimension());
                object.addProperty("centerX", importedMap.centerX());
                object.addProperty("centerZ", importedMap.centerZ());
                object.addProperty("scale", importedMap.scale());
                object.addProperty("colors", Base64.getEncoder().encodeToString(importedMap.colors()));

                maps.add(object);
            }

            root.add("maps", maps);

            JsonArray exploredTiles = new JsonArray();

            for (ExploredTile tile : data.exploredTiles.values()) {
                JsonObject object = new JsonObject();

                object.addProperty("dimension", tile.dimension());
                object.addProperty("tileX", tile.tileX());
                object.addProperty("tileZ", tile.tileZ());
                object.addProperty("colors", encodeColors(tile.colors));
                object.addProperty("explored", Base64.getEncoder().encodeToString(tile.explored.toByteArray()));

                exploredTiles.add(object);
            }

            root.add("exploredTiles", exploredTiles);

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }

            data.dirty = false;
        } catch (Throwable ignored) {
        }
    }

    private static String encodeColors(int[] colors) {
        ByteBuffer buffer = ByteBuffer.allocate(colors.length * Integer.BYTES);

        for (int color : colors) {
            buffer.putInt(color);
        }

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private static int[] decodeColors(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int[] colors = new int[bytes.length / Integer.BYTES];

        for (int index = 0; index < colors.length; index++) {
            colors[index] = buffer.getInt();
        }

        return colors;
    }
}