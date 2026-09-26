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
 * Type 2: Glitch Portal — The Paper-Thin Facade Approach.
 *
 * Key behaviors:
 * - Does NOT remove the Overworld block (keeps original solid wall).
 * - Spawns a face-aligned Immersive Portal 0.001 blocks in front of the wall.
 * - In the misgenerate world, replaces the exit blocks with PortalFacadeBlock.
 * - PortalFacadeBlock renders the Overworld texture and Misgenerate texture as paper-thin planes.
 */
public class GlitchPortalPlacer {

	public static boolean place(StructureWorldAccess world, SurfaceResult surface, Random random) {
		Direction widthDir = SurfaceScanner.getWidthDirection(surface);
		Direction heightDir = SurfaceScanner.getHeightDirection(surface);

		// === Wall enforcement: min height 2 ===
		int minHeight = surface.isWall() ? 2 : 1;
		if (surface.maxHeight() < minHeight) return false;

		int width = random.nextBetween(1, Math.min(3, surface.maxWidth()));
		int height = random.nextBetween(minHeight, Math.min(3, surface.maxHeight()));

		if (width < 1 || height < minHeight) return false;

		// For walls: ensure the glitch starts from the floor
		BlockPos startPos = surface.solidPos();
		if (surface.isWall()) {
			startPos = groundToFloor(world, startPos, surface.facing());
		}

		// === Collect overworld blocks without replacing them YET ===
		Map<BlockPos, BlockState> overworldBlocks = new LinkedHashMap<>();

		for (int w = 0; w < width; w++) {
			for (int h = 0; h < height; h++) {
				BlockPos blockPos = startPos
						.offset(widthDir, w)
						.offset(heightDir, h);

				BlockState original = world.getBlockState(blockPos);
				if (!original.isOpaque()) return false; // Abort if not a full opaque block

				overworldBlocks.put(blockPos, original);
			}
		}

		if (overworldBlocks.isEmpty()) return false;

		// === Spawn face-aligned portal at inner face (-0.0001 offset) ===
		Vec3d portalCenter = PortalSpawner.calculateFaceAlignedCenter(
				startPos, widthDir, heightDir, surface.facing(),
				width, height, -0.0001
		);

		Vec3d axisW = PortalSpawner.computePortalAxisW(heightDir, surface.facing());
		Vec3d axisH = PortalSpawner.getPortalAxisH(heightDir);

		int dy = 100 - startPos.getY();
		int destStartX = startPos.getX() * 7;
		int destStartZ = startPos.getZ() * 7;
		BlockPos destStartPos = new BlockPos(destStartX, startPos.getY() + dy, destStartZ);

		// === Replace Overworld blocks with PortalFacadeBlock (Glitch has no cracks) ===
		for (var entry : overworldBlocks.entrySet()) {
			BlockPos origPos = entry.getKey();
			BlockState overworldState = entry.getValue();

			world.setBlockState(origPos, ModBlocks.PORTAL_FACADE_BLOCK.getDefaultState(), 3);
			BlockEntity be = world.getBlockEntity(origPos);
			if (be instanceof PortalFacadeBlockEntity facade) {
				facade.setFacadeData(overworldState, Blocks.AIR.getDefaultState(), surface.facing(), Vec3d.ZERO);
				facade.setDepthOffset(0.0f);
				facade.setFacadeType(PortalFacadeBlockEntity.FacadeType.SEAM_OVERWORLD);
			}
		}

		Vec3d destination = new Vec3d(
				destStartX + (portalCenter.x - startPos.getX()),
				portalCenter.y + dy,
				destStartZ + (portalCenter.z - startPos.getZ())
		);

		PortalSpawner.spawnBidirectionalPortal(
				world, portalCenter, axisW, axisH, width, height, destination
		);

		final BlockPos finalStartPos = startPos;

		// === Place PortalFacadeBlocks at destination in misgenerate ===
		ServerWorld serverWorld = world.toServerWorld();
		serverWorld.getServer().execute(() -> {
			try {
				ServerWorld destWorld = serverWorld.getServer().getWorld(ModDimensions.MISGENERATE_WORLD);
				if (destWorld != null) {
					// === Place PortalFacadeBlocks in Misgenerate (Stone fallback) ===
					for (var entry : overworldBlocks.entrySet()) {
						BlockPos origPos = entry.getKey();
						BlockPos mirrorPos = destStartPos.add(origPos.subtract(finalStartPos));

						BlockState misgenerateState = destWorld.getBlockState(mirrorPos);
						if (misgenerateState.isAir()) {
							misgenerateState = Blocks.STONE.getDefaultState();
						}

						// Restore SEAM_MISGENERATE (flat stone) at mirrorPos
						destWorld.setBlockState(mirrorPos, ModBlocks.PORTAL_FACADE_BLOCK.getDefaultState(), 3);
						BlockEntity be = destWorld.getBlockEntity(mirrorPos);
						if (be instanceof PortalFacadeBlockEntity facade) {
							facade.setFacadeData(Blocks.AIR.getDefaultState(), misgenerateState, surface.facing(), Vec3d.ZERO);
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
				Misgenerate.LOGGER.error("Failed to place glitch misgenerate blocks in server thread", e);
			}
		});

		return true;
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
