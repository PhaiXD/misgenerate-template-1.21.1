package com.misgenerate.worldgen.anomaly;

import com.misgenerate.Misgenerate;
import com.misgenerate.block.ModBlocks;
import com.misgenerate.block.PortalFacadeBlockEntity;
import com.misgenerate.dimension.ModDimensions;
import com.misgenerate.portal.PortalSpawner;
import com.misgenerate.worldgen.scanner.SurfaceScanner;
import com.misgenerate.worldgen.scanner.SurfaceScanner.SurfaceResult;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Type 1: The Seam — The Paper-Thin Facade Approach.
 *
 * Key behaviors:
 * - Does NOT replace solid overworld blocks.
 * - Width is ALWAYS 2 (no width-1 seams).
 * - Spawns a face-aligned Immersive Portal 0.001 blocks in front of the wall.
 * - In misgenerate, places PortalFacadeBlock with a crack scaling factor.
 * - Wall placement grounded to floor.
 */
public class SeamPlacer {

	public static boolean place(StructureWorldAccess world, SurfaceResult surface, Random random) {
		Direction widthDir = SurfaceScanner.getWidthDirection(surface);
		Direction heightDir = SurfaceScanner.getHeightDirection(surface);

		// === Width is ALWAYS 2 ===
		if (surface.maxWidth() < 2) return false;
		int width = 2;

		// === Wall enforcement: min height 2, grounded to floor ===
		int minLength = surface.isWall() ? 2 : 1;
		if (surface.maxHeight() < minLength) return false;

		// For walls: ensure the seam starts from the floor
		BlockPos startPos = surface.solidPos();
		if (surface.isWall()) {
			startPos = groundToFloor(world, startPos, surface.facing());
		}

		// === Snapping Mechanic ===
		int maxLength = 7;
		int snapDist = SurfaceScanner.snapCast(world, startPos, heightDir, maxLength);
		int length;
		if (snapDist > 0 && snapDist <= maxLength) {
			length = Math.max(Math.min(snapDist, surface.maxHeight()), minLength);
		} else {
			length = random.nextBetween(minLength, Math.min(maxLength, surface.maxHeight()));
		}

		if (length < minLength) return false;

		// === Collect overworld blocks without replacing them ===
		Map<BlockPos, BlockState> overworldBlocks = new LinkedHashMap<>();

		for (int w = 0; w < width; w++) {
			for (int h = 0; h < length; h++) {
				BlockPos blockPos = startPos
						.offset(widthDir, w)
						.offset(heightDir, h);

				BlockState original = world.getBlockState(blockPos);
				if (!original.isOpaque()) return false; // Abort if not a full opaque block

				overworldBlocks.put(blockPos, original);
			}
		}

		if (overworldBlocks.isEmpty()) return false;

		// === Spawn face-aligned portal at inner face (-0.001 offset) ===
		Vec3d portalCenter = PortalSpawner.calculateFaceAlignedCenter(
				startPos, widthDir, heightDir, surface.facing(),
				width, length, -0.0001
		);

		Vec3d axisW = PortalSpawner.computePortalAxisW(heightDir, surface.facing());
		Vec3d axisH = PortalSpawner.getPortalAxisH(heightDir);

		int dy = 100 - startPos.getY();
		BlockPos blockOffset = new BlockPos(0, dy, 0);

		// === Place PortalFacadeBlocks in Overworld with crackSide ===
		for (var entry : overworldBlocks.entrySet()) {
			BlockPos origPos = entry.getKey();
			BlockState overworldState = entry.getValue();

			int dx = origPos.getX() - startPos.getX();
			int ddy = origPos.getY() - startPos.getY();
			int dz = origPos.getZ() - startPos.getZ();
			int w = dx * widthDir.getOffsetX() + ddy * widthDir.getOffsetY() + dz * widthDir.getOffsetZ();
			Vec3d crackSide = calculateCrackSide(w, widthDir);

			world.setBlockState(origPos, ModBlocks.PORTAL_FACADE_BLOCK.getDefaultState(), 3);
			BlockEntity be = world.getBlockEntity(origPos);
			if (be instanceof PortalFacadeBlockEntity facade) {
				facade.setFacadeData(overworldState, Blocks.AIR.getDefaultState(), surface.facing(), crackSide);
				facade.setDepthOffset(0.0f);
				facade.setFacadeType(PortalFacadeBlockEntity.FacadeType.SEAM_OVERWORLD);
			}
		}

		Vec3d destination = portalCenter.add(0, dy, 0);

		PortalSpawner.spawnBidirectionalPortal(
				world, portalCenter, axisW, axisH, width, length, destination
		);

		ServerWorld serverWorld = world.toServerWorld();
		final BlockPos finalStartPos = startPos;
		
		serverWorld.getServer().execute(() -> {
			try {
				ServerWorld destWorld = serverWorld.getServer().getWorld(ModDimensions.MISGENERATE_WORLD);
				if (destWorld != null) {
					// === Place PortalFacadeBlocks in misgenerate (Stone fallback) ===
					for (var entry : overworldBlocks.entrySet()) {
						BlockPos origPos = entry.getKey();
						BlockPos mirrorPos = origPos.add(blockOffset);

						BlockState misgenerateState = destWorld.getBlockState(mirrorPos);
						if (misgenerateState.isAir()) {
							misgenerateState = Blocks.STONE.getDefaultState();
						}
						
						int dx = origPos.getX() - finalStartPos.getX();
						int ddy = origPos.getY() - finalStartPos.getY();
						int dz = origPos.getZ() - finalStartPos.getZ();
						int w = dx * widthDir.getOffsetX() + ddy * widthDir.getOffsetY() + dz * widthDir.getOffsetZ();
						Vec3d crackSide = calculateCrackSide(w, widthDir);

						// Restore SEAM_MISGENERATE (flat stone with crack) at mirrorPos
						destWorld.setBlockState(mirrorPos, ModBlocks.PORTAL_FACADE_BLOCK.getDefaultState(), 3);
						BlockEntity be = destWorld.getBlockEntity(mirrorPos);
						if (be instanceof PortalFacadeBlockEntity facade) {
							facade.setFacadeData(Blocks.AIR.getDefaultState(), misgenerateState, surface.facing(), crackSide);
							facade.setDepthOffset(0.002f);
							facade.setFacadeType(PortalFacadeBlockEntity.FacadeType.SEAM_MISGENERATE);
						}

						// Place SOLID_STONE (real block) behind the portal
						BlockPos solidPos = mirrorPos.offset(surface.facing());
						destWorld.setBlockState(solidPos, ModBlocks.PORTAL_FACADE_BLOCK.getDefaultState(), 3);
						BlockEntity solidBe = destWorld.getBlockEntity(solidPos);
						if (solidBe instanceof PortalFacadeBlockEntity solidFacade) {
							solidFacade.setFacadeData(Blocks.AIR.getDefaultState(), misgenerateState, surface.facing(), Vec3d.ZERO);
							solidFacade.setDepthOffset(0.0f);
							solidFacade.setFacadeType(PortalFacadeBlockEntity.FacadeType.SOLID_STONE);
						}

						// Clear air space in front and behind
						for (int i = 0; i <= 3; i++) {
							if (i == 1) continue; // Skip solidPos
							BlockPos airPosFront = mirrorPos.offset(surface.facing(), i);
							if (destWorld.getBlockState(airPosFront).isOpaqueFullCube(destWorld, airPosFront)) destWorld.setBlockState(airPosFront, Blocks.AIR.getDefaultState(), 3);
						}
						for (int i = 1; i <= 3; i++) {
							BlockPos airPosBack = mirrorPos.offset(surface.facing().getOpposite(), i);
							if (destWorld.getBlockState(airPosBack).isOpaqueFullCube(destWorld, airPosBack)) destWorld.setBlockState(airPosBack, Blocks.AIR.getDefaultState(), 3);
						}
					}
				}
			} catch (Exception e) {
				Misgenerate.LOGGER.error("Failed to place misgenerate blocks in server thread", e);
			}
		});

		Misgenerate.LOGGER.debug("Placed Seam Facade: {}x{} at {} (wall: {})",
				width, length, startPos, surface.isWall());

		return true;
	}

