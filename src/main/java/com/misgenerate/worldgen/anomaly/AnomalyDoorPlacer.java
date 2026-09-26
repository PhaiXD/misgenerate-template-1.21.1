package com.misgenerate.worldgen.anomaly;

import com.misgenerate.Misgenerate;
import com.misgenerate.block.ModBlocks;
import com.misgenerate.worldgen.scanner.SurfaceScanner;
import com.misgenerate.worldgen.scanner.SurfaceScanner.SurfaceResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.server.world.ServerWorld;

/**
 * Type 3: The Anomaly Door — a normal-looking door.
 * Portal spawning is deferred to player interaction (AnomalyDoorBlock.onUse).
 *
 * Placement:
 * - Only spawns in Mineshaft (random wood) or Stronghold (oak)
 * - Always wall-attached
 * - Placed in the AIR space adjacent to the surface (not replacing the surface)
 */
public class AnomalyDoorPlacer {

	public static boolean place(StructureWorldAccess world, SurfaceResult surface, Random random, boolean isMineshaft) {
		if (surface.maxHeight() < 2 || !surface.isWall()) return false;

		Direction normal = surface.facing();
		
		// Instead of placing the door in the air, we dig a 1-deep doorway into the wall!
		BlockPos doorPos = surface.solidPos();
		BlockPos upperPos = doorPos.up();
		
		// Check if the wall actually has 2 blocks of height to dig into
		if (!world.getBlockState(doorPos).isOpaqueFullCube(world, doorPos) || 
			!world.getBlockState(upperPos).isOpaqueFullCube(world, upperPos)) {
			return false;
		}

		// Check if the air blocks in front of the door are clear
		BlockPos airPos = doorPos.offset(normal);
		if (!canPlaceDoorAt(world, airPos) || !canPlaceDoorAt(world, airPos.up())) {
			return false;
		}

		// Ensure we are truly in a mineshaft by checking for structural blocks nearby
		if (isMineshaft) {
			boolean hasMineshaftBlocks = false;
			for (int dx = -4; dx <= 4; dx++) {
				for (int dy = -4; dy <= 4; dy++) {
					for (int dz = -4; dz <= 4; dz++) {
						BlockState st = world.getBlockState(doorPos.add(dx, dy, dz));
						if (st.isOf(net.minecraft.block.Blocks.OAK_PLANKS) ||
							st.isOf(net.minecraft.block.Blocks.OAK_FENCE) ||
							st.isOf(net.minecraft.block.Blocks.COBWEB) ||
							st.isOf(net.minecraft.block.Blocks.RAIL)) {
							hasMineshaftBlocks = true;
							break;
						}
					}
					if (hasMineshaftBlocks) break;
				}
				if (hasMineshaftBlocks) break;
			}
			if (!hasMineshaftBlocks) return false;
		}

		// Face the door OUT of the wall, so it sits flush with the front of the hole and swings INWARDS
		Direction doorFacing = normal;

		Block doorBlockType = ModBlocks.WOODEN_ANOMALY_DOORS[random.nextInt(ModBlocks.WOODEN_ANOMALY_DOORS.length)];
		BlockState doorBlock = doorBlockType.getDefaultState();

		BlockState lowerState = doorBlock
				.with(Properties.HORIZONTAL_FACING, doorFacing)
				.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
				.with(Properties.DOOR_HINGE, random.nextBoolean() ? DoorHinge.LEFT : DoorHinge.RIGHT)
				.with(Properties.OPEN, false);

		BlockState upperState = doorBlock
				.with(Properties.HORIZONTAL_FACING, doorFacing)
				.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
				.with(Properties.DOOR_HINGE, lowerState.get(Properties.DOOR_HINGE))
				.with(Properties.OPEN, false);

		world.setBlockState(doorPos, lowerState, 3);
		world.setBlockState(upperPos, upperState, 3);

		// Anchor blocks are the blocks BEHIND the doorway (deeper in the wall)
		BlockPos anchorLower = doorPos.offset(normal.getOpposite());
		BlockPos anchorUpper = upperPos.offset(normal.getOpposite());
		BlockState origLower = world.getBlockState(anchorLower);
		BlockState origUpper = world.getBlockState(anchorUpper);

		ServerWorld serverWorld = world.toServerWorld();
		serverWorld.getServer().execute(() -> {
			BlockState facadeState = ModBlocks.PORTAL_FACADE_BLOCK.getDefaultState();
			
			serverWorld.setBlockState(anchorLower, facadeState, 3);
			if (serverWorld.getBlockEntity(anchorLower) instanceof com.misgenerate.block.PortalFacadeBlockEntity be) {
				be.setFacadeType(com.misgenerate.block.PortalFacadeBlockEntity.FacadeType.SOLID_STONE);
				be.setFacadeData(origLower.isAir() ? net.minecraft.block.Blocks.STONE.getDefaultState() : origLower, 
								 net.minecraft.block.Blocks.AIR.getDefaultState(), normal, Vec3d.ZERO);
			}
			
			serverWorld.setBlockState(anchorUpper, facadeState, 3);
			if (serverWorld.getBlockEntity(anchorUpper) instanceof com.misgenerate.block.PortalFacadeBlockEntity be) {
				be.setFacadeType(com.misgenerate.block.PortalFacadeBlockEntity.FacadeType.SOLID_STONE);
				be.setFacadeData(origUpper.isAir() ? net.minecraft.block.Blocks.STONE.getDefaultState() : origUpper, 
								 net.minecraft.block.Blocks.AIR.getDefaultState(), normal, Vec3d.ZERO);
			}
		});

		// Portal center aligned with the BACK of the doorway
		Vec3d axisH = new Vec3d(0, 1, 0);
		Vec3d axisW = com.misgenerate.portal.PortalSpawner.computePortalAxisW(Direction.UP, normal);
		
		// Push the portal to exactly 0.5 in the wall direction (boundary of the door block and anchor)
		// We set interactable=false on the forward portal so it doesn't intercept door clicks!
		double px = doorPos.getX() + 0.5 + normal.getOpposite().getOffsetX() * 0.5;
		double py = doorPos.getY() + 1.0;
		double pz = doorPos.getZ() + 0.5 + normal.getOpposite().getOffsetZ() * 0.5;
		Vec3d portalCenter = new Vec3d(px, py, pz);
		
		// Scale X and Z by 7 for the Misgenerate dimension (1 block Overworld = 7 blocks Misgenerate)
		double dy = 100 - portalCenter.y;
		Vec3d destination = new Vec3d(portalCenter.x * 7.0, portalCenter.y + dy, portalCenter.z * 7.0);
		
		// Clear a small 3x3x3 room in the Misgenerate world at the destination so the player doesn't get stuck in solid stone!
		serverWorld.getServer().execute(() -> {
			net.minecraft.server.world.ServerWorld misgenerateWorld = serverWorld.getServer().getWorld(com.misgenerate.dimension.ModDimensions.MISGENERATE_WORLD);
			if (misgenerateWorld != null) {
				BlockPos destPos = BlockPos.ofFloored(destination);
				// Clear a 3x3x3 room (mostly in front of the door)
				for (int dx = -1; dx <= 1; dx++) {
					for (int dY = 0; dY <= 2; dY++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos clearPos = destPos.add(dx, dY - 1, dz); // -1 to start at floor level
							misgenerateWorld.setBlockState(clearPos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
						}
					}
				}
				// Place a floor so they don't fall if it happens to be hollow
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos floorPos = destPos.add(dx, -2, dz);
						misgenerateWorld.setBlockState(floorPos, net.minecraft.block.Blocks.STONE.getDefaultState(), 3);
					}
				}
			}
		});

		// Spawn portal unconditionally (uses spawnDoorPortal which sets forward interactable=false)
		com.misgenerate.portal.PortalSpawner.spawnDoorPortal(
				world, portalCenter, axisW, axisH, 1.0, 2.0, destination
		);

		Misgenerate.LOGGER.debug("Placed Anomaly Door at {} facing {} (Mineshaft: {})",
				doorPos, doorFacing, isMineshaft);
		return true;
	}

	private static boolean canPlaceDoorAt(StructureWorldAccess world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isAir() || state.isReplaceable();
	}
}
