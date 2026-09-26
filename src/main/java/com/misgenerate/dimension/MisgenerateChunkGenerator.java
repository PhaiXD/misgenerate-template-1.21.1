package com.misgenerate.dimension;

import com.misgenerate.Misgenerate;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
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
 * Currently generates a pure Void (100% Air).
 */
public class MisgenerateChunkGenerator extends ChunkGenerator {

	public static final MapCodec<MisgenerateChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
		).apply(instance, MisgenerateChunkGenerator::new)
	);

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
		// Pure Void: Do nothing. Chunks are filled with Air by default.
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
		// Pure Void: No surface
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
		return world.getBottomY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		int height = world.getHeight();
		BlockState[] states = new BlockState[height];
		for (int i = 0; i < height; i++) {
			states[i] = Blocks.AIR.getDefaultState();
		}
		return new VerticalBlockSample(world.getBottomY(), states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
		text.add("Misgenerate Void ChunkGen");
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
