package com.misgenerate.worldgen.scanner;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that scans the world around a target position
 * to find suitable flat surfaces (walls, floors) for anomaly placement.
 *
 * IMPORTANT: solidPos in SurfaceResult is the SOLID block to be REPLACED,
 * not the air block adjacent to it.
 */
public class SurfaceScanner {

	/**
	 * Result of a surface scan — describes an available flat area.
	 */
	public record SurfaceResult(
			BlockPos solidPos,      // Position of the solid surface block (TO BE REPLACED)
			Direction facing,       // Normal direction from solid → air
			int maxWidth,           // Max width of contiguous solid surface
			int maxHeight,          // Max height/length of contiguous solid surface
			boolean isWall          // true = wall, false = floor
	) {}

	/**
	 * Find all suitable surfaces near the given center position.
	 * Includes chunk boundary safety checks.
	 */
	public static List<SurfaceResult> findSuitableSurfaces(
			StructureWorldAccess world, BlockPos center, int radius
	) {
		List<SurfaceResult> results = new ArrayList<>();

		// Clamp radius to prevent cross-chunk access during worldgen
		int safeRadius = Math.min(radius, 4);

		for (int dx = -safeRadius; dx <= safeRadius; dx++) {
			for (int dy = -safeRadius; dy <= safeRadius; dy++) {
				for (int dz = -safeRadius; dz <= safeRadius; dz++) {
					BlockPos pos = center.add(dx, dy, dz);

					// Safety: skip unloaded chunks
					if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;

					BlockState state;
					try {
						state = world.getBlockState(pos);
					} catch (Exception e) {
						continue; // Skip inaccessible positions
					}

					if (!isSolid(world, pos)) continue;

					// Check each face for air adjacency → this is a surface block
					for (Direction dir : Direction.values()) {
						BlockPos adjacent = pos.offset(dir);
						try {
							if (isAir(world, adjacent)) {
								SurfaceResult result = measureSurface(world, pos, dir);
								if (result != null && result.maxWidth >= 1 && result.maxHeight >= 1) {
									results.add(result);
								}
							}
						} catch (Exception e) {
							// Skip positions near chunk boundaries
						}
					}
				}
			}
		}

		results.sort((a, b) -> Integer.compare(
				b.maxWidth * b.maxHeight,
				a.maxWidth * a.maxHeight
		));

		return results;
	}

	/**
	 * Measure the flat area available starting from a surface block.
	 * Returns the SOLID block position (the block to be replaced).
	 */
	public static SurfaceResult measureSurface(
			StructureWorldAccess world, BlockPos solidPos, Direction normal
	) {
		boolean isFloor = normal == Direction.UP;
		boolean isWall = normal.getAxis() != Direction.Axis.Y;

		if (!isFloor && !isWall) return null; // Skip ceilings

		Direction widthDir;
		Direction heightDir;

		if (isFloor) {
			widthDir = Direction.EAST;
			heightDir = Direction.SOUTH;
		} else {
			widthDir = getPerpendicularHorizontal(normal);
			heightDir = Direction.UP;
		}

		// Measure width along the surface
		int maxWidth = 1;
		for (int w = 1; w <= 3; w++) {
			BlockPos checkSolid = solidPos.offset(widthDir, w);
			BlockPos checkAir = checkSolid.offset(normal);
			try {
				if (isSolid(world, checkSolid) &&
						isAir(world, checkAir)) {
					maxWidth = w + 1;
				} else {
					break;
				}
			} catch (Exception e) {
				break;
			}
		}

		// Measure height along the surface
		int maxHeight = 1;
		for (int h = 1; h <= 7; h++) {
			BlockPos checkSolid = solidPos.offset(heightDir, h);
			BlockPos checkAir = checkSolid.offset(normal);
			try {
				if (isSolid(world, checkSolid) &&
						isAir(world, checkAir)) {
					maxHeight = h + 1;
				} else {
					break;
				}
			} catch (Exception e) {
				break;
			}
		}

		// Return the SOLID block position (to be replaced)
		return new SurfaceResult(solidPos, normal, maxWidth, maxHeight, isWall);
	}

	/**
	 * Cast a ray to find solid blocks within range (for Seam snapping).
	 */
	public static int snapCast(StructureWorldAccess world, BlockPos start, Direction direction, int maxDist) {
		for (int i = 1; i <= maxDist; i++) {
			BlockPos check = start.offset(direction, i);
			try {
				if (isSolid(world, check)) {
					return i;
				}
			} catch (Exception e) {
				return -1;
			}
		}
		return -1;
	}

	/**
	 * Get the horizontal direction perpendicular to the given direction.
	 */
	public static Direction getPerpendicularHorizontal(Direction dir) {
		return switch (dir) {
			case NORTH, SOUTH -> Direction.EAST;
			case EAST, WEST -> Direction.SOUTH;
			default -> Direction.EAST;
		};
	}

	/**
	 * Get the width direction for a given surface facing.
	 */
	public static Direction getWidthDirection(SurfaceResult surface) {
		if (surface.isWall()) {
			return getPerpendicularHorizontal(surface.facing());
		} else {
			return Direction.EAST;
		}
	}

	/**
	 * Get the height direction for a given surface facing.
	 */
	public static Direction getHeightDirection(SurfaceResult surface) {
		return surface.isWall() ? Direction.UP : Direction.SOUTH;
	}

	private static boolean isSolid(BlockView world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isOpaqueFullCube(world, pos);
	}

	private static boolean isAir(BlockView world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isAir() || (!state.isOpaqueFullCube(world, pos) && state.getFluidState().isEmpty());
	}
}
