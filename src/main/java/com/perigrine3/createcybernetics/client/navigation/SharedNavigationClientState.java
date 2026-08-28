package com.perigrine3.createcybernetics.client.navigation;

import com.perigrine3.createcybernetics.client.MinimapWaypointClient;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SharedNavigationClientState {

    private SharedNavigationClientState() {}

    public record SharedPlayer(UUID playerId, String name, String dimension, double x, double y, double z, float yaw) {}

    private static UUID networkId;
    private static final List<SharedPlayer> PLAYERS = new ArrayList<>();
    private static final Map<String, MinimapWaypointClient.ExploredTile> TILES = new LinkedHashMap<>();
    private static final Map<String, MinimapWaypointClient.Waypoint> WAYPOINTS = new LinkedHashMap<>();

    private static final Collection<MinimapWaypointClient.ExploredTile> TILE_VALUES = Collections.unmodifiableCollection(TILES.values());
    private static final Collection<MinimapWaypointClient.Waypoint> WAYPOINT_VALUES = Collections.unmodifiableCollection(WAYPOINTS.values());

    public static UUID getNetworkId() {
        return networkId;
    }

    public static List<SharedPlayer> getPlayers() {
        return Collections.unmodifiableList(PLAYERS);
    }

    public static List<MinimapWaypointClient.ExploredTile> getTiles() {
        return Collections.unmodifiableList(new ArrayList<>(TILES.values()));
    }

    public static List<MinimapWaypointClient.Waypoint> getWaypoints() {
        return Collections.unmodifiableList(new ArrayList<>(WAYPOINTS.values()));
    }

    public static Collection<MinimapWaypointClient.ExploredTile> getTileValues() {
        return TILE_VALUES;
    }

    public static Collection<MinimapWaypointClient.Waypoint> getWaypointValues() {
        return WAYPOINT_VALUES;
    }

    public static MinimapWaypointClient.Waypoint getWaypoint(String waypointId) {
        return waypointId == null ? null : WAYPOINTS.get(waypointId);
    }

    public static boolean isSharedWaypoint(String waypointId) {
        return waypointId != null && WAYPOINTS.containsKey(waypointId);
    }

    public static void acceptPlayers(UUID newNetworkId, List<SharedPlayer> players) {
        switchNetwork(newNetworkId);
        PLAYERS.clear();

        if (players != null) {
            PLAYERS.addAll(players);
        }
    }

    public static boolean mergeTile(String dimension, int tileX, int tileZ, int[] colors, long[] exploredWords) {
        if (networkId == null) return false;
        if (dimension == null || dimension.isBlank()) return false;
        if (colors == null || colors.length != MinimapWaypointClient.EXPLORATION_TILE_SIZE * MinimapWaypointClient.EXPLORATION_TILE_SIZE) return false;

        BitSet explored = BitSet.valueOf(exploredWords);
        String key = tileKey(dimension, tileX, tileZ);
        MinimapWaypointClient.ExploredTile tile = TILES.computeIfAbsent(key, ignored -> MinimapWaypointClient.createEmptySharedTile(dimension, tileX, tileZ));
        boolean changed = false;

        for (int index = explored.nextSetBit(0); index >= 0 && index < MinimapWaypointClient.EXPLORATION_TILE_SIZE * MinimapWaypointClient.EXPLORATION_TILE_SIZE; index = explored.nextSetBit(index + 1)) {
            int localX = index % MinimapWaypointClient.EXPLORATION_TILE_SIZE;
            int localZ = index / MinimapWaypointClient.EXPLORATION_TILE_SIZE;

            if (MinimapWaypointClient.setSharedTileColor(tile, localX, localZ, colors[index])) {
                changed = true;
            }
        }

        return changed;
    }

    public static boolean mergeWaypoint(String id, String dimension, int x, int y, int z, String name, int color, String typeName) {
        if (networkId == null) return false;
        if (id == null || id.isBlank()) return false;
        if (WAYPOINTS.containsKey(id)) return false;

        MinimapWaypointClient.WaypointType type;

        try {
            type = MinimapWaypointClient.WaypointType.valueOf(typeName);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        WAYPOINTS.put(id, new MinimapWaypointClient.Waypoint(id, dimension, x, y, z, name == null ? "" : name, color, type, false));
        return true;
    }

    public static void clear() {
        networkId = null;
        PLAYERS.clear();
        TILES.clear();
        WAYPOINTS.clear();
    }

    private static void switchNetwork(UUID newNetworkId) {
        if (newNetworkId == null) {
            clear();
            return;
        }

        if (newNetworkId.equals(networkId)) return;

        networkId = newNetworkId;
        PLAYERS.clear();
        TILES.clear();
        WAYPOINTS.clear();
    }

    private static String tileKey(String dimension, int tileX, int tileZ) {
        return dimension + "|" + tileX + "|" + tileZ;
    }
}
