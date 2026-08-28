package com.perigrine3.createcybernetics.cyberspace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CyberspaceDungeonSavedData extends SavedData {
    private static final String DATA_NAME = "createcybernetics_cyberspace_dungeons";

    private static final String NBT_DUNGEONS = "Dungeons";
    private static final String NBT_SERVER_BINDINGS = "ServerBindings";

    private static final String NBT_SERVER_ID = "ServerId";
    private static final String NBT_CELL_X = "CellX";
    private static final String NBT_CELL_Z = "CellZ";
    private static final String NBT_ENTRY_X = "EntryX";
    private static final String NBT_ENTRY_Y = "EntryY";
    private static final String NBT_ENTRY_Z = "EntryZ";
    private static final String NBT_GENERATED = "Generated";

    private static final String NBT_DIMENSION = "Dimension";
    private static final String NBT_BLOCK_X = "BlockX";
    private static final String NBT_BLOCK_Y = "BlockY";
    private static final String NBT_BLOCK_Z = "BlockZ";

    private final Map<UUID, DungeonRecord> dungeons = new HashMap<>();
    private final Map<ServerBindingKey, UUID> serverBindings = new HashMap<>();

    public static CyberspaceDungeonSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(CyberspaceDungeonSavedData::new, CyberspaceDungeonSavedData::load), DATA_NAME);
    }

    public static CyberspaceDungeonSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CyberspaceDungeonSavedData data = new CyberspaceDungeonSavedData();

        if (tag.contains(NBT_DUNGEONS, Tag.TAG_LIST)) {
            ListTag dungeonList = tag.getList(NBT_DUNGEONS, Tag.TAG_COMPOUND);

            for (int i = 0; i < dungeonList.size(); i++) {
                CompoundTag dungeonTag = dungeonList.getCompound(i);

                if (!dungeonTag.hasUUID(NBT_SERVER_ID)) {
                    continue;
                }

                UUID serverId = dungeonTag.getUUID(NBT_SERVER_ID);
                int cellX = dungeonTag.getInt(NBT_CELL_X);
                int cellZ = dungeonTag.getInt(NBT_CELL_Z);
                int entryX = dungeonTag.getInt(NBT_ENTRY_X);
                int entryY = dungeonTag.getInt(NBT_ENTRY_Y);
                int entryZ = dungeonTag.getInt(NBT_ENTRY_Z);
                boolean generated = dungeonTag.getBoolean(NBT_GENERATED);

                data.dungeons.put(serverId, new DungeonRecord(serverId, cellX, cellZ, new BlockPos(entryX, entryY, entryZ), generated));
            }
        }

        if (tag.contains(NBT_SERVER_BINDINGS, Tag.TAG_LIST)) {
            ListTag bindingList = tag.getList(NBT_SERVER_BINDINGS, Tag.TAG_COMPOUND);

            for (int i = 0; i < bindingList.size(); i++) {
                CompoundTag bindingTag = bindingList.getCompound(i);

                if (!bindingTag.hasUUID(NBT_SERVER_ID)) {
                    continue;
                }

                ResourceLocation dimension = ResourceLocation.tryParse(bindingTag.getString(NBT_DIMENSION));

                if (dimension == null) {
                    continue;
                }

                BlockPos pos = new BlockPos(
                        bindingTag.getInt(NBT_BLOCK_X),
                        bindingTag.getInt(NBT_BLOCK_Y),
                        bindingTag.getInt(NBT_BLOCK_Z)
                );

                UUID serverId = bindingTag.getUUID(NBT_SERVER_ID);

                data.serverBindings.put(new ServerBindingKey(dimension, pos), serverId);
            }
        }

        return data;
    }

    public DungeonRecord getDungeon(UUID serverId) {
        return dungeons.get(serverId);
    }

    public DungeonRecord getOrCreateDungeon(UUID serverId) {
        DungeonRecord existing = dungeons.get(serverId);

        if (existing != null) {
            return existing;
        }

        CellPos cell = findNextAvailableCell();
        BlockPos entryPos = CyberspaceDungeonManager.getDefaultEntryPosition(cell.x(), cell.z());

        DungeonRecord record = new DungeonRecord(serverId, cell.x(), cell.z(), entryPos, false);
        dungeons.put(serverId, record);

        setDirty();

        return record;
    }

    public UUID getBoundServerId(ResourceLocation dimension, BlockPos pos) {
        return serverBindings.get(new ServerBindingKey(dimension, pos));
    }

    public UUID getOrCreateServerBinding(ResourceLocation dimension, BlockPos pos, UUID proposedServerId) {
        ServerBindingKey key = new ServerBindingKey(dimension, pos);
        UUID existing = serverBindings.get(key);

        if (existing != null) {
            return existing;
        }

        serverBindings.put(key, proposedServerId);
        setDirty();

        return proposedServerId;
    }

    public void markGenerated(UUID serverId, BlockPos entryPos) {
        DungeonRecord existing = dungeons.get(serverId);

        if (existing == null) {
            return;
        }

        DungeonRecord updated = new DungeonRecord(existing.serverId(), existing.cellX(), existing.cellZ(), entryPos.immutable(), true);
        dungeons.put(serverId, updated);

        setDirty();
    }

    public void setEntryPosition(UUID serverId, BlockPos entryPos) {
        DungeonRecord existing = dungeons.get(serverId);

        if (existing == null) {
            return;
        }

        DungeonRecord updated = new DungeonRecord(existing.serverId(), existing.cellX(), existing.cellZ(), entryPos.immutable(), existing.generated());
        dungeons.put(serverId, updated);

        setDirty();
    }

    public Collection<DungeonRecord> getDungeons() {
        return dungeons.values();
    }

    private CellPos findNextAvailableCell() {
        if (!isCellUsed(0, 0)) {
            return new CellPos(0, 0);
        }

        for (int radius = 1; radius <= CyberspaceDungeonManager.MAX_CELL_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                if (!isCellUsed(x, -radius)) {
                    return new CellPos(x, -radius);
                }

                if (!isCellUsed(x, radius)) {
                    return new CellPos(x, radius);
                }
            }

            for (int z = -radius + 1; z <= radius - 1; z++) {
                if (!isCellUsed(-radius, z)) {
                    return new CellPos(-radius, z);
                }

                if (!isCellUsed(radius, z)) {
                    return new CellPos(radius, z);
                }
            }
        }

        throw new IllegalStateException("No available cyberspace dungeon cells remain");
    }

    private boolean isCellUsed(int cellX, int cellZ) {
        for (DungeonRecord record : dungeons.values()) {
            if (record.cellX() == cellX && record.cellZ() == cellZ) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag dungeonList = new ListTag();

        for (DungeonRecord record : dungeons.values()) {
            CompoundTag dungeonTag = new CompoundTag();

            dungeonTag.putUUID(NBT_SERVER_ID, record.serverId());
            dungeonTag.putInt(NBT_CELL_X, record.cellX());
            dungeonTag.putInt(NBT_CELL_Z, record.cellZ());
            dungeonTag.putInt(NBT_ENTRY_X, record.entryPos().getX());
            dungeonTag.putInt(NBT_ENTRY_Y, record.entryPos().getY());
            dungeonTag.putInt(NBT_ENTRY_Z, record.entryPos().getZ());
            dungeonTag.putBoolean(NBT_GENERATED, record.generated());

            dungeonList.add(dungeonTag);
        }

        tag.put(NBT_DUNGEONS, dungeonList);

        ListTag bindingList = new ListTag();

        for (Map.Entry<ServerBindingKey, UUID> entry : serverBindings.entrySet()) {
            ServerBindingKey key = entry.getKey();

            CompoundTag bindingTag = new CompoundTag();

            bindingTag.putUUID(NBT_SERVER_ID, entry.getValue());
            bindingTag.putString(NBT_DIMENSION, key.dimension().toString());
            bindingTag.putInt(NBT_BLOCK_X, key.pos().getX());
            bindingTag.putInt(NBT_BLOCK_Y, key.pos().getY());
            bindingTag.putInt(NBT_BLOCK_Z, key.pos().getZ());

            bindingList.add(bindingTag);
        }

        tag.put(NBT_SERVER_BINDINGS, bindingList);

        return tag;
    }

    public record DungeonRecord(UUID serverId, int cellX, int cellZ, BlockPos entryPos, boolean generated) {}

    private record ServerBindingKey(ResourceLocation dimension, BlockPos pos) {
        private ServerBindingKey {
            pos = pos.immutable();
        }
    }

    private record CellPos(int x, int z) {}
}