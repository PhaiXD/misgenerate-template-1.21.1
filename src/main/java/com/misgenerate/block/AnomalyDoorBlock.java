package com.misgenerate.block;

import com.misgenerate.portal.PortalSpawner;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.Block;

/**
 * Type 3: The Anomaly Door — looks like a normal door.
 * When opened, a bi-directional portal aligns flat inside the doorway.
 * When closed, the portal despawns.
 *
 * Portal direction uses computePortalAxisW(UP, facing) to ensure
 * the portal faces toward the player (same direction as door facing).
 */
public class AnomalyDoorBlock extends DoorBlock {

	public AnomalyDoorBlock(BlockSetType blockSetType, Settings settings) {
		super(blockSetType, settings);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		// Allow opening ANY anomaly door by hand (including iron)
		state = state.cycle(Properties.OPEN);
		world.setBlockState(pos, state, 10);
		
		// Play vanilla door sounds!
		boolean isOpen = state.get(Properties.OPEN);
		world.playSound(null, pos, isOpen ? net.minecraft.sound.SoundEvents.BLOCK_WOODEN_DOOR_OPEN : net.minecraft.sound.SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, net.minecraft.sound.SoundCategory.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);
		
		world.emitGameEvent(player, isOpen ? net.minecraft.world.event.GameEvent.BLOCK_OPEN : net.minecraft.world.event.GameEvent.BLOCK_CLOSE, pos);
		return ActionResult.success(world.isClient);
	}




}
