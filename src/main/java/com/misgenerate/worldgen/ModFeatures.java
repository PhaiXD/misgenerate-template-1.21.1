package com.misgenerate.worldgen;

import com.misgenerate.Misgenerate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Registers the Anomaly Feature and injects it into world generation
 * via Fabric's BiomeModifications API.
 */
public class ModFeatures {

	// === Feature Registration ===
	public static final Feature<AnomalyFeatureConfig> ANOMALY_FEATURE =
			Registry.register(
					Registries.FEATURE,
					Misgenerate.id("anomaly"),
					new AnomalyFeature()
			);

	// === Placed Feature Key (references the JSON data) ===
	public static final RegistryKey<PlacedFeature> ANOMALY_PLACED =
			RegistryKey.of(RegistryKeys.PLACED_FEATURE, Misgenerate.id("anomaly"));

	/**
	 * Called from ModInitializer to register features and inject into biomes.
	 */
	public static void register() {
		Misgenerate.LOGGER.info("Registering Misgenerate Features...");

		// Inject the anomaly feature into all overworld biomes
		// (and optionally Nether/End — excluding misgenerate itself)
		BiomeModifications.addFeature(
				// Select all biomes except misgenerate's void biome
				BiomeSelectors.foundInOverworld()
						.or(BiomeSelectors.foundInTheNether())
						.or(BiomeSelectors.foundInTheEnd()),
				// Run at TOP_LAYER_MODIFICATION — after all structures and terrain are placed
				GenerationStep.Feature.TOP_LAYER_MODIFICATION,
				ANOMALY_PLACED
		);
	}
}
