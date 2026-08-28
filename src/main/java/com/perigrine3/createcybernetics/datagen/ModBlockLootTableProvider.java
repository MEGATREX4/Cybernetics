package com.perigrine3.createcybernetics.datagen;

import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {

        dropSelf(ModBlocks.SURGERY_CHAMBER_BOTTOM.get());
        dropSelf(ModBlocks.SURGERY_CHAMBER_TOP.get());
        dropSelf(ModBlocks.ROBOSURGEON.get());
        dropSelf(ModBlocks.ENGINEERING_TABLE.get());
        dropSelf(ModBlocks.GRAFTING_TABLE.get());
        dropSelf(ModBlocks.CHARGING_BLOCK.get());
        dropSelf(ModBlocks.HOLOPROJECTOR.get());
        dropSelf(ModBlocks.SURGERY_TABLE.get());
        dropSelf(ModBlocks.COMPUTER.get());
        dropSelf(ModBlocks.COMPUTER_TOWER.get());

        add(ModBlocks.MACHINE_CASING.get(),
                block -> LootTable.lootTable().withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.COMPONENT_SSD.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))));

        dropSelf(ModBlocks.TITANIUM_BLOCK.get());

        dropSelf(ModBlocks.SMOOTH_TITANIUM.get());
        dropSelf(ModBlocks.SMOOTH_TITANIUM_STAIRS.get());
        add(ModBlocks.SMOOTH_TITANIUM_SLAB.get(),
                block -> createSlabItemTable(ModBlocks.SMOOTH_TITANIUM_SLAB.get()));

        dropSelf(ModBlocks.TITANIUM_GRATE.get());

        dropSelf(ModBlocks.TITANIUM_CLAD_COPPER.get());
        dropSelf(ModBlocks.TITANIUM_CLAD_COPPER_STAIRS.get());
        add(ModBlocks.TITANIUM_CLAD_COPPER_SLAB.get(),
                block -> createSlabItemTable(ModBlocks.TITANIUM_CLAD_COPPER_SLAB.get()));

        dropSelf(ModBlocks.ETCHED_TITANIUM_COPPER.get());
        dropSelf(ModBlocks.ETCHED_TITANIUM_COPPER_STAIRS.get());
        add(ModBlocks.ETCHED_TITANIUM_COPPER_SLAB.get(),
                block -> createSlabItemTable(ModBlocks.ETCHED_TITANIUM_COPPER_SLAB.get()));

        dropSelf(ModBlocks.RAW_TITANIUM_BLOCK.get());

        add(ModBlocks.SERVER_TOWER.get(), noDrop());

        add(ModBlocks.DIGITAL_ARCHITECTURE.get(), noDrop());
        add(ModBlocks.ACCESS_NODE.get(), noDrop());
        add(ModBlocks.DATA_CACHE.get(), noDrop());
        add(ModBlocks.DAEMON_ANCHOR.get(), noDrop());
        add(ModBlocks.DIGITAL_GATE.get(), noDrop());

        add(ModBlocks.TARGET_CODE_FRAGMENT.get(), block -> createWeightedDropTable(
                new WeightedDrop(ModItems.MICROPROGRAM_PROCESSOR.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_CYBEROPTICS.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_AUDITORY.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_MOTOR_CONTROL.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_ARMS.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_LEGS.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_INTERNAL_ORGANS.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_SKELETON.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_DERMAL.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_POWER_SYSTEM.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_CYBERDECK.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_NEURAL_SYSTEM.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_ACTIVE_CYBERWARE.get(), 8),
                new WeightedDrop(ModItems.MICROPROGRAM_RANDOM_IMPLANT.get(), 3),
                new WeightedDrop(ModItems.MICROPROGRAM_HIGHEST_DRAW.get(), 2),
                new WeightedDrop(ModItems.MICROPROGRAM_SYSTEM_ALL.get(), 1)));

        add(ModBlocks.FUNCTION_CODE_FRAGMENT.get(), block -> createWeightedDropTable(
                new WeightedDrop(ModItems.MICROPROGRAM_REBOOT.get(), 36),
                new WeightedDrop(ModItems.MICROPROGRAM_TOGGLE_OFF.get(), 36),
                new WeightedDrop(ModItems.MICROPROGRAM_TOGGLE_ON.get(), 36),
                new WeightedDrop(ModItems.MICROPROGRAM_PING.get(), 32),
                new WeightedDrop(ModItems.MICROPROGRAM_TRACE.get(), 28),
                new WeightedDrop(ModItems.MICROPROGRAM_LOOP.get(), 24),
                new WeightedDrop(ModItems.MICROPROGRAM_MALFUNCTION.get(), 20),
                new WeightedDrop(ModItems.MICROPROGRAM_OVERCLOCK.get(), 12),
                new WeightedDrop(ModItems.MICROPROGRAM_DRAIN.get(), 8),
                new WeightedDrop(ModItems.MICROPROGRAM_SPIKE.get(), 6),
                new WeightedDrop(ModItems.MICROPROGRAM_BURNOUT.get(), 4),
                new WeightedDrop(ModItems.MICROPROGRAM_DESYNC.get(), 4),
                new WeightedDrop(ModItems.MICROPROGRAM_PURGE.get(), 2),
                new WeightedDrop(ModItems.MICROPROGRAM_CORRUPT.get(), 1)));

        add(ModBlocks.MODIFIER_CODE_FRAGMENT.get(), block -> createWeightedDropTable(
                new WeightedDrop(ModItems.MICROPROGRAM_DELAY.get(), 36),
                new WeightedDrop(ModItems.MICROPROGRAM_EFFICIENCY.get(), 32),
                new WeightedDrop(ModItems.MICROPROGRAM_SILENT.get(), 28),
                new WeightedDrop(ModItems.MICROPROGRAM_RANDOMIZE.get(), 24),
                new WeightedDrop(ModItems.MICROPROGRAM_REPEAT.get(), 16),
                new WeightedDrop(ModItems.MICROPROGRAM_DURATION_PLUS.get(), 6),
                new WeightedDrop(ModItems.MICROPROGRAM_INTENSITY_PLUS.get(), 5),
                new WeightedDrop(ModItems.MICROPROGRAM_CASCADE.get(), 3),
                new WeightedDrop(ModItems.MICROPROGRAM_CONTAGION.get(), 2),
                new WeightedDrop(ModItems.MICROPROGRAM_DURATION_PLUS_PLUS.get(), 2),
                new WeightedDrop(ModItems.MICROPROGRAM_PERSISTENT.get(), 1)));

        dropSelf(ModBlocks.RAW_DATA_FRAGMENT.get());

        add(ModBlocks.TITANIUMORE_BLOCK.get(),
                block -> createOreDrop(ModBlocks.TITANIUMORE_BLOCK.get(), ModItems.RAWTITANIUM.get()));
        add(ModBlocks.DEEPSLATE_TITANIUMORE_BLOCK.get(),
                block -> createMultipleOreDrops(ModBlocks.DEEPSLATE_TITANIUMORE_BLOCK.get(), ModItems.RAWTITANIUM.get(), 2, 5));

        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);

        this.add(ModBlocks.DATURA_BUSH.get(), block -> this.applyExplosionDecay(
                block, LootTable.lootTable().withPool(LootPool.lootPool().when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.DATURA_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 3)))
                                .add(LootItem.lootTableItem(ModItems.DATURA_FLOWER.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE))))
                        .withPool(LootPool.lootPool().when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.DATURA_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 2)))
                                .add(LootItem.lootTableItem(ModItems.DATURA_FLOWER.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE))))));

        add(ModBlocks.POTTED_DATURA.get(),
                block -> createPotFlowerItemTable(ModItems.DATURA_FLOWER.get()));
    }

    private LootTable.Builder createWeightedDropTable(WeightedDrop... drops) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(ModItems.MATERIALIZED_DATA_MINER.get())));

        for (WeightedDrop drop : drops) {
            pool.add(LootItem.lootTableItem(drop.item()).setWeight(drop.weight()));
        }

        return LootTable.lootTable().withPool(pool);
    }

    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registryLookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registryLookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }

    private record WeightedDrop(Item item, int weight) {}
}