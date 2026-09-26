package com.misgenerate.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class PortalFacadeBlock extends Block implements BlockEntityProvider {

	public PortalFacadeBlock(Settings settings) {
		super(settings);
	}

	@Override
	public int getOpacity(BlockState state, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos) {
		return 0;
	}

	@Override
	public float calcBlockBreakingDelta(BlockState state, net.minecraft.entity.player.PlayerEntity player, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos) {
		net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof PortalFacadeBlockEntity facade) {
			if (facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SOLID_STONE) {
				return super.calcBlockBreakingDelta(state, player, world, pos);
			}
		}
		// Unbreakable for Overworld portals
		return 0.0f;
	}

	@Override
	public net.minecraft.util.shape.VoxelShape getCameraCollisionShape(BlockState state, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.ShapeContext context) {
		return net.minecraft.util.shape.VoxelShapes.empty();
	}

	@Override
	public net.minecraft.util.shape.VoxelShape getCullingShape(BlockState state, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos) {
		return net.minecraft.util.shape.VoxelShapes.empty();
	}

	@Override
	public boolean isTransparent(BlockState state, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos) {
		return true;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public void onStateReplaced(BlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, BlockState newState, boolean moved) {
		if (state.getBlock() != newState.getBlock() && !world.isClient && world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
			// Always remove portals nearby and trigger dimension-wide cleanup when the facade is destroyed
			// We do this unconditionally because the BlockEntity might already be removed by the time this is called!
			serverWorld.getServer().execute(() -> {
				com.misgenerate.portal.PortalSpawner.removePortalsAt(serverWorld, pos);
			});
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	public static void revertSeamBlocks(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.BlockPos centerPos) {
		int radius = 8;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					net.minecraft.util.math.BlockPos current = centerPos.add(dx, dy, dz);
					net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(current);
					
					if (be instanceof PortalFacadeBlockEntity facade) {
						if (facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SEAM_OVERWORLD || 
						    facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SOLID_STONE) {
							BlockState originalState = facade.getOverworldState();
							if (originalState != null && !originalState.isAir()) {
								world.setBlockState(current, originalState, 3);
							} else {
								world.setBlockState(current, net.minecraft.block.Blocks.GRASS_BLOCK.getDefaultState(), 3);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.ShapeContext context) {
		net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof PortalFacadeBlockEntity facade) {
			if (facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SEAM_OVERWORLD) {
				net.minecraft.util.math.Direction facing = facade.getFacing();
				if (facing != null) {
					double minX = 0, minY = 0, minZ = 0;
					double maxX = 1, maxY = 1, maxZ = 1;
					if (facing == net.minecraft.util.math.Direction.UP) maxY = 0.9;
					else if (facing == net.minecraft.util.math.Direction.DOWN) minY = 0.1;
					else if (facing == net.minecraft.util.math.Direction.NORTH) minZ = 0.1;
					else if (facing == net.minecraft.util.math.Direction.SOUTH) maxZ = 0.9;
					else if (facing == net.minecraft.util.math.Direction.WEST) minX = 0.1;
					else if (facing == net.minecraft.util.math.Direction.EAST) maxX = 0.9;
					return net.minecraft.util.shape.VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
				}
			} else if (facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SOLID_STONE) {
				// Shrink the outline shape by a microscopic amount (0.001) on all sides.
				// This ensures that a cross-dimensional raytrace starting EXACTLY on the block boundary (from the portal)
				// will not accidentally intersect the SOLID_STONE block, while keeping it perfectly breakable!
				return net.minecraft.util.shape.VoxelShapes.cuboid(0.001, 0.001, 0.001, 0.999, 0.999, 0.999);
			} else if (facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SEAM_MISGENERATE) {
				// Misgenerate seams are unbreakable from the front (void), but if they were, they'd have this shape.
				// We shrink it just in case.
				return net.minecraft.util.shape.VoxelShapes.cuboid(0.001, 0.001, 0.001, 0.999, 0.999, 0.999);
			}
		}
		return net.minecraft.util.shape.VoxelShapes.fullCube();
	}

	@Override
	public net.minecraft.util.shape.VoxelShape getCollisionShape(BlockState state, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.ShapeContext context) {
		net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof PortalFacadeBlockEntity facade) {
			if (facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SEAM_MISGENERATE || facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SOLID_STONE) {
				return net.minecraft.util.shape.VoxelShapes.empty();
			}
			net.minecraft.util.math.Direction facing = facade.getFacing();
			if (facing != null && facade.getFacadeType() == PortalFacadeBlockEntity.FacadeType.SEAM_OVERWORLD) {
				// Shrink the collision box slightly on the facing side
				// This allows the player to step slightly 'inside' the block and touch the portal!
				double minX = 0, minY = 0, minZ = 0;
				double maxX = 1, maxY = 1, maxZ = 1;
				
				if (facing == net.minecraft.util.math.Direction.UP) maxY = 0.9;
				else if (facing == net.minecraft.util.math.Direction.DOWN) minY = 0.1;
				else if (facing == net.minecraft.util.math.Direction.NORTH) minZ = 0.1;
				else if (facing == net.minecraft.util.math.Direction.SOUTH) maxZ = 0.9;
				else if (facing == net.minecraft.util.math.Direction.WEST) minX = 0.1;
				else if (facing == net.minecraft.util.math.Direction.EAST) maxX = 0.9;
				
				return net.minecraft.util.shape.VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
			}
		}
		return net.minecraft.util.shape.VoxelShapes.fullCube();
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PortalFacadeBlockEntity(pos, state);
	}
}
