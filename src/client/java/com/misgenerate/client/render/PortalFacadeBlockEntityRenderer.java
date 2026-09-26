package com.misgenerate.client.render;

import com.misgenerate.block.PortalFacadeBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class PortalFacadeBlockEntityRenderer implements BlockEntityRenderer<PortalFacadeBlockEntity> {

	private final BlockRenderManager blockRenderManager;

	public PortalFacadeBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this.blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
	}

	@Override
	public void render(
			PortalFacadeBlockEntity entity,
			float tickDelta,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light,
			int overlay
	) {
		BlockState overworldState = entity.getOverworldState();
		BlockState misgenerateState = entity.getMisgenerateState();
		Direction facing = entity.getFacing();
		Vec3d crackSide = entity.getCrackSide();

		if ((overworldState == null || overworldState.isAir()) && (misgenerateState == null || misgenerateState.isAir())) {
			return;
		}

		// The facade block is located at Z=0 (solid block coords).
		// The portal is at Z=1.001 facing SOUTH (+Z).
		// Plane 1 (Overworld texture): should be the SOUTH face of the block at Z=0, which sits exactly at Z=1.0.
		// Get camera position to determine which side of the facade we are looking at
		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		Vec3d blockCenter = Vec3d.ofCenter(entity.getPos());
		boolean seeFront = cameraPos.subtract(blockCenter).dotProduct(Vec3d.of(facing.getVector())) >= 0;

		PortalFacadeBlockEntity.FacadeType type = entity.getFacadeType();

		if (overworldState != null && !overworldState.isAir()) {
			if (type == PortalFacadeBlockEntity.FacadeType.SEAM_OVERWORLD || type == PortalFacadeBlockEntity.FacadeType.SOLID_STONE) {
				// Render ALL 6 faces for the Overworld block (or 5 faces for SOLID_STONE)
				for (Direction dir : Direction.values()) {
					// For SOLID_STONE, skip the face touching the portal to prevent Z-fighting and obscuring the portal!
					if (type == PortalFacadeBlockEntity.FacadeType.SOLID_STONE && dir == facing) {
						continue;
					}

					matrices.push();
					if (dir == facing && type == PortalFacadeBlockEntity.FacadeType.SEAM_OVERWORLD) {
						// Apply crack gap scaling ONLY to the top face (for Seam)
						if (crackSide != null && !crackSide.equals(Vec3d.ZERO)) {
							float sx = 1.0f, sy = 1.0f, sz = 1.0f;
							float tx = 0, ty = 0, tz = 0;
							float CRACK_GAP = 0.1f / 16.0f; // Extremely narrow gap

							if (Math.abs(crackSide.x) > 0.5) {
								sx = 1.0f - CRACK_GAP;
								if (crackSide.x < 0) tx = CRACK_GAP;
							}
							if (Math.abs(crackSide.y) > 0.5) {
								sy = 1.0f - CRACK_GAP;
								if (crackSide.y < 0) ty = CRACK_GAP;
							}
							if (Math.abs(crackSide.z) > 0.5) {
								sz = 1.0f - CRACK_GAP;
								if (crackSide.z < 0) tz = CRACK_GAP;
							}
							matrices.translate(tx, ty, tz);
							matrices.scale(sx, sy, sz);
						}

						matrices.translate(
								facing.getOffsetX() * (0.0005f - entity.getDepthOffset()),
								facing.getOffsetY() * (0.0005f - entity.getDepthOffset()),
								facing.getOffsetZ() * (0.0005f - entity.getDepthOffset())
						);
						renderFaceQuad(entity, matrices, vertexConsumers, overworldState, dir, Vec3d.ZERO, light, overlay);
					} else {
						// Other faces are NOT scaled! Full block sides!
						renderFaceQuad(entity, matrices, vertexConsumers, overworldState, dir, Vec3d.ZERO, light, overlay);
					}
					matrices.pop();
				}
			} else {
				// Legacy / Glitch block behavior: render ONLY the facing face (Plane 1)
				if (seeFront) {
					matrices.push();
					matrices.translate(
							facing.getOffsetX() * (0.0005f - entity.getDepthOffset()),
							facing.getOffsetY() * (0.0005f - entity.getDepthOffset()),
							facing.getOffsetZ() * (0.0005f - entity.getDepthOffset())
					);
					renderFaceQuad(entity, matrices, vertexConsumers, overworldState, facing, crackSide, light, overlay);
					matrices.pop();
				}
			}
		}

		if (misgenerateState != null && !misgenerateState.isAir()) {
			if (type == PortalFacadeBlockEntity.FacadeType.SOLID_STONE) {
				// Render ALL 6 faces for the Solid Stone block
				for (Direction dir : Direction.values()) {
					matrices.push();
					renderFaceQuad(entity, matrices, vertexConsumers, misgenerateState, dir, Vec3d.ZERO, light, overlay);
					matrices.pop();
				}
			} else if (type == PortalFacadeBlockEntity.FacadeType.SEAM_MISGENERATE) {
				// Flat plane facing misgenerate with the exact same crack as Overworld.
				matrices.push();
				
				// Move Plane 2 to perfectly sit just IN FRONT of the portal (offset = 0.998)
				// The return portal is at 0.999. If it was at 1.0, the portal would clip it out of view!
				float offset = 0.998f;
				matrices.translate(
						facing.getOffsetX() * offset,
						facing.getOffsetY() * offset,
						facing.getOffsetZ() * offset
				);
				
				// Apply same crack scaling to the Misgenerate stone plane
				if (crackSide != null && !crackSide.equals(Vec3d.ZERO)) {
					float sx = 1.0f, sy = 1.0f, sz = 1.0f;
					float tx = 0, ty = 0, tz = 0;
					float CRACK_GAP = 0.1f / 16.0f;

					if (Math.abs(crackSide.x) > 0.5) { sx = 1.0f - CRACK_GAP; if (crackSide.x < 0) tx = CRACK_GAP; }
					if (Math.abs(crackSide.y) > 0.5) { sy = 1.0f - CRACK_GAP; if (crackSide.y < 0) ty = CRACK_GAP; }
					if (Math.abs(crackSide.z) > 0.5) { sz = 1.0f - CRACK_GAP; if (crackSide.z < 0) tz = CRACK_GAP; }
					matrices.translate(tx, ty, tz);
					matrices.scale(sx, sy, sz);
				}

				// Render both faces of the flat plane
				renderFaceQuad(entity, matrices, vertexConsumers, misgenerateState, facing.getOpposite(), Vec3d.ZERO, light, overlay);
				renderFaceQuad(entity, matrices, vertexConsumers, misgenerateState, facing, Vec3d.ZERO, light, overlay);
				
				matrices.pop();
			} else {
				// Legacy / Glitch block behavior: render Plane 2 (Misgenerate texture) only if viewed from the back
				if (!seeFront) {
					matrices.push();
					
					// Move Plane 2 to perfectly align with the portal boundary (offset = 1.0)
					float offset = 1.0f;
					matrices.translate(
							facing.getOffsetX() * offset,
							facing.getOffsetY() * offset,
							facing.getOffsetZ() * offset
					);
					
					renderFaceQuad(entity, matrices, vertexConsumers, misgenerateState, facing.getOpposite(), crackSide, light, overlay);
					
					matrices.pop();
				}
			}
		}
	}

	private void renderFaceQuad(
			PortalFacadeBlockEntity entity,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			BlockState state,
			Direction face,
			Vec3d crackSide,
			int light,
			int overlay
	) {
		BakedModel model = blockRenderManager.getModel(state);
		long seed = state.getRenderingSeed(entity.getPos());
		List<BakedQuad> quads = model.getQuads(state, face, Random.create(seed));

		if (quads.isEmpty()) return;

		matrices.push();

		// Apply crack gap scaling if this is a Glitch anomaly
		if (crackSide != null && !crackSide.equals(Vec3d.ZERO)) {
			float sx = 1.0f, sy = 1.0f, sz = 1.0f;
			float tx = 0, ty = 0, tz = 0;
			float CRACK_GAP = 0.2f / 16.0f; // Narrower gap for Glitch blocks as well

			if (Math.abs(crackSide.x) > 0.5) {
				sx = 1.0f - CRACK_GAP;
				if (crackSide.x < 0) tx = CRACK_GAP;
			}
			if (Math.abs(crackSide.y) > 0.5) {
				sy = 1.0f - CRACK_GAP;
				if (crackSide.y < 0) ty = CRACK_GAP;
			}
			if (Math.abs(crackSide.z) > 0.5) {
				sz = 1.0f - CRACK_GAP;
				if (crackSide.z < 0) tz = CRACK_GAP;
			}
			matrices.translate(tx, ty, tz);
			matrices.scale(sx, sy, sz);
		}

		RenderLayer layer = RenderLayers.getBlockLayer(state);
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);

		// Manually draw the quads to avoid scaling the entire block state and destroying normals
		int quadIndex = 0;
		for (BakedQuad quad : quads) {
			matrices.push();
			
			// Prevent coplanar z-fighting within the same block (e.g. Grass block dirt base + grass overlay)
			if (quadIndex > 0) {
				matrices.translate(face.getOffsetX() * 0.001f, face.getOffsetY() * 0.001f, face.getOffsetZ() * 0.001f);
			}

			int color = -1;
			if (quad.hasColor()) {
				// Use the client's current world (usually Overworld when viewing Plane 1) to get the correct biome tint.
				// Since portal X, Z coordinates match perfectly, the biome lookup at entity.getPos() will be correct!
				color = MinecraftClient.getInstance().getBlockColors().getColor(state, MinecraftClient.getInstance().world, entity.getPos(), quad.getColorIndex());
			}

			float r = 1.0f;
			float g = 1.0f;
			float b = 1.0f;

			if (color != -1) {
				r = (color >> 16 & 0xFF) / 255.0f;
				g = (color >> 8 & 0xFF) / 255.0f;
				b = (color & 0xFF) / 255.0f;
			}

			vertexConsumer.quad(matrices.peek(), quad, r, g, b, 1.0f, light, overlay);
			
			matrices.pop();
			quadIndex++;
		}

		matrices.pop();
	}
}
