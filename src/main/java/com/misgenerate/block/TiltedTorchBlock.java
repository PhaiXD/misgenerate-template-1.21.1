package com.misgenerate.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Map;

public class TiltedTorchBlock extends WallMountedBlock {
    public static final MapCodec<TiltedTorchBlock> CODEC = createCodec(TiltedTorchBlock::new);

    private static final Map<Direction, VoxelShape> WALL_SHAPES = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.createCuboidShape(5.5, 3.0, 11.0, 10.5, 13.0, 16.0),
            Direction.SOUTH, Block.createCuboidShape(5.5, 3.0, 0.0, 10.5, 13.0, 5.0),
            Direction.WEST, Block.createCuboidShape(11.0, 3.0, 5.5, 16.0, 13.0, 10.5),
            Direction.EAST, Block.createCuboidShape(0.0, 3.0, 5.5, 5.0, 13.0, 10.5)
    ));
    
    private static final Map<Direction, VoxelShape> FLOOR_SHAPES = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.createCuboidShape(7.0, 0.0, -2.0, 9.0, 2.0, 8.0),
            Direction.SOUTH, Block.createCuboidShape(7.0, 0.0, 8.0, 9.0, 2.0, 18.0),
            Direction.WEST, Block.createCuboidShape(-2.0, 0.0, 7.0, 8.0, 2.0, 9.0),
            Direction.EAST, Block.createCuboidShape(8.0, 0.0, 7.0, 18.0, 2.0, 9.0)
    ));

    private static final Map<Direction, VoxelShape> CEILING_SHAPES = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.createCuboidShape(7.0, 14.0, -2.0, 9.0, 16.0, 8.0),
            Direction.SOUTH, Block.createCuboidShape(7.0, 14.0, 8.0, 9.0, 16.0, 18.0),
            Direction.WEST, Block.createCuboidShape(-2.0, 14.0, 7.0, 8.0, 16.0, 9.0),
            Direction.EAST, Block.createCuboidShape(8.0, 14.0, 7.0, 18.0, 16.0, 9.0)
    ));

    public TiltedTorchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(FACE, BlockFace.WALL));
    }

    @Override
    protected MapCodec<? extends WallMountedBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACE)) {
            case FLOOR -> FLOOR_SHAPES.get(state.get(FACING));
            case CEILING -> CEILING_SHAPES.get(state.get(FACING));
            default -> WALL_SHAPES.get(state.get(FACING));
        };
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        Direction direction = state.get(FACING);
        BlockFace face = state.get(FACE);
        
        double d = (double)pos.getX() + 0.5;
        double e = (double)pos.getY() + 0.5;
        double f = (double)pos.getZ() + 0.5;

        if (face == BlockFace.WALL) {
            Direction direction2 = direction.getOpposite();
            e += 0.22;
            d += 0.27 * direction2.getOffsetX();
            f += 0.27 * direction2.getOffsetZ();
        } else if (face == BlockFace.FLOOR) {
            e -= 0.26;
            d += 0.625 * direction.getOffsetX();
            f += 0.625 * direction.getOffsetZ();
        } else if (face == BlockFace.CEILING) {
            e += 0.26;
            d += 0.625 * direction.getOffsetX();
            f += 0.625 * direction.getOffsetZ();
        }

        world.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0);
        world.addParticle(ParticleTypes.FLAME, d, e, f, 0.0, 0.0, 0.0);
    }
}
