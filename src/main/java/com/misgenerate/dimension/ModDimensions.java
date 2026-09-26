package com.misgenerate.dimension;

import com.misgenerate.Misgenerate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;

public class ModDimensions {

	// RegistryKey สำหรับ DimensionType
	public static final RegistryKey<DimensionType> MISGENERATE_DIM_TYPE =
			RegistryKey.of(RegistryKeys.DIMENSION_TYPE, Misgenerate.id("misgenerate"));

	// RegistryKey สำหรับ World (Dimension)
	public static final RegistryKey<World> MISGENERATE_WORLD =
			RegistryKey.of(RegistryKeys.WORLD, Misgenerate.id("misgenerate"));

	// RegistryKeys สำหรับ Biomes
	public static final RegistryKey<Biome> STRONGHOLD_ZONE =
			RegistryKey.of(RegistryKeys.BIOME, Misgenerate.id("stronghold_zone"));

	public static final RegistryKey<Biome> NETHER_ZONE =
			RegistryKey.of(RegistryKeys.BIOME, Misgenerate.id("nether_zone"));

	public static final RegistryKey<Biome> VILLAGE_ZONE =
			RegistryKey.of(RegistryKeys.BIOME, Misgenerate.id("village_zone"));

	/**
	 * เรียกใน ModInitializer เพื่อให้ class ถูก load และ static keys ถูก register
	 */
	public static void register() {
		Misgenerate.LOGGER.info("Registering Misgenerate Dimensions...");

		// Register our custom ChunkGenerator codec so the dimension JSON can reference it
		Registry.register(
				Registries.CHUNK_GENERATOR,
				Misgenerate.id("misgenerate"),
				MisgenerateChunkGenerator.CODEC
		);

		Misgenerate.LOGGER.info("Registered MisgenerateChunkGenerator");
	}
}
