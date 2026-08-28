package com.perigrine3.createcybernetics.block.entity;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateCybernetics.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RobosurgeonBlockEntity>> ROBOSURGEON_BLOCKENTITY =
            BLOCK_ENTITIES.register("robosurgeon_blockentity", () ->
                    BlockEntityType.Builder.of(
                            RobosurgeonBlockEntity::new,
                            ModBlocks.ROBOSURGEON.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineeringTableBlockEntity>> ENGINEERING_TABLE_BLOCKENTITY =
            BLOCK_ENTITIES.register("engineering_table", () ->
                    BlockEntityType.Builder.of(
                            EngineeringTableBlockEntity::new,
                            ModBlocks.ENGINEERING_TABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HoloprojectorBlockEntity>> HOLOPROJECTOR_BLOCKENTITY =
            BLOCK_ENTITIES.register("holoprojector_blockentity", () ->
                    BlockEntityType.Builder.of(
                            HoloprojectorBlockEntity::new,
                            ModBlocks.HOLOPROJECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SurgeryTableBlockEntity>> SURGERY_TABLE =
            BLOCK_ENTITIES.register("surgery_table", () ->
                    BlockEntityType.Builder.of(
                            SurgeryTableBlockEntity::new,
                            ModBlocks.SURGERY_TABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingBlockEntity>> CHARGING_BLOCK =
            BLOCK_ENTITIES.register("charging_block", () ->
                    BlockEntityType.Builder.of(
                            ChargingBlockEntity::new,
                            ModBlocks.CHARGING_BLOCK.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerBlockEntity>> COMPUTER =
            BLOCK_ENTITIES.register("computer", () ->
                    BlockEntityType.Builder.of(
                            ComputerBlockEntity::new,
                            ModBlocks.COMPUTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerTowerBlockEntity>> COMPUTER_TOWER =
            BLOCK_ENTITIES.register("computer_tower", () ->
                    BlockEntityType.Builder.of(
                            ComputerTowerBlockEntity::new,
                            ModBlocks.COMPUTER_TOWER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerTowerBlockEntity>> SERVER_TOWER =
            BLOCK_ENTITIES.register("server_tower", () -> BlockEntityType.Builder.of(ServerTowerBlockEntity::new, ModBlocks.SERVER_TOWER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DataCacheBlockEntity>> DATA_CACHE =
            BLOCK_ENTITIES.register("data_cache", () ->
                    BlockEntityType.Builder.of(
                            DataCacheBlockEntity::new,
                            ModBlocks.DATA_CACHE.get()
                    ).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}