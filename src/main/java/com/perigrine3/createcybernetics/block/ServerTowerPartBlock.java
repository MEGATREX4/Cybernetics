package com.perigrine3.createcybernetics.block;

import com.perigrine3.createcybernetics.common.multiblock.ServerTowerMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ServerTowerPartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART_INDEX = IntegerProperty.create("part_index", 0, 11);

    private static final VoxelShape BODY = Shapes.block();

    private static final VoxelShape FRONT_NORTH = Shapes.or(
            box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 8.0D, 16.0D, 8.0D, 16.0D)
    );

    private static final VoxelShape FRONT_EAST = Shapes.or(
            box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 16.0D)
    );

    private static final VoxelShape FRONT_SOUTH = Shapes.or(
            box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D)
    );

    private static final VoxelShape FRONT_WEST = Shapes.or(
            box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            box(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D)
    );

    public ServerTowerPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART_INDEX, 0));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPartShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPartShape(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private static VoxelShape getPartShape(BlockState state) {
        if (state.getValue(PART_INDEX) != ServerTowerMultiblock.FRONT_EXTENSION_INDEX) {
            return BODY;
        }

        return switch (state.getValue(FACING)) {
            case EAST -> FRONT_EAST;
            case SOUTH -> FRONT_SOUTH;
            case WEST -> FRONT_WEST;
            default -> FRONT_NORTH;
        };
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerTowerMultiblock.breakPart(level, pos, state.getValue(FACING), state.getValue(PART_INDEX));
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART_INDEX);
    }
}