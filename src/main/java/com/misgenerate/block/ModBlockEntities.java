package com.misgenerate.block;

import com.misgenerate.Misgenerate;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Registers all block entity types for the mod.
 */
public class ModBlockEntities {

	public static final BlockEntityType<PortalFacadeBlockEntity> PORTAL_FACADE =
			Registry.register(
					Registries.BLOCK_ENTITY_TYPE,
					Misgenerate.id("portal_facade"),
					FabricBlockEntityTypeBuilder.create(
							PortalFacadeBlockEntity::new,
							ModBlocks.PORTAL_FACADE_BLOCK
					).build()
			);

	public static void register() {
		Misgenerate.LOGGER.info("Registering Misgenerate Block Entities...");
	}
}
