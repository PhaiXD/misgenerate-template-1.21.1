package com.misgenerate.client;

import com.misgenerate.block.ModBlockEntities;
import com.misgenerate.client.render.PortalFacadeBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class MisgenerateClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register the PortalFacadeBlockEntityRenderer
		BlockEntityRendererFactories.register(
				ModBlockEntities.PORTAL_FACADE,
				PortalFacadeBlockEntityRenderer::new
		);

		// Register render layers for translucent block textures (like doors)
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(com.misgenerate.block.ModBlocks.IRON_ANOMALY_DOOR, net.minecraft.client.render.RenderLayer.getCutout());
		for (net.minecraft.block.Block door : com.misgenerate.block.ModBlocks.WOODEN_ANOMALY_DOORS) {
			net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(door, net.minecraft.client.render.RenderLayer.getCutout());
		}

		// Torches
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(com.misgenerate.block.ModBlocks.UPRIGHT_TORCH, net.minecraft.client.render.RenderLayer.getCutout());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(com.misgenerate.block.ModBlocks.TILTED_TORCH, net.minecraft.client.render.RenderLayer.getCutout());
	}
}