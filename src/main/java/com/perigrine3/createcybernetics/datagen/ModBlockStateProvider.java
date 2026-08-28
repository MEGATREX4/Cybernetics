package com.perigrine3.createcybernetics.datagen;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.DaturaBushBlock;
import com.perigrine3.createcybernetics.block.DigitalArchitectureBlock;
import com.perigrine3.createcybernetics.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, CreateCybernetics.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.TITANIUMORE_BLOCK);
        blockWithItem(ModBlocks.DEEPSLATE_TITANIUMORE_BLOCK);
        blockWithItem(ModBlocks.RAW_TITANIUM_BLOCK);
        blockWithItem(ModBlocks.TITANIUM_BLOCK);

        blockWithItem(ModBlocks.SMOOTH_TITANIUM);
        blockWithItem(ModBlocks.ETCHED_TITANIUM_COPPER);

        digitalArchitecture(ModBlocks.DIGITAL_ARCHITECTURE);
        blockWithItem(ModBlocks.DATA_CACHE);
        blockWithItem(ModBlocks.ACCESS_NODE);
        simpleBlockWithItem(ModBlocks.DAEMON_ANCHOR.get(), cubeAll(ModBlocks.DAEMON_ANCHOR.get()));
        simpleBlockWithItem(ModBlocks.DIGITAL_GATE.get(), cubeAll(ModBlocks.DIGITAL_GATE.get()));

        blockWithItem(ModBlocks.MACHINE_CASING);

        ModelFile serverTowerModel = new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "block/server_tower"));
        horizontalBlock(ModBlocks.SERVER_TOWER.get(), serverTowerModel);

        codeFragment(ModBlocks.TARGET_CODE_FRAGMENT);
        codeFragment(ModBlocks.FUNCTION_CODE_FRAGMENT);
        codeFragment(ModBlocks.MODIFIER_CODE_FRAGMENT);
        codeFragment(ModBlocks.RAW_DATA_FRAGMENT);

        stairsBlock(ModBlocks.SMOOTH_TITANIUM_STAIRS.get(), blockTexture(ModBlocks.SMOOTH_TITANIUM.get()));
        stairsBlock(ModBlocks.ETCHED_TITANIUM_COPPER_STAIRS.get(), blockTexture(ModBlocks.ETCHED_TITANIUM_COPPER.get()));
        stairsBlock(ModBlocks.TITANIUM_CLAD_COPPER_STAIRS.get(), blockTexture(ModBlocks.TITANIUM_CLAD_COPPER.get()));
        slabBlock(ModBlocks.SMOOTH_TITANIUM_SLAB.get(), blockTexture(ModBlocks.SMOOTH_TITANIUM.get()), blockTexture(ModBlocks.SMOOTH_TITANIUM.get()));
        slabBlock(ModBlocks.ETCHED_TITANIUM_COPPER_SLAB.get(), blockTexture(ModBlocks.ETCHED_TITANIUM_COPPER.get()), blockTexture(ModBlocks.ETCHED_TITANIUM_COPPER.get()));
        slabBlock(ModBlocks.TITANIUM_CLAD_COPPER_SLAB.get(), blockTexture(ModBlocks.TITANIUM_CLAD_COPPER.get()), blockTexture(ModBlocks.TITANIUM_CLAD_COPPER.get()));

        blockItem(ModBlocks.SMOOTH_TITANIUM_STAIRS);
        blockItem(ModBlocks.SMOOTH_TITANIUM_SLAB);
        blockItem(ModBlocks.ETCHED_TITANIUM_COPPER_STAIRS);
        blockItem(ModBlocks.ETCHED_TITANIUM_COPPER_SLAB);
        blockItem(ModBlocks.TITANIUM_CLAD_COPPER_STAIRS);
        blockItem(ModBlocks.TITANIUM_CLAD_COPPER_SLAB);

        blockWithItem(ModBlocks.ROBOSURGEON);

        makeBush(((SweetBerryBushBlock) ModBlocks.DATURA_BUSH.get()), "datura_bush_stage", "datura_bush_stage");
    }

    private void digitalArchitecture(DeferredBlock<?> deferredBlock) {
        ModelFile normalModel = models().cubeAll(name(deferredBlock.get()), blockTexture(deferredBlock.get()));

        ModelFile corruptedModel = models().cubeAll(
                name(deferredBlock.get()) + "_corrupted",
                ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "block/" + name(deferredBlock.get()) + "_corrupted")
        );

        getVariantBuilder(deferredBlock.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DigitalArchitectureBlock.CORRUPTED) ? corruptedModel : normalModel)
                .build());

        simpleBlockItem(deferredBlock.get(), normalModel);
    }

    private void codeFragment(DeferredBlock<? extends AmethystClusterBlock> deferredBlock) {
        ModelFile model = models().withExistingParent(name(deferredBlock.get()), mcLoc("block/amethyst_cluster"))
                .texture("cross", blockTexture(deferredBlock.get()))
                .renderType("cutout");

        getVariantBuilder(deferredBlock.get()).forAllStates(state -> {
            Direction direction = state.getValue(AmethystClusterBlock.FACING);
            int rotationX = direction == Direction.DOWN ? 180 : direction.getAxis().isHorizontal() ? 90 : 0;
            int rotationY = direction.getAxis().isVertical() ? 0 : ((int) direction.toYRot() + 180) % 360;

            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(rotationX)
                    .rotationY(rotationY)
                    .build();
        });

        simpleBlockItem(deferredBlock.get(), model);
    }

    public void makeBush(SweetBerryBushBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, modelName, textureName);
        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] states(BlockState state, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().cross(modelName + state.getValue(DaturaBushBlock.AGE),
                ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "block/" + textureName + state.getValue(DaturaBushBlock.AGE))).renderType("cutout"));
        return models;
    }

    private void cutoutBlockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), models().cubeAll(name(deferredBlock.get()), blockTexture(deferredBlock.get())).renderType("cutout"));
    }

    private void cutoutMippedBlockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), models().cubeAll(name(deferredBlock.get()), blockTexture(deferredBlock.get())).renderType("cutout_mipped"));
    }

    private static String name(net.minecraft.world.level.block.Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).getPath();
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("createcybernetics:block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("createcybernetics:block/" + deferredBlock.getId().getPath() + appendix));
    }
}