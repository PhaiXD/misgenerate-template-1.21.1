package com.misgenerate.portal;

import com.misgenerate.Misgenerate;
import com.misgenerate.dimension.ModDimensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.StructureWorldAccess;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.List;
import java.util.Map;

/**
 * Centralized utility for creating Immersive Portal entities.
 *
 * Portal orientation math:
 *   portal normal = axisW × axisH
 *   The portal is VISIBLE from the side the normal points to.
 *   So we compute axisW = heightDir × normal (cross product)
 *   to ensure axisW × axisH = surface normal → portal faces toward the air.
 */
public class PortalSpawner {

	/**
	 * Compute the correct portal axisW so that axisW × axisH = surfaceNormal.
	 */
	public static Vec3d computePortalAxisW(Direction heightDir, Direction normal) {
		Vec3d h = new Vec3d(heightDir.getOffsetX(), heightDir.getOffsetY(), heightDir.getOffsetZ());
		Vec3d n = new Vec3d(normal.getOffsetX(), normal.getOffsetY(), normal.getOffsetZ());
		return h.crossProduct(n);
	}

	/**
	 * Get the portal axisH vector for a given height direction.
	 */
	public static Vec3d getPortalAxisH(Direction heightDir) {
		return new Vec3d(heightDir.getOffsetX(), heightDir.getOffsetY(), heightDir.getOffsetZ());
	}

	/**
	 * Calculate the face-aligned portal center for a surface anomaly.
	 * Places the portal at the specified offset from the OUTER FACE of the block.
	 */
	public static Vec3d calculateFaceAlignedCenter(
			BlockPos solidPos,
			Direction widthDir, Direction heightDir, Direction normal,
			int width, int height, double offsetFromFace
	) {
		double cx = solidPos.getX() + 0.5
				+ widthDir.getOffsetX() * (width - 1) * 0.5
				+ heightDir.getOffsetX() * (height - 1) * 0.5;
		double cy = solidPos.getY() + 0.5
				+ widthDir.getOffsetY() * (width - 1) * 0.5
				+ heightDir.getOffsetY() * (height - 1) * 0.5;
		double cz = solidPos.getZ() + 0.5
				+ widthDir.getOffsetZ() * (width - 1) * 0.5
				+ heightDir.getOffsetZ() * (height - 1) * 0.5;

		// Shift to the OUTER FACE of the block surface (+0.5 along normal), plus additional offset
		double totalOffset = 0.5 + offsetFromFace;
		cx += normal.getOffsetX() * totalOffset;
		cy += normal.getOffsetY() * totalOffset;
		cz += normal.getOffsetZ() * totalOffset;

		return new Vec3d(cx, cy, cz);
	}

	// ===== Bi-directional portal spawning =====

	/**
	 * Spawn a bi-directional portal (worldgen context — uses toServerWorld()).
	 */
	public static void spawnBidirectionalPortal(
			StructureWorldAccess world,
			Vec3d position, Vec3d axisW, Vec3d axisH,
			double width, double height, Vec3d destination
	) {
		try {
			ServerWorld serverWorld = world.toServerWorld();
			serverWorld.getServer().execute(() -> {
				spawnBidirectionalPortalImpl(serverWorld, position, axisW, axisH, width, height, destination, true);
			});
		} catch (Exception e) {
			Misgenerate.LOGGER.error("Failed to schedule portal spawn at {}", position, e);
		}
	}

	/**
	 * Spawn a bi-directional portal with default destination (Y=100 in misgenerate).
	 */
	public static void spawnBidirectionalPortal(
			StructureWorldAccess world,
			Vec3d position, Vec3d axisW, Vec3d axisH,
			double width, double height
	) {
		Vec3d destination = new Vec3d(position.x, 100, position.z);
		spawnBidirectionalPortal(world, position, axisW, axisH, width, height, destination);
	}

	/**
	 * Spawn a bi-directional portal (ServerWorld context — for doors).
	 */
	public static void spawnDoorPortal(
			ServerWorld world,
			Vec3d position, Vec3d axisW, Vec3d axisH,
			double width, double height, Vec3d destination
	) {
		try {
			// For doors, forward portal is NOT interactable so it doesn't intercept door clicks!
			spawnBidirectionalPortalImpl(world, position, axisW, axisH, width, height, destination, false);
		} catch (Exception e) {
			Misgenerate.LOGGER.error("Failed to spawn door portal at {}", position, e);
		}
	}

	/**
	 * Spawn a bi-directional door portal (worldgen context).
	 */
	public static void spawnDoorPortal(
			StructureWorldAccess world,
			Vec3d position, Vec3d axisW, Vec3d axisH,
			double width, double height, Vec3d destination
	) {
		try {
			ServerWorld serverWorld = world.toServerWorld();
			serverWorld.getServer().execute(() -> {
				spawnDoorPortal(serverWorld, position, axisW, axisH, width, height, destination);
			});
		} catch (Exception e) {
			Misgenerate.LOGGER.error("Failed to schedule door portal spawn at {}", position, e);
		}
	}

	/**
	 * Core implementation: creates forward + return portal.
	 */
	private static void spawnBidirectionalPortalImpl(
			ServerWorld serverWorld,
			Vec3d position, Vec3d axisW, Vec3d axisH,
			double width, double height, Vec3d destination, boolean forwardInteractable
	) {
		// === Forward portal: origin → misgenerate ===
		Portal forward = Portal.ENTITY_TYPE.create(serverWorld);
		if (forward == null) {
			Misgenerate.LOGGER.error("Failed to create forward Portal entity");
			return;
		}

		forward.setOriginPos(position);
		forward.setDestinationDimension(ModDimensions.MISGENERATE_WORLD);
		forward.setDestination(destination);
		forward.setOrientationAndSize(axisW, axisH, width, height);
		forward.setTeleportable(true);
		forward.setInteractable(forwardInteractable);
		PortalAPI.spawnServerEntity(forward);

		// === Return portal: misgenerate → origin ===
		ServerWorld destWorld = serverWorld.getServer().getWorld(ModDimensions.MISGENERATE_WORLD);
		if (destWorld != null) {

			Portal reverse = Portal.ENTITY_TYPE.create(destWorld);
			if (reverse != null) {
				reverse.setOriginPos(destination);
				reverse.setDestinationDimension(serverWorld.getRegistryKey());
				reverse.setDestination(position);
				// Flip portal facing by negating axisW
				reverse.setOrientationAndSize(axisW.negate(), axisH, width, height);
				reverse.setTeleportable(true);
				reverse.setInteractable(true);
				PortalAPI.spawnServerEntity(reverse);
			}
		}

		Misgenerate.LOGGER.debug("Spawned bi-directional portal at {} → {} ({}x{})",
				position, destination, width, height);
	}



