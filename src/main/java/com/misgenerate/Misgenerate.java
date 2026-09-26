package com.misgenerate;

import com.misgenerate.block.ModBlockEntities;
import com.misgenerate.block.ModBlocks;
import com.misgenerate.dimension.ModDimensions;
import com.misgenerate.worldgen.ModFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Misgenerate implements ModInitializer {
	public static final String MOD_ID = "misgenerate";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModBlocks.register();
		ModBlockEntities.register();
		ModDimensions.register();
		ModFeatures.register();


		LOGGER.info("Misgenerate mod initialized!");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
