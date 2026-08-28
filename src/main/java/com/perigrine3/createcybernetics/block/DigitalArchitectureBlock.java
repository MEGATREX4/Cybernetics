package com.perigrine3.createcybernetics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class DigitalArchitectureBlock extends Block {
    public static final MapCodec<DigitalArchitectureBlock> CODEC = simpleCodec(DigitalArchitectureBlock::new);
    public static final BooleanProperty CORRUPTED = BooleanProperty.create("corrupted");

    private static final int CORRUPTION_CHECK_INTERVAL = 60;
    private static final float CORRUPTION_CHANCE = 0.25F;

    private static final int MIN_CORRUPTION_DURATION = 8;
    private static final int MAX_CORRUPTION_DURATION = 30;

    public DigitalArchitectureBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CORRUPTED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORRUPTED);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CORRUPTED)) {
            level.setBlock(pos, state.setValue(CORRUPTED, false), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, CORRUPTION_CHECK_INTERVAL);
            return;
        }

        if (shouldCorrupt(level, pos) && random.nextFloat() < CORRUPTION_CHANCE) {
            level.setBlock(pos, state.setValue(CORRUPTED, true), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, random.nextIntBetweenInclusive(MIN_CORRUPTION_DURATION, MAX_CORRUPTION_DURATION));

            return;
        }

        level.scheduleTick(pos, this, CORRUPTION_CHECK_INTERVAL);
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, CORRUPTION_CHECK_INTERVAL);
        }
    }

    private boolean shouldCorrupt(ServerLevel level, BlockPos pos) {
        return true;
    }
}