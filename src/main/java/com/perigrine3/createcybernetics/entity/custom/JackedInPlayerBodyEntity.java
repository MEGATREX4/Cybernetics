package com.perigrine3.createcybernetics.entity.custom;

import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.cyberspace.CyberspaceTravelManager;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class JackedInPlayerBodyEntity extends PathfinderMob {
    private static final String NBT_OWNER = "Owner";
    private static final String NBT_CYBERWARE_SNAPSHOT = "CyberwareSnapshot";
    private static final String NBT_MEATSPACE_INVENTORY = "MeatspaceInventory";
    private static final String NBT_INVENTORY_ITEMS = "Items";
    private static final String NBT_INVENTORY_SLOT = "Slot";
    private static final String NBT_INVENTORY_STACK = "Stack";
    private static final String NBT_SELECTED_SLOT = "SelectedSlot";
    private static final String NBT_TETHER_POS = "TetherPos";
    private static final String NBT_TETHER_FACING = "TetherFacing";

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(JackedInPlayerBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<CompoundTag> CYBERWARE_SNAPSHOT = SynchedEntityData.defineId(JackedInPlayerBodyEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Optional<BlockPos>> TETHER_POS = SynchedEntityData.defineId(JackedInPlayerBodyEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Direction> TETHER_FACING = SynchedEntityData.defineId(JackedInPlayerBodyEntity.class, EntityDataSerializers.DIRECTION);

    private CompoundTag meatspaceInventory = new CompoundTag();
    private boolean inventoryReleased;

    public JackedInPlayerBodyEntity(EntityType<? extends JackedInPlayerBodyEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(CYBERWARE_SNAPSHOT, new CompoundTag());
        builder.define(TETHER_POS, Optional.empty());
        builder.define(TETHER_FACING, Direction.NORTH);
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(ownerUUID));
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setCyberwareSnapshot(CompoundTag snapshot) {
        this.entityData.set(CYBERWARE_SNAPSHOT, snapshot == null ? new CompoundTag() : snapshot.copy());
    }

    public CompoundTag getCyberwareSnapshot() {
        return this.entityData.get(CYBERWARE_SNAPSHOT).copy();
    }

    public void setTetherAnchor(BlockPos pos, Direction facing) {
        this.entityData.set(TETHER_POS, Optional.ofNullable(pos == null ? null : pos.immutable()));
        this.entityData.set(TETHER_FACING, facing == null ? Direction.NORTH : facing);
    }

    public BlockPos getTetherPos() {
        return this.entityData.get(TETHER_POS).orElse(null);
    }

    public Direction getTetherFacing() {
        return this.entityData.get(TETHER_FACING);
    }

    public void clearTetherAnchor() {
        this.entityData.set(TETHER_POS, Optional.empty());
    }

    public ServerPlayer getOwner() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return null;

        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID == null) return null;

        return serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
    }

    public void initializeFromPlayer(ServerPlayer player) {
        setOwnerUUID(player.getUUID());
        setCustomName(player.getName());
        setCustomNameVisible(true);

        CompoundTag cyberwareSnapshot = PlayerCyberwareData.createSnapshotTagFor(player, player.registryAccess());
        setCyberwareSnapshot(cyberwareSnapshot);

        storeMeatspaceInventory(player);

        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getMaxHealth());
        }

        setHealth(player.getHealth());

        setPos(player.getX(), player.getY(), player.getZ());
        setYRot(player.getYRot());
        setXRot(player.getXRot());
        setYHeadRot(player.getYHeadRot());
        setYBodyRot(player.yBodyRot);
    }

    public void storeMeatspaceInventory(ServerPlayer player) {
        meatspaceInventory = serializeInventory(player);
        inventoryReleased = false;
    }

    public void restoreMeatspaceInventory(ServerPlayer player) {
        if (inventoryReleased) return;

        deserializeInventory(player, meatspaceInventory);
        inventoryReleased = true;
        meatspaceInventory = new CompoundTag();
    }

    public void discardStoredInventory() {
        inventoryReleased = true;
        meatspaceInventory = new CompoundTag();
    }

    public void dropMeatspaceInventory() {
        if (inventoryReleased) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (meatspaceInventory.contains(NBT_INVENTORY_ITEMS, Tag.TAG_LIST)) {
            ListTag items = meatspaceInventory.getList(NBT_INVENTORY_ITEMS, Tag.TAG_COMPOUND);

            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompound(i);

                if (!entry.contains(NBT_INVENTORY_STACK, Tag.TAG_COMPOUND)) continue;

                ItemStack stack = ItemStack.parseOptional(serverLevel.registryAccess(), entry.getCompound(NBT_INVENTORY_STACK));
                if (stack.isEmpty()) continue;

                spawnAtLocation(stack);
            }
        }

        inventoryReleased = true;
        meatspaceInventory = new CompoundTag();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        ServerPlayer owner = getOwner();

        if (owner != null && owner.isCreative()) {
            return false;
        }

        return super.canBeSeenAsEnemy();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }

        boolean hurt = super.hurt(source, amount);

        if (!hurt) {
            return false;
        }

        if (isAlive() && getHealth() > 0.0F) {
            syncHealthToOwner();
        }

        return true;
    }

    private void syncHealthToOwner() {
        ServerPlayer owner = getOwner();
        if (owner == null) return;
        if (!owner.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) return;

        owner.setHealth(Math.max(1.0F, getHealth()));
    }

    @Override
    public void die(DamageSource damageSource) {
        ServerPlayer owner = getOwner();

        super.die(damageSource);

        if (level().isClientSide) return;

        dropMeatspaceInventory();

        if (owner == null) return;
        if (!owner.level().dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) return;
        if (!owner.isAlive()) return;

        CyberspaceTravelManager.handleStandInBodyDeath(owner, this);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        ServerPlayer owner = getOwner();
        return owner != null ? owner.getMainArm() : HumanoidArm.RIGHT;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        UUID ownerUUID = getOwnerUUID();

        if (ownerUUID != null) {
            tag.putUUID(NBT_OWNER, ownerUUID);
        }

        CompoundTag cyberwareSnapshot = getCyberwareSnapshot();

        if (!cyberwareSnapshot.isEmpty()) {
            tag.put(NBT_CYBERWARE_SNAPSHOT, cyberwareSnapshot);
        }

        BlockPos tetherPos = getTetherPos();

        if (tetherPos != null) {
            tag.putLong(NBT_TETHER_POS, tetherPos.asLong());
            tag.putString(NBT_TETHER_FACING, getTetherFacing().getName());
        }

        if (!inventoryReleased && !meatspaceInventory.isEmpty()) {
            tag.put(NBT_MEATSPACE_INVENTORY, meatspaceInventory.copy());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID(NBT_OWNER)) {
            setOwnerUUID(tag.getUUID(NBT_OWNER));
        }

        if (tag.contains(NBT_CYBERWARE_SNAPSHOT, Tag.TAG_COMPOUND)) {
            setCyberwareSnapshot(tag.getCompound(NBT_CYBERWARE_SNAPSHOT));
        }

        if (tag.contains(NBT_TETHER_POS, Tag.TAG_LONG)) {
            BlockPos tetherPos = BlockPos.of(tag.getLong(NBT_TETHER_POS));
            Direction tetherFacing = Direction.byName(tag.getString(NBT_TETHER_FACING));

            if (tetherFacing == null) {
                tetherFacing = Direction.NORTH;
            }

            setTetherAnchor(tetherPos, tetherFacing);
        } else {
            clearTetherAnchor();
        }

        if (tag.contains(NBT_MEATSPACE_INVENTORY, Tag.TAG_COMPOUND)) {
            meatspaceInventory = tag.getCompound(NBT_MEATSPACE_INVENTORY).copy();
            inventoryReleased = false;
        } else {
            meatspaceInventory = new CompoundTag();
            inventoryReleased = true;
        }
    }

    private static CompoundTag serializeInventory(ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        ListTag items = new ListTag();
        Inventory inventory = player.getInventory();
        HolderLookup.Provider provider = player.registryAccess();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;

            CompoundTag entry = new CompoundTag();
            entry.putInt(NBT_INVENTORY_SLOT, slot);
            entry.put(NBT_INVENTORY_STACK, stack.save(provider));
            items.add(entry);
        }

        root.put(NBT_INVENTORY_ITEMS, items);
        root.putInt(NBT_SELECTED_SLOT, inventory.selected);

        return root;
    }

    private static void deserializeInventory(ServerPlayer player, CompoundTag root) {
        Inventory inventory = player.getInventory();
        inventory.clearContent();

        if (root.contains(NBT_INVENTORY_ITEMS, Tag.TAG_LIST)) {
            ListTag items = root.getList(NBT_INVENTORY_ITEMS, Tag.TAG_COMPOUND);

            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompound(i);
                int slot = entry.getInt(NBT_INVENTORY_SLOT);

                if (slot < 0 || slot >= inventory.getContainerSize()) continue;
                if (!entry.contains(NBT_INVENTORY_STACK, Tag.TAG_COMPOUND)) continue;

                ItemStack stack = ItemStack.parseOptional(player.registryAccess(), entry.getCompound(NBT_INVENTORY_STACK));
                inventory.setItem(slot, stack);
            }
        }

        if (root.contains(NBT_SELECTED_SLOT, Tag.TAG_INT)) {
            inventory.selected = Math.max(0, Math.min(8, root.getInt(NBT_SELECTED_SLOT)));
        }

        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }
}