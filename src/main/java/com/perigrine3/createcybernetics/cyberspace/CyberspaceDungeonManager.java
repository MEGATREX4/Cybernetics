package com.perigrine3.createcybernetics.cyberspace;

import com.perigrine3.createcybernetics.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

public final class CyberspaceDungeonManager {
    public static final int DUNGEON_CELL_SIZE = 4096;
    public static final int MAX_DUNGEON_SIZE = 96;
    public static final int MAX_CELL_RADIUS = 7000;

    private static final int TEMPORARY_PLATFORM_SIZE = 9;
    private static final int TEMPORARY_PLATFORM_Y = 80;

    private CyberspaceDungeonManager() {}

    public static CyberspaceDungeonSavedData.DungeonRecord getOrCreateDungeon(MinecraftServer server, UUID serverId) {
        CyberspaceDungeonSavedData data = CyberspaceDungeonSavedData.get(server);
        CyberspaceDungeonSavedData.DungeonRecord record = data.getOrCreateDungeon(serverId);

        ServerLevel cyberspace = server.getLevel(com.perigrine3.createcybernetics.world.dimension.ModDimensions.CYBERSPACE_LEVEL);

        if (cyberspace == null) {
            return record;
        }

        if (!record.generated()) {
            generateTemporaryDungeon(cyberspace, data, record);
            record = data.getDungeon(serverId);
        }

        return record;
    }

    public static BlockPos getDefaultEntryPosition(int cellX, int cellZ) {
        int centerX = getCellCenterCoordinate(cellX);
        int centerZ = getCellCenterCoordinate(cellZ);

        return new BlockPos(centerX, TEMPORARY_PLATFORM_Y, centerZ + 2);
    }

    public static BlockPos getCellCenter(int cellX, int cellZ) {
        return new BlockPos(getCellCenterCoordinate(cellX), TEMPORARY_PLATFORM_Y, getCellCenterCoordinate(cellZ));
    }

    private static int getCellCenterCoordinate(int cell) {
        return cell * DUNGEON_CELL_SIZE + DUNGEON_CELL_SIZE / 2;
    }

    private static void generateTemporaryDungeon(ServerLevel level, CyberspaceDungeonSavedData data, CyberspaceDungeonSavedData.DungeonRecord record) {
        BlockPos center = getCellCenter(record.cellX(), record.cellZ());

        int half = TEMPORARY_PLATFORM_SIZE / 2;

        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                BlockPos platformPos = center.offset(x, -1, z);
                level.setBlock(platformPos, ModBlocks.DIGITAL_ARCHITECTURE.get().defaultBlockState(), 3);
            }
        }

        level.setBlock(center, ModBlocks.ACCESS_NODE.get().defaultBlockState(), 3);

        BlockPos entryPos = center.offset(0, 0, 2);

        data.markGenerated(record.serverId(), entryPos);
    }
}