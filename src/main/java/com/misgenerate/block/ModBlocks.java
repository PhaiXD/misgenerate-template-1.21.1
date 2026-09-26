package com.misgenerate.block;

import com.misgenerate.Misgenerate;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

	// ===== Type 2.5: Portal Facade Block for Paper-Thin Illusion =====
	public static final Block PORTAL_FACADE_BLOCK = registerBlock("portal_facade_block",
			new PortalFacadeBlock(AbstractBlock.Settings.create()
					.nonOpaque()
					.solidBlock((state, world, pos) -> false)
					.suffocates((state, world, pos) -> false)
					.blockVision((state, world, pos) -> false)
					.strength(1.5F, 6.0F)
					.dropsNothing()
					.pistonBehavior(PistonBehavior.BLOCK)));

	// ===== Type 3: Anomaly Doors (Wood Variants) =====
	public static final Block OAK_ANOMALY_DOOR = registerAnomalyDoor("oak_anomaly_door", BlockSetType.OAK, MapColor.OAK_TAN);
	public static final Block SPRUCE_ANOMALY_DOOR = registerAnomalyDoor("spruce_anomaly_door", BlockSetType.SPRUCE, MapColor.SPRUCE_BROWN);
	public static final Block BIRCH_ANOMALY_DOOR = registerAnomalyDoor("birch_anomaly_door", BlockSetType.BIRCH, MapColor.PALE_YELLOW);
	public static final Block JUNGLE_ANOMALY_DOOR = registerAnomalyDoor("jungle_anomaly_door", BlockSetType.JUNGLE, MapColor.DIRT_BROWN);
	public static final Block ACACIA_ANOMALY_DOOR = registerAnomalyDoor("acacia_anomaly_door", BlockSetType.ACACIA, MapColor.ORANGE);
	public static final Block DARK_OAK_ANOMALY_DOOR = registerAnomalyDoor("dark_oak_anomaly_door", BlockSetType.DARK_OAK, MapColor.BROWN);
	public static final Block IRON_ANOMALY_DOOR = registerBlock("iron_anomaly_door", new AnomalyDoorBlock(
			BlockSetType.IRON, AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).instrument(NoteBlockInstrument.BASEDRUM).strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL).pistonBehavior(PistonBehavior.DESTROY).nonOpaque()
	));

	public static final Block[] WOODEN_ANOMALY_DOORS = {
			OAK_ANOMALY_DOOR, SPRUCE_ANOMALY_DOOR, BIRCH_ANOMALY_DOOR,
			JUNGLE_ANOMALY_DOOR, ACACIA_ANOMALY_DOOR, DARK_OAK_ANOMALY_DOOR
	};

	private static Block registerAnomalyDoor(String name, BlockSetType type, MapColor mapColor) {
		return registerBlock(name, new AnomalyDoorBlock(type, AbstractBlock.Settings.create()
				.mapColor(mapColor)
				.instrument(NoteBlockInstrument.BASS)
				.strength(-1.0f, 3600000.0f) // Unbreakable, must destroy anchor blocks
				.sounds(BlockSoundGroup.WOOD)
				.pistonBehavior(PistonBehavior.DESTROY)
				.nonOpaque()
		));
	}

	// ===== Backrooms Level 0 Blocks =====
	// Legacy theme (light_gray/gray/black wool equivalents)
	public static final Block BACKROOMS_LEGACY_WALL = registerSimpleBlock("backrooms_legacy_wall");
	public static final Block BACKROOMS_LEGACY_FLOOR = registerSimpleBlock("backrooms_legacy_floor");
	public static final Block BACKROOMS_LEGACY_EXTRA_SCRATCHED = registerSimpleBlock("backrooms_legacy_extra_scratched");
	// Standard theme (brown/red/orange wool equivalents)
	public static final Block BACKROOMS_WALL = registerSimpleBlock("backrooms_wall");
	public static final Block BACKROOMS_EXTRA_SCRATCHED = registerSimpleBlock("backrooms_extra_scratched");
	public static final Block BACKROOMS_FLOOR = registerSimpleBlock("backrooms_floor");
	public static final Block BACKROOMS_SCRATCHED = registerSimpleBlock("backrooms_scratched");
	// Alt theme (lime/green/blue/purple/cyan wool equivalents)
	public static final Block BACKROOMS_ALT_FLOOR = registerSimpleBlock("backrooms_alt_floor");
	public static final Block BACKROOMS_ALT_EXTRA_SCRATCHED = registerSimpleBlock("backrooms_alt_extra_scratched");
	public static final Block BACKROOMS_LEGACY_SCRATCHED = registerSimpleBlock("backrooms_legacy_scratched");
	public static final Block BACKROOMS_ALT_WALL = registerSimpleBlock("backrooms_alt_wall");
	public static final Block BACKROOMS_ALT_SCRATCHED = registerSimpleBlock("backrooms_alt_scratched");

	public static final Block[] ALL_BACKROOMS_BLOCKS = {
		BACKROOMS_LEGACY_WALL, BACKROOMS_LEGACY_FLOOR, BACKROOMS_LEGACY_EXTRA_SCRATCHED,
		BACKROOMS_WALL, BACKROOMS_EXTRA_SCRATCHED, BACKROOMS_FLOOR, BACKROOMS_SCRATCHED,
		BACKROOMS_ALT_FLOOR, BACKROOMS_ALT_EXTRA_SCRATCHED, BACKROOMS_LEGACY_SCRATCHED,
		BACKROOMS_ALT_WALL, BACKROOMS_ALT_SCRATCHED
	};

	// ===== Custom Torches =====
	public static final Block UPRIGHT_TORCH = registerBlock("upright_torch", new UprightTorchBlock(AbstractBlock.Settings.create()
			.noCollision()
			.breakInstantly()
			.luminance(state -> 14)
			.sounds(BlockSoundGroup.WOOD)
			.pistonBehavior(PistonBehavior.DESTROY)));
			
	public static final Block TILTED_TORCH = registerBlock("tilted_torch", new TiltedTorchBlock(AbstractBlock.Settings.create()
			.noCollision()
			.breakInstantly()
			.luminance(state -> 14)
			.sounds(BlockSoundGroup.WOOD)
			.pistonBehavior(PistonBehavior.DESTROY)));

	// ===== Registration helpers =====

	private static Block registerSimpleBlock(String name) {
		return registerBlock(name, new Block(AbstractBlock.Settings.create()
				.mapColor(MapColor.YELLOW)
				.strength(1.5F, 6.0F)
				.sounds(BlockSoundGroup.WOOL)));
	}

	private static Block registerBlock(String name, Block block) {
		Identifier id = Misgenerate.id(name);
		// Register the block item
		Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
		// Register the block
		return Registry.register(Registries.BLOCK, id, block);
	}

	/**
	 * Called from ModInitializer to force class loading and static registration.
	 */
	public static void register() {
		Misgenerate.LOGGER.info("Registering Misgenerate Blocks...");

		// Add to creative tab for debugging
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
			entries.add(PORTAL_FACADE_BLOCK);
			for (Block door : WOODEN_ANOMALY_DOORS) {
				entries.add(door);
			}
			entries.add(IRON_ANOMALY_DOOR);
			for (Block block : ALL_BACKROOMS_BLOCKS) {
				entries.add(block);
			}
			entries.add(UPRIGHT_TORCH);
			entries.add(TILTED_TORCH);
		});
	}
}