	/**
	 * Calculate which side of the crack this block is on.
	 * Returns a unit vector pointing TOWARD the crack (inward).
	 * w=0: crack is on the positive widthDir side → offset = +widthDir
	 * w=1: crack is on the negative widthDir side → offset = -widthDir
	 */
	private static Vec3d calculateCrackSide(int widthIndex, Direction widthDir) {
		double sign = (widthIndex == 0) ? 1.0 : -1.0;
		return new Vec3d(
				widthDir.getOffsetX() * sign,
				widthDir.getOffsetY() * sign,
				widthDir.getOffsetZ() * sign
		);
	}

	/**
	 * Find the floor level by checking the AIR side of the wall.
	 * Returns the wall block position at floor level.
	 */
	private static BlockPos groundToFloor(StructureWorldAccess world, BlockPos wallPos, Direction normal) {
		BlockPos airInFront = wallPos.offset(normal);

		for (int dy = 0; dy <= 20; dy++) {
			BlockPos checkAir = airInFront.down(dy);
			BlockPos checkFloor = checkAir.down();

			try {
				boolean isAirHere = !world.getBlockState(checkAir).isOpaque();
				boolean isSolidBelow = world.getBlockState(checkFloor).isOpaque();

				if (isAirHere && isSolidBelow) {
					int floorY = checkAir.getY();
					BlockPos groundedWall = new BlockPos(wallPos.getX(), floorY, wallPos.getZ());

					if (world.getBlockState(groundedWall).isOpaque()) {
						return groundedWall;
					}
				}
			} catch (Exception e) {
				break;
			}
		}
		return wallPos; // Fallback
	}
}
