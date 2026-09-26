package com.misgenerate.worldgen.anomaly;

/**
 * Defines the three anomaly types with their dimension constraints.
 */
public enum AnomalyType {

	/**
	 * Type 1: The Seam — ghost wall/floor with noclip.
	 * Width: ALWAYS 2 blocks, Height/Length: 1-7 blocks.
	 * Snaps to ceiling/wall if one is detected within range.
	 */
	SEAM(2, 2, 1, 7),

	/**
	 * Type 2: Full Glitch Portal — visible corruption with lighting issues.
	 * Width: 1-3 blocks, Height: 1-3 blocks.
	 */
	GLITCH_PORTAL(1, 3, 1, 3),

	/**
	 * Type 3: The Anomaly Door — normal-looking door that spawns a portal on use.
	 * Always 1x2 (standard door size).
	 */
	ANOMALY_DOOR(1, 1, 2, 2);

	public final int minWidth;
	public final int maxWidth;
	public final int minHeight;
	public final int maxHeight;

	AnomalyType(int minWidth, int maxWidth, int minHeight, int maxHeight) {
		this.minWidth = minWidth;
		this.maxWidth = maxWidth;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
	}

	/**
	 * Check if the given surface dimensions can fit this anomaly type.
	 */
	public boolean canFit(int availableWidth, int availableHeight) {
		return availableWidth >= minWidth && availableHeight >= minHeight;
	}
}