	/**
	 * Remove all anomaly portal entities near a given block position.
	 */
	public static void removePortalsAt(ServerWorld world, BlockPos pos) {
		Box searchBox = new Box(pos).expand(1.5);

		// Find ALL portals near the pos (either forward or return)
		List<Portal> portals = world.getEntitiesByClass(
				Portal.class, searchBox,
				portal -> true // Just get all portals nearby
		);

		for (Portal portal : portals) {
			ServerWorld destWorld = world.getServer().getWorld(portal.getDestDim());
			if (destWorld != null) {
				// Destroy return portals
				net.minecraft.util.math.Box returnSearchBox = new net.minecraft.util.math.Box(
						portal.getDestPos().add(-1.5, -1.5, -1.5),
						portal.getDestPos().add(1.5, 1.5, 1.5)
				);
				List<Portal> returnPortals = destWorld.getEntitiesByClass(
						Portal.class, returnSearchBox,
						p -> p.getDestDim() == world.getRegistryKey()
				);
				for (Portal rp : returnPortals) rp.discard();
				
				// Identify Overworld and Misgenerate
				ServerWorld overworld = null;
				ServerWorld misgenerate = null;
				BlockPos overworldPos = null;
				BlockPos misgeneratePos = null;

				if (world.getRegistryKey() == ModDimensions.MISGENERATE_WORLD) {
					misgenerate = world;
					misgeneratePos = pos;
					overworld = destWorld;
					overworldPos = BlockPos.ofFloored(portal.getDestPos());
				} else {
					overworld = world;
					overworldPos = pos;
					misgenerate = destWorld;
					misgeneratePos = BlockPos.ofFloored(portal.getDestPos());
				}

				// Clean up Overworld blocks
				if (overworld != null && overworldPos != null) {
					com.misgenerate.block.PortalFacadeBlock.revertSeamBlocks(overworld, overworldPos);

					// Convert unbreakable Anomaly Doors back into normal Vanilla Doors
					for (int dx = -2; dx <= 2; dx++) {
						for (int dy = -2; dy <= 2; dy++) {
							for (int dz = -2; dz <= 2; dz++) {
								BlockPos current = overworldPos.add(dx, dy, dz);
								BlockState state = overworld.getBlockState(current);
								if (state.getBlock() instanceof com.misgenerate.block.AnomalyDoorBlock) {
									net.minecraft.block.Block vanillaDoor = net.minecraft.block.Blocks.OAK_DOOR;
									
									if (state.isOf(com.misgenerate.block.ModBlocks.SPRUCE_ANOMALY_DOOR)) vanillaDoor = net.minecraft.block.Blocks.SPRUCE_DOOR;
									else if (state.isOf(com.misgenerate.block.ModBlocks.BIRCH_ANOMALY_DOOR)) vanillaDoor = net.minecraft.block.Blocks.BIRCH_DOOR;
									else if (state.isOf(com.misgenerate.block.ModBlocks.JUNGLE_ANOMALY_DOOR)) vanillaDoor = net.minecraft.block.Blocks.JUNGLE_DOOR;
									else if (state.isOf(com.misgenerate.block.ModBlocks.ACACIA_ANOMALY_DOOR)) vanillaDoor = net.minecraft.block.Blocks.ACACIA_DOOR;
									else if (state.isOf(com.misgenerate.block.ModBlocks.DARK_OAK_ANOMALY_DOOR)) vanillaDoor = net.minecraft.block.Blocks.DARK_OAK_DOOR;
									else if (state.isOf(com.misgenerate.block.ModBlocks.IRON_ANOMALY_DOOR)) vanillaDoor = net.minecraft.block.Blocks.IRON_DOOR;
									
									BlockState vanillaState = vanillaDoor.getDefaultState()
										.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, state.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING))
										.with(net.minecraft.state.property.Properties.DOUBLE_BLOCK_HALF, state.get(net.minecraft.state.property.Properties.DOUBLE_BLOCK_HALF))
										.with(net.minecraft.state.property.Properties.DOOR_HINGE, state.get(net.minecraft.state.property.Properties.DOOR_HINGE))
										.with(net.minecraft.state.property.Properties.OPEN, state.get(net.minecraft.state.property.Properties.OPEN))
										.with(net.minecraft.state.property.Properties.POWERED, state.get(net.minecraft.state.property.Properties.POWERED));
									
									overworld.setBlockState(current, vanillaState, 3);
								}
							}
						}
					}
				}

				// Clean up Misgenerate blocks
				if (misgenerate != null && misgeneratePos != null) {
					for (int dx = -8; dx <= 8; dx++) {
						for (int dy = -8; dy <= 8; dy++) {
							for (int dz = -8; dz <= 8; dz++) {
								BlockPos checkPos = misgeneratePos.add(dx, dy, dz);
								BlockState misState = misgenerate.getBlockState(checkPos);
								if (misState.isOf(com.misgenerate.block.ModBlocks.PORTAL_FACADE_BLOCK)) {
									misgenerate.setBlockState(checkPos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
								}
							}
						}
					}
				}
			}
			portal.discard();
			Misgenerate.LOGGER.debug("Removed anomaly portal at {}", portal.getOriginPos());
		}
	}
}
