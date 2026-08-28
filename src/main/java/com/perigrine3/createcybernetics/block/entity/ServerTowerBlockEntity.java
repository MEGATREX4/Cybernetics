package com.perigrine3.createcybernetics.block.entity;

import com.perigrine3.createcybernetics.common.energy.ConditionalBlockPower;
import com.perigrine3.createcybernetics.common.energy.SyncingEnergyStorage;
import com.perigrine3.createcybernetics.common.multiblock.ServerTowerMultiblock;
import com.perigrine3.createcybernetics.cyberspace.CyberspaceDungeonSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServerTowerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private static final String NBT_SERVER_ID = "ServerId";
    private static final String NBT_ENERGY = "Energy";

    public static final int ENERGY_CAPACITY = 5_000_000;
    public static final int MAX_RECEIVE = 1_000_000;
    public static final int ENERGY_USED_PER_TICK = 1_000_000;

    private UUID serverId = UUID.randomUUID();

    private final SyncingEnergyStorage energyStorage = new SyncingEnergyStorage(this, ENERGY_CAPACITY, MAX_RECEIVE, 0);

    private final IEnergyStorage externalEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return receiveExternalEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private long energyInputTick = Long.MIN_VALUE;
    private int energyReceivedThisTick;
    private boolean powered;
    private boolean serverBindingResolved;

    public ServerTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVER_TOWER.get(), pos, state);
    }

    public UUID getServerId() {
        ensureServerBinding();
        return serverId;
    }

    public IEnergyStorage getEnergyStorage() {
        return externalEnergyStorage;
    }

    public SyncingEnergyStorage getMutableEnergyStorage() {
        return energyStorage;
    }

    public boolean isOperational() {
        if (!ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            return true;
        }

        return powered;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level instanceof ServerLevel) {
            ensureServerBinding();
        }
    }

    private void ensureServerBinding() {
        if (serverBindingResolved) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        CyberspaceDungeonSavedData data = CyberspaceDungeonSavedData.get(serverLevel.getServer());

        UUID resolvedServerId = data.getOrCreateServerBinding(serverLevel.dimension().location(), worldPosition, serverId);

        if (!resolvedServerId.equals(serverId)) {
            serverId = resolvedServerId;
            setChanged();
        }

        serverBindingResolved = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ServerTowerBlockEntity blockEntity) {
        if (level.isClientSide) return;

        blockEntity.ensureServerBinding();

        if (!ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            blockEntity.powered = true;
            return;
        }

        blockEntity.beginEnergyInputTick();
        blockEntity.pullEnergyFromMultiblock();

        blockEntity.powered = blockEntity.energyStorage.consumeEnergyInternally(ENERGY_USED_PER_TICK);
    }

    private void beginEnergyInputTick() {
        if (level == null) return;

        long gameTime = level.getGameTime();

        if (energyInputTick == gameTime) return;

        energyInputTick = gameTime;
        energyReceivedThisTick = 0;
    }

    private int receiveExternalEnergy(int requested, boolean simulate) {
        if (level == null || level.isClientSide) return 0;
        if (!ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) return 0;
        if (requested <= 0) return 0;

        beginEnergyInputTick();

        int remainingInput = MAX_RECEIVE - energyReceivedThisTick;

        if (remainingInput <= 0) return 0;

        int accepted = energyStorage.receiveEnergy(Math.min(requested, remainingInput), simulate);

        if (!simulate && accepted > 0) {
            energyReceivedThisTick += accepted;
        }

        return accepted;
    }

    private void pullEnergyFromMultiblock() {
        if (level == null || level.isClientSide) return;

        int remainingInput = MAX_RECEIVE - energyReceivedThisTick;

        if (remainingInput <= 0) return;

        Set<BlockPos> towerPositions = new HashSet<>(ServerTowerMultiblock.getOccupiedPositions(worldPosition, getBlockState().getValue(com.perigrine3.createcybernetics.block.ServerTowerBlock.FACING)));
        Set<BlockPos> checkedNeighbors = new HashSet<>();

        for (BlockPos towerPos : towerPositions) {
            for (Direction direction : Direction.values()) {
                if (remainingInput <= 0) return;

                BlockPos neighborPos = towerPos.relative(direction);

                if (towerPositions.contains(neighborPos)) continue;
                if (!checkedNeighbors.add(neighborPos)) continue;

                IEnergyStorage sidedNeighborEnergy = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());

                remainingInput -= pullEnergyFromHandler(sidedNeighborEnergy, remainingInput);

                if (remainingInput <= 0) return;

                IEnergyStorage unsidedNeighborEnergy = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, null);

                if (unsidedNeighborEnergy == sidedNeighborEnergy) continue;

                remainingInput -= pullEnergyFromHandler(unsidedNeighborEnergy, remainingInput);
            }
        }
    }

    private int pullEnergyFromHandler(IEnergyStorage neighborEnergy, int maxAmount) {
        if (neighborEnergy == null) return 0;
        if (maxAmount <= 0) return 0;
        if (!neighborEnergy.canExtract()) return 0;

        int simulatedExtract = neighborEnergy.extractEnergy(maxAmount, true);

        if (simulatedExtract <= 0) return 0;

        int simulatedReceive = receiveExternalEnergy(simulatedExtract, true);

        if (simulatedReceive <= 0) return 0;

        int extracted = neighborEnergy.extractEnergy(simulatedReceive, false);

        if (extracted <= 0) return 0;

        int received = receiveExternalEnergy(extracted, false);

        if (received < extracted && neighborEnergy.canReceive()) {
            neighborEnergy.receiveEnergy(extracted - received, false);
        }

        return received;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        tag.putUUID(NBT_SERVER_ID, serverId);
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        if (tag.hasUUID(NBT_SERVER_ID)) {
            serverId = tag.getUUID(NBT_SERVER_ID);
        } else {
            serverId = UUID.randomUUID();
        }

        serverBindingResolved = false;

        if (tag.contains(NBT_ENERGY)) {
            energyStorage.setEnergyStoredSilently(tag.getInt(NBT_ENERGY));
        }
    }
}