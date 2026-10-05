package com.misgenerate.dimension;

import com.misgenerate.Misgenerate;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Custom ChunkGenerator for the Misgenerate dimension.
 */
public class MisgenerateChunkGenerator extends ChunkGenerator {

	public static final MapCodec<MisgenerateChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
		).apply(instance, MisgenerateChunkGenerator::new)
	);

	public static final RegistryKey<Biome> VILLAGE_ZONE = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("misgenerate", "village_zone"));

	public MisgenerateChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
												  StructureAccessor structureAccessor, Chunk chunk) {
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = startX + dx;
				int z = startZ + dz;
				int biomeX = x >> 2;
				int biomeZ = z >> 2;
				int biomeY = -60 >> 2;

				RegistryEntry<Biome> biome = this.biomeSource.getBiome(biomeX, biomeY, biomeZ, noiseConfig.getMultiNoiseSampler());
				
				if (biome.matchesKey(VILLAGE_ZONE)) {
					BlockPos pos1 = new BlockPos(x, -61, z);
					BlockPos pos2 = new BlockPos(x, -60, z);
					chunk.setBlockState(pos1, Blocks.STONE.getDefaultState(), false);
					chunk.setBlockState(pos2, Blocks.GRASS_BLOCK.getDefaultState(), false);
				}
			}
		}

		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
		// No additional surface logic needed for now.
	}

	@Override
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
					  BiomeAccess biomeAccess, StructureAccessor structureAccessor,
					  Chunk chunk, GenerationStep.Carver carverStep) {
		// Pure Void: No carving
	}

	@Override
	public void populateEntities(ChunkRegion region) {
		// No custom mob spawning during generation yet
	}

	@Override
	public int getWorldHeight() {
		return 384;
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		int biomeX = x >> 2;
		int biomeZ = z >> 2;
		int biomeY = -60 >> 2;

		RegistryEntry<Biome> biome = this.biomeSource.getBiome(biomeX, biomeY, biomeZ, noiseConfig.getMultiNoiseSampler());
		if (biome.matchesKey(VILLAGE_ZONE)) {
			return -59;
		}
		return world.getBottomY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		int biomeX = x >> 2;
		int biomeZ = z >> 2;
		int biomeY = -60 >> 2;

		RegistryEntry<Biome> biome = this.biomeSource.getBiome(biomeX, biomeY, biomeZ, noiseConfig.getMultiNoiseSampler());
		
		int height = world.getHeight();
		BlockState[] states = new BlockState[height];
		for (int i = 0; i < height; i++) {
			states[i] = Blocks.AIR.getDefaultState();
		}
		
		if (biome.matchesKey(VILLAGE_ZONE)) {
			int bottomY = world.getBottomY();
			int idxStone = -61 - bottomY;
			int idxGrass = -60 - bottomY;
			if (idxStone >= 0 && idxStone < height) states[idxStone] = Blocks.STONE.getDefaultState();
			if (idxGrass >= 0 && idxGrass < height) states[idxGrass] = Blocks.GRASS_BLOCK.getDefaultState();
		}
		
		return new VerticalBlockSample(world.getBottomY(), states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
		text.add("Misgenerate ChunkGen");
	}

	@Override
	public int getMinimumY() {
		return -64;
	}

	@Override
	public int getSeaLevel() {
		return -64;
	}
}
