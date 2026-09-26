package com.misgenerate.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.feature.FeatureConfig;

/**
 * Configuration for the Anomaly Feature.
 * Controls probability splits and scan parameters.
 */
public record AnomalyFeatureConfig(
		float structureChance,
		float terrainChance,
		int maxScanRadius
) implements FeatureConfig {

	public static final Codec<AnomalyFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Codec.FLOAT.fieldOf("structure_chance").orElse(0.8f)
							.forGetter(AnomalyFeatureConfig::structureChance),
					Codec.FLOAT.fieldOf("terrain_chance").orElse(0.2f)
							.forGetter(AnomalyFeatureConfig::terrainChance),
					Codec.INT.fieldOf("max_scan_radius").orElse(8)
							.forGetter(AnomalyFeatureConfig::maxScanRadius)
			).apply(instance, AnomalyFeatureConfig::new)
	);

	/**
	 * Default configuration: 80% structure, 20% terrain, 8-block scan radius.
	 */
	public static final AnomalyFeatureConfig DEFAULT = new AnomalyFeatureConfig(0.8f, 0.2f, 8);
}
