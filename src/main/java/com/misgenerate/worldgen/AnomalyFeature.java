package com.misgenerate.worldgen;

import com.misgenerate.Misgenerate;
import com.misgenerate.worldgen.anomaly.*;
import com.misgenerate.worldgen.scanner.SurfaceScanner;
import com.misgenerate.worldgen.scanner.SurfaceScanner.SurfaceResult;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.structure.MineshaftStructure;
import net.minecraft.world.gen.structure.StrongholdStructure;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Feature for the Anomaly Injection System.
 *
 * Logic flow:
 * 1. Roll 80/20 → structure or terrain targeting
 * 2. If structure: detect type (village/mineshaft/stronghold/etc.)
 * 3. Find suitable surfaces with wall preference for structures
 * 4. Select anomaly type (doors ONLY in mineshaft/stronghold)
 * 5. Delegate to the appropriate placer
 */
public class AnomalyFeature extends Feature<AnomalyFeatureConfig> {

	private static final double MINESHAFT_DOOR_PROBABILITY = 1.0; // 100% chance for doors in mineshafts

	public AnomalyFeature() {
		super(AnomalyFeatureConfig.CODEC);
	}

	/**
	 * Result of structure target search — includes position and structure type info.
	 */
	private record StructureTarget(BlockPos pos, boolean isMineshaft, boolean isStronghold) {}

	@Override
	public boolean generate(FeatureContext<AnomalyFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();
		AnomalyFeatureConfig config = context.getConfig();

		// === Phase 1: Target Selection ===
		float roll = random.nextFloat();
		boolean targetStructure = roll <= config.structureChance();

		BlockPos targetCenter;
		boolean isMineshaft = false;
		boolean isStronghold = false;

		if (targetStructure) {
			StructureTarget structTarget = findStructureTarget(world, origin, random);
			if (structTarget != null) {
				targetCenter = structTarget.pos;
				isMineshaft = structTarget.isMineshaft;
				isStronghold = structTarget.isStronghold;
			} else {
				targetCenter = findTerrainTarget(world, origin, random);
			}
		} else {
			targetCenter = findTerrainTarget(world, origin, random);
		}

		if (targetCenter == null) {
			return false;
		}

		// === Phase 2: Find surfaces ===
		int scanRadius = targetStructure ? 6 : config.maxScanRadius();
		List<SurfaceResult> surfaces = SurfaceScanner.findSuitableSurfaces(
				world, targetCenter, scanRadius
		);

		if (surfaces.isEmpty()) {
			return false;
		}

		// === Phase 3: Surface selection with wall preference ===
		SurfaceResult surface = selectSurface(surfaces, random, targetStructure);

		// === Phase 4: Type selection ===
		List<AnomalyType> candidates = new ArrayList<>();

		// Seam and Glitch are available everywhere
		if (AnomalyType.SEAM.canFit(surface.maxWidth(), surface.maxHeight())) {
			candidates.add(AnomalyType.SEAM);
		}
		if (AnomalyType.GLITCH_PORTAL.canFit(surface.maxWidth(), surface.maxHeight())) {
			candidates.add(AnomalyType.GLITCH_PORTAL);
		}

		// Doors ONLY in mineshaft
		if (isMineshaft && surface.isWall() &&
				AnomalyType.ANOMALY_DOOR.canFit(surface.maxWidth(), surface.maxHeight())) {
			if (random.nextDouble() < MINESHAFT_DOOR_PROBABILITY) {
				candidates.clear(); // Ensure we ONLY spawn doors in Mineshafts to make them much easier to find
				candidates.add(AnomalyType.ANOMALY_DOOR);
			}
		}

		if (candidates.isEmpty()) {
			return false;
		}

		AnomalyType selectedType = candidates.get(random.nextInt(candidates.size()));

		// === Phase 5: Place the Anomaly ===
		boolean success = switch (selectedType) {
			case SEAM -> SeamPlacer.place(world, surface, random);
			case GLITCH_PORTAL -> GlitchPortalPlacer.place(world, surface, random);
			case ANOMALY_DOOR -> AnomalyDoorPlacer.place(world, surface, random, isMineshaft);
		};

		if (success) {
			Misgenerate.LOGGER.debug("Generated {} at {} (struct: mineshaft={}, stronghold={})",
					selectedType.name(), targetCenter, isMineshaft, isStronghold);
		}

		return success;
	}

	/**
	 * Select a surface from the list, preferring walls when in structure context.
	 */
	private SurfaceResult selectSurface(List<SurfaceResult> surfaces, Random random, boolean preferWalls) {
		if (!preferWalls) {
			return surfaces.get(0); // Largest area first
		}

		// Separate walls and floors
		List<SurfaceResult> walls = new ArrayList<>();
		List<SurfaceResult> floors = new ArrayList<>();
		for (SurfaceResult s : surfaces) {
			if (s.isWall()) walls.add(s);
			else floors.add(s);
		}

		// 70% wall preference when in structure context
		if (!walls.isEmpty() && (floors.isEmpty() || random.nextFloat() < 0.7f)) {
			return walls.get(random.nextInt(walls.size()));
		}
		if (!floors.isEmpty()) {
			return floors.get(0);
		}
		return surfaces.get(0);
	}

	/**
	 * Find a target inside a generated structure, detecting the structure type.
	 * Searches for actual solid-air boundaries instead of random bounding box points.
	 */
	private StructureTarget findStructureTarget(StructureWorldAccess world, BlockPos origin, Random random) {
		try {
			ChunkPos chunkPos = new ChunkPos(origin);
			var structureAccessor = world.toServerWorld().getStructureAccessor();

			List<StructureStart> starts = structureAccessor.getStructureStarts(chunkPos, s -> true);
			if (starts == null || starts.isEmpty()) return null;

			// Filter to valid starts
			List<StructureStart> validStarts = new ArrayList<>();
			for (StructureStart start : starts) {
				if (start.hasChildren()) {
					validStarts.add(start);
				}
			}
			if (validStarts.isEmpty()) return null;

			StructureStart chosen = validStarts.get(random.nextInt(validStarts.size()));
			Structure structure = chosen.getStructure();

			// Detect structure type
			boolean isMineshaft = structure instanceof MineshaftStructure;
			boolean isStronghold = structure instanceof StrongholdStructure;

			// Constrain to safe chunk area
			var bb = chosen.getBoundingBox();
			int minX = Math.max(bb.getMinX(), origin.getX() - 8);
			int maxX = Math.min(bb.getMaxX(), origin.getX() + 8);
			int minZ = Math.max(bb.getMinZ(), origin.getZ() - 8);
			int maxZ = Math.min(bb.getMaxZ(), origin.getZ() + 8);

			if (minX > maxX || minZ > maxZ) return null;

			// Search for an actual solid-air boundary (not a random point)
			for (int attempt = 0; attempt < 16; attempt++) {
				int x = minX < maxX ? random.nextBetween(minX, maxX) : minX;
				int z = minZ < maxZ ? random.nextBetween(minZ, maxZ) : minZ;

				// Search downward from top of BB for a surface
				int topY = bb.getMaxY();
				int botY = bb.getMinY();

				for (int y = topY; y >= botY; y--) {
					BlockPos checkPos = new BlockPos(x, y, z);
					try {
						if (world.getBlockState(checkPos).isOpaqueFullCube(world, checkPos) &&
								world.getBlockState(checkPos.up()).isAir()) {
							return new StructureTarget(checkPos, isMineshaft, isStronghold);
						}
					} catch (Exception e) {
						break;
					}
				}
			}

			return null;
		} catch (Exception e) {
			Misgenerate.LOGGER.debug("Failed to find structure target at {}: {}", origin, e.getMessage());
			return null;
		}
	}

	/**
	 * Find a terrain target. 50% surface, 50% underground (for caves/walls).
	 * Returns the SOLID block position (not the air above).
	 */
	private BlockPos findTerrainTarget(StructureWorldAccess world, BlockPos origin, Random random) {
		int offsetX = random.nextBetween(-8, 8);
		int offsetZ = random.nextBetween(-8, 8);
		BlockPos searchPos = origin.add(offsetX, 0, offsetZ);

		boolean searchUnderground = random.nextBoolean();

		if (searchUnderground) {
			// Search for caves at a random lower Y
			int surfaceY = searchPos.getY();
			if (surfaceY > world.getBottomY() + 20) {
				int targetY = random.nextBetween(world.getBottomY() + 10, surfaceY - 5);
				for (int dy = 0; dy < 15; dy++) {
					BlockPos checkPos = new BlockPos(searchPos.getX(), targetY + dy, searchPos.getZ());
					try {
						if (world.getBlockState(checkPos).isOpaqueFullCube(world, checkPos) &&
								world.getBlockState(checkPos.up()).isAir()) {
							return checkPos; // Found a cave surface
						}
					} catch (Exception e) {
						break;
					}
				}
			}
		}

		// Standard surface search (downward from world surface)
		for (int y = searchPos.getY(); y > world.getBottomY() + 5; y--) {
			BlockPos checkPos = new BlockPos(searchPos.getX(), y, searchPos.getZ());
			try {
				if (world.getBlockState(checkPos).isOpaqueFullCube(world, checkPos) &&
						world.getBlockState(checkPos.up()).isAir()) {
					return checkPos; // Return SOLID block position
				}
			} catch (Exception e) {
				break;
			}
		}

		return null;
	}
}
