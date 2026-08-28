package com.perigrine3.createcybernetics.common.navigation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SharedNavigationMapData extends SavedData {

    private static final String DATA_NAME = "createcybernetics_shared_navigation";
    public static final int TILE_SIZE = 64;
    public static final int TILE_PIXELS = TILE_SIZE * TILE_SIZE;

    public record SharedWaypoint(String id, String dimension, int x, int y, int z, String name, int color, String type) {}

    public static final class SharedTile {
        private final String dimension;
        private final int tileX;
        private final int tileZ;
        private final int[] colors;
        private final BitSet explored;

        private SharedTile(String dimension, int tileX, int tileZ) {
            this(dimension, tileX, tileZ, new int[TILE_PIXELS], new BitSet(TILE_PIXELS));
        }

        private SharedTile(String dimension, int tileX, int tileZ, int[] colors, BitSet explored) {
            this.dimension = dimension;
            this.tileX = tileX;
            this.tileZ = tileZ;
            this.colors = colors;
            this.explored = explored;
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

        public int[] colors() {
            return colors;
        }

        public BitSet explored() {
            return explored;
        }
    }

    private static final class NetworkData {
        private final Map<String, SharedTile> tiles = new HashMap<>();
        private final Map<String, SharedWaypoint> waypoints = new HashMap<>();
        private long revision;
    }

    private final Map<UUID, NetworkData> networks = new HashMap<>();
    private final Map<UUID, UUID> aliases = new HashMap<>();

    public static SharedNavigationMapData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(SharedNavigationMapData::new, SharedNavigationMapData::load, null), DATA_NAME);
    }

    public UUID resolveNetworkId(UUID networkId) {
        if (networkId == null) return null;

        UUID current = networkId;

        for (int depth = 0; depth < 64; depth++) {
            UUID next = aliases.get(current);

            if (next == null || next.equals(current)) {
                return current;
            }

            current = next;
        }

        return current;
    }

    public boolean mergeNetworks(UUID targetNetworkId, UUID sourceNetworkId) {
        UUID targetId = resolveNetworkId(targetNetworkId);
        UUID sourceId = resolveNetworkId(sourceNetworkId);

        if (targetId == null || sourceId == null) return false;
        if (targetId.equals(sourceId)) return false;

        NetworkData target = networks.computeIfAbsent(targetId, ignored -> new NetworkData());
        NetworkData source = networks.get(sourceId);
        boolean changed = false;

        if (source != null) {
            for (SharedTile sourceTile : source.tiles.values()) {
                String key = tileKey(sourceTile.dimension(), sourceTile.tileX(), sourceTile.tileZ());
                SharedTile targetTile = target.tiles.computeIfAbsent(key, ignored -> new SharedTile(sourceTile.dimension(), sourceTile.tileX(), sourceTile.tileZ()));

                for (int index = sourceTile.explored().nextSetBit(0); index >= 0 && index < TILE_PIXELS; index = sourceTile.explored().nextSetBit(index + 1)) {
                    if (targetTile.explored().get(index)) continue;

                    targetTile.colors()[index] = sourceTile.colors()[index];
                    targetTile.explored().set(index);
                    changed = true;
                }
            }

            for (SharedWaypoint waypoint : source.waypoints.values()) {
                if (target.waypoints.putIfAbsent(waypoint.id(), waypoint) == null) {
                    changed = true;
                }
            }

            networks.remove(sourceId);
        }

        aliases.put(sourceId, targetId);

        for (Map.Entry<UUID, UUID> alias : aliases.entrySet()) {
            if (alias.getValue().equals(sourceId)) {
                alias.setValue(targetId);
            }
        }

        target.revision++;
        setDirty();
        return changed;
    }

    public long getRevision(UUID networkId) {
        NetworkData network = networks.get(resolveNetworkId(networkId));
        return network != null ? network.revision : 0L;
    }

    public List<SharedWaypoint> getWaypoints(UUID networkId) {
        NetworkData network = networks.get(resolveNetworkId(networkId));

        if (network == null) {
            return List.of();
        }

        return new ArrayList<>(network.waypoints.values());
    }

    public List<SharedTile> getTiles(UUID networkId) {
        NetworkData network = networks.get(resolveNetworkId(networkId));

        if (network == null) {
            return List.of();
        }

        return new ArrayList<>(network.tiles.values());
    }

    public SharedTile getTile(UUID networkId, String dimension, int tileX, int tileZ) {
        NetworkData network = networks.get(resolveNetworkId(networkId));

        if (network == null || dimension == null) {
            return null;
        }

        return network.tiles.get(tileKey(dimension, tileX, tileZ));
    }

    public SharedWaypoint getWaypoint(UUID networkId, String waypointId) {
        NetworkData network = networks.get(resolveNetworkId(networkId));

        if (network == null || waypointId == null) {
            return null;
        }

        return network.waypoints.get(waypointId);
    }

    public boolean mergeTile(UUID networkId, String dimension, int tileX, int tileZ, int[] colors, BitSet explored) {
        if (networkId == null || dimension == null || dimension.isBlank()) return false;
        if (colors == null || colors.length != TILE_PIXELS) return false;
        if (explored == null || explored.isEmpty()) return false;

        networkId = resolveNetworkId(networkId);
        NetworkData network = networks.computeIfAbsent(networkId, ignored -> new NetworkData());
        String key = tileKey(dimension, tileX, tileZ);
        SharedTile target = network.tiles.computeIfAbsent(key, ignored -> new SharedTile(dimension, tileX, tileZ));
        boolean changed = false;

        for (int index = explored.nextSetBit(0); index >= 0 && index < TILE_PIXELS; index = explored.nextSetBit(index + 1)) {
            if (target.explored.get(index)) continue;

            target.colors[index] = colors[index];
            target.explored.set(index);
            changed = true;
        }

        if (changed) {
            network.revision++;
            setDirty();
        }

        return changed;
    }

    public boolean mergeWaypoint(UUID networkId, SharedWaypoint waypoint) {
        if (networkId == null || waypoint == null || waypoint.id() == null || waypoint.id().isBlank()) return false;

        networkId = resolveNetworkId(networkId);
        NetworkData network = networks.computeIfAbsent(networkId, ignored -> new NetworkData());

        if (network.waypoints.containsKey(waypoint.id())) {
            return false;
        }

        network.waypoints.put(waypoint.id(), waypoint);
        network.revision++;
        setDirty();
        return true;
    }

    public boolean mergeMap(UUID networkId, String sourceMapId, MapItemSavedData mapData) {
        if (networkId == null || sourceMapId == null || sourceMapId.isBlank() || mapData == null) return false;

        NetworkData network = networks.computeIfAbsent(networkId, ignored -> new NetworkData());
        String dimension = mapData.dimension.location().toString();
        int blocksPerPixel = 1 << mapData.scale;
        int minWorldX = mapData.centerX - 64 * blocksPerPixel;
        int minWorldZ = mapData.centerZ - 64 * blocksPerPixel;
        boolean changed = false;

        for (int mapZ = 0; mapZ < 128; mapZ++) {
            for (int mapX = 0; mapX < 128; mapX++) {
                byte packedColor = mapData.colors[mapZ * 128 + mapX];

                if (packedColor == 0) continue;

                int color = 0xFF000000 | MapColor.getColorFromPackedId(packedColor);
                int baseWorldX = minWorldX + mapX * blocksPerPixel;
                int baseWorldZ = minWorldZ + mapZ * blocksPerPixel;

                for (int offsetZ = 0; offsetZ < blocksPerPixel; offsetZ++) {
                    for (int offsetX = 0; offsetX < blocksPerPixel; offsetX++) {
                        int worldX = baseWorldX + offsetX;
                        int worldZ = baseWorldZ + offsetZ;
                        int tileX = Math.floorDiv(worldX, TILE_SIZE);
                        int tileZ = Math.floorDiv(worldZ, TILE_SIZE);
                        int localX = Math.floorMod(worldX, TILE_SIZE);
                        int localZ = Math.floorMod(worldZ, TILE_SIZE);
                        int index = localZ * TILE_SIZE + localX;
                        String tileKey = tileKey(dimension, tileX, tileZ);
                        SharedTile tile = network.tiles.computeIfAbsent(tileKey, ignored -> new SharedTile(dimension, tileX, tileZ));

                        if (tile.explored.get(index)) continue;

                        tile.colors[index] = color;
                        tile.explored.set(index);
                        changed = true;
                    }
                }
            }
        }

        for (MapBanner banner : mapData.getBanners()) {
            String id = mapBannerWaypointId(dimension, banner.pos().getX(), banner.pos().getY(), banner.pos().getZ());

            if (network.waypoints.containsKey(id)) continue;

            String name = banner.name().map(Component::getString).orElseGet(() -> Component.translatable("gui.createcybernetics.navigation.default_banner", Component.translatable("color.minecraft." + banner.color().getName())).getString());

            network.waypoints.put(id, new SharedWaypoint(id, dimension, banner.pos().getX(), banner.pos().getY(), banner.pos().getZ(), name, getWaypointColor(banner.color()), "MAP_BANNER"));
            changed = true;
        }

        for (MapDecoration decoration : mapData.getDecorations()) {
            Holder<MapDecorationType> type = decoration.type();
            boolean buriedTreasure = type.equals(MapDecorationTypes.RED_X);

            if (!buriedTreasure && !type.value().explorationMapElement()) continue;

            double worldX = mapData.centerX + decoration.x() * 0.5D * blocksPerPixel;
            double worldZ = mapData.centerZ + decoration.y() * 0.5D * blocksPerPixel;
            int x = (int) Math.floor(worldX);
            int z = (int) Math.floor(worldZ);
            String typeId = type.value().assetId().toString();
            String id = explorerWaypointId(dimension, x, z, sourceMapId, typeId);

            if (network.waypoints.containsKey(id)) continue;

            int mapColor = type.value().mapColor();
            int waypointColor = buriedTreasure ? 0xFFFF3B55 : mapColor < 0 ? 0xFFFF3B55 : 0xFF000000 | mapColor;

            network.waypoints.put(id, new SharedWaypoint(id, dimension, x, 0, z, typeId, waypointColor, "EXPLORER"));
            changed = true;
        }

        if (changed) {
            network.revision++;
            setDirty();
        }

        return changed;
    }

    private static String tileKey(String dimension, int tileX, int tileZ) {
        return dimension + "|" + tileX + "|" + tileZ;
    }

    private static String mapBannerWaypointId(String dimension, int x, int y, int z) {
        return "map_banner|" + dimension + "|" + x + "|" + y + "|" + z;
    }

    private static String explorerWaypointId(String dimension, int x, int z, String sourceId, String typeId) {
        return "explorer|" + dimension + "|" + x + "|" + z + "|" + sourceId + "|" + typeId;
    }

    private static int getWaypointColor(DyeColor color) {
        return switch (color) {
            case WHITE -> 0xFFFFFFFF;
            case ORANGE -> 0xFFFFA500;
            case MAGENTA -> 0xFFFF55FF;
            case LIGHT_BLUE -> 0xFF55AAFF;
            case YELLOW -> 0xFFFFFF55;
            case LIME -> 0xFF55FF55;
            case PINK -> 0xFFFF88AA;
            case GRAY -> 0xFF777777;
            case LIGHT_GRAY -> 0xFFAAAAAA;
            case CYAN -> 0xFF00FFFF;
            case PURPLE -> 0xFFAA55FF;
            case BLUE -> 0xFF5555FF;
            case BROWN -> 0xFFAA6633;
            case GREEN -> 0xFF00AA55;
            case RED -> 0xFFFF5555;
            case BLACK -> 0xFF333333;
        };
    }

    private static SharedNavigationMapData load(CompoundTag root, HolderLookup.Provider registries) {
        SharedNavigationMapData savedData = new SharedNavigationMapData();
        ListTag networksTag = root.getList("Networks", Tag.TAG_COMPOUND);

        for (Tag networkElement : networksTag) {
            if (!(networkElement instanceof CompoundTag networkTag)) continue;
            if (!networkTag.hasUUID("NetworkId")) continue;

            UUID networkId = networkTag.getUUID("NetworkId");
            NetworkData network = new NetworkData();
            network.revision = networkTag.getLong("Revision");

            ListTag tilesTag = networkTag.getList("Tiles", Tag.TAG_COMPOUND);

            for (Tag tileElement : tilesTag) {
                if (!(tileElement instanceof CompoundTag tileTag)) continue;

                String dimension = tileTag.getString("Dimension");
                int tileX = tileTag.getInt("TileX");
                int tileZ = tileTag.getInt("TileZ");
                int[] colors = tileTag.getIntArray("Colors");
                BitSet explored = BitSet.valueOf(tileTag.getLongArray("Explored"));

                if (dimension.isBlank() || colors.length != TILE_PIXELS) continue;

                network.tiles.put(tileKey(dimension, tileX, tileZ), new SharedTile(dimension, tileX, tileZ, colors, explored));
            }

            ListTag waypointsTag = networkTag.getList("Waypoints", Tag.TAG_COMPOUND);

            for (Tag waypointElement : waypointsTag) {
                if (!(waypointElement instanceof CompoundTag waypointTag)) continue;

                String id = waypointTag.getString("Id");

                if (id.isBlank()) continue;

                SharedWaypoint waypoint = new SharedWaypoint(
                        id,
                        waypointTag.getString("Dimension"),
                        waypointTag.getInt("X"),
                        waypointTag.getInt("Y"),
                        waypointTag.getInt("Z"),
                        waypointTag.getString("Name"),
                        waypointTag.getInt("Color"),
                        waypointTag.getString("Type")
                );

                network.waypoints.put(id, waypoint);
            }

            savedData.networks.put(networkId, network);
        }

        ListTag aliasesTag = root.getList("Aliases", Tag.TAG_COMPOUND);

        for (Tag aliasElement : aliasesTag) {
            if (!(aliasElement instanceof CompoundTag aliasTag)) continue;
            if (!aliasTag.hasUUID("Source")) continue;
            if (!aliasTag.hasUUID("Target")) continue;

            savedData.aliases.put(aliasTag.getUUID("Source"), aliasTag.getUUID("Target"));
        }

        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag networksTag = new ListTag();

        for (Map.Entry<UUID, NetworkData> networkEntry : networks.entrySet()) {
            CompoundTag networkTag = new CompoundTag();
            networkTag.putUUID("NetworkId", networkEntry.getKey());
            networkTag.putLong("Revision", networkEntry.getValue().revision);

            ListTag tilesTag = new ListTag();

            for (SharedTile tile : networkEntry.getValue().tiles.values()) {
                CompoundTag tileTag = new CompoundTag();
                tileTag.putString("Dimension", tile.dimension());
                tileTag.putInt("TileX", tile.tileX());
                tileTag.putInt("TileZ", tile.tileZ());
                tileTag.putIntArray("Colors", tile.colors());
                tileTag.putLongArray("Explored", tile.explored().toLongArray());
                tilesTag.add(tileTag);
            }

            networkTag.put("Tiles", tilesTag);

            ListTag waypointsTag = new ListTag();

            for (SharedWaypoint waypoint : networkEntry.getValue().waypoints.values()) {
                CompoundTag waypointTag = new CompoundTag();
                waypointTag.putString("Id", waypoint.id());
                waypointTag.putString("Dimension", waypoint.dimension());
                waypointTag.putInt("X", waypoint.x());
                waypointTag.putInt("Y", waypoint.y());
                waypointTag.putInt("Z", waypoint.z());
                waypointTag.putString("Name", waypoint.name());
                waypointTag.putInt("Color", waypoint.color());
                waypointTag.putString("Type", waypoint.type());
                waypointsTag.add(waypointTag);
            }

            networkTag.put("Waypoints", waypointsTag);
            networksTag.add(networkTag);
        }

        root.put("Networks", networksTag);

        ListTag aliasesTag = new ListTag();

        for (Map.Entry<UUID, UUID> alias : aliases.entrySet()) {
            CompoundTag aliasTag = new CompoundTag();
            aliasTag.putUUID("Source", alias.getKey());
            aliasTag.putUUID("Target", alias.getValue());
            aliasesTag.add(aliasTag);
        }

        root.put("Aliases", aliasesTag);

        return root;
    }
}