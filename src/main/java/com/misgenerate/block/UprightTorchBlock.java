package com.misgenerate.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Map;

public class UprightTorchBlock extends FacingBlock {
    public static final MapCodec<UprightTorchBlock> CODEC = createCodec(UprightTorchBlock::new);

    private static final Map<Direction, VoxelShape> BOUNDING_SHAPES = Maps.newEnumMap(ImmutableMap.of(
            Direction.UP, Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 10.0, 10.0),
            Direction.DOWN, Block.createCuboidShape(6.0, 6.0, 6.0, 10.0, 16.0, 10.0),
            Direction.NORTH, Block.createCuboidShape(6.0, 6.0, 6.0, 10.0, 10.0, 16.0),
            Direction.SOUTH, Block.createCuboidShape(6.0, 6.0, 0.0, 10.0, 10.0, 10.0),
            Direction.WEST, Block.createCuboidShape(6.0, 6.0, 6.0, 16.0, 10.0, 10.0),
            Direction.EAST, Block.createCuboidShape(0.0, 6.0, 6.0, 10.0, 10.0, 10.0)
    ));

    public UprightTorchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends FacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getSide());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BOUNDING_SHAPES.get(state.get(FACING));
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        Direction direction = state.get(FACING);
        double d = (double)pos.getX() + 0.5;
        double e = (double)pos.getY() + 0.5;
        double f = (double)pos.getZ() + 0.5;
        
        d += direction.getOffsetX() * 0.2;
        e += direction.getOffsetY() * 0.2;
        f += direction.getOffsetZ() * 0.2;
        
        world.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0);
        world.addParticle(ParticleTypes.FLAME, d, e, f, 0.0, 0.0, 0.0);
    }
}
