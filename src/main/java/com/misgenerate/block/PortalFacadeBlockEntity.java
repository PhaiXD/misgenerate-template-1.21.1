package com.misgenerate.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class PortalFacadeBlockEntity extends BlockEntity {

	public enum FacadeType {
		LEGACY,
		SEAM_OVERWORLD,
		SEAM_MISGENERATE,
		SOLID_STONE
	}

	private BlockState overworldState = Blocks.AIR.getDefaultState();
	private BlockState misgenerateState = Blocks.AIR.getDefaultState();
	private Direction facing = Direction.NORTH;
	private Vec3d crackSide = Vec3d.ZERO;
	private float depthOffset = 0.0f;
	private FacadeType facadeType = FacadeType.LEGACY;

	public PortalFacadeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PORTAL_FACADE, pos, state);
	}

	public BlockState getOverworldState() {
		return overworldState;
	}

	public BlockState getMisgenerateState() {
		return misgenerateState;
	}

	public Direction getFacing() {
		return facing;
	}

	public Vec3d getCrackSide() {
		return crackSide;
	}

	public float getDepthOffset() {
		return depthOffset;
	}

	public FacadeType getFacadeType() {
		return facadeType;
	}

	public void setFacadeData(BlockState overworldState, BlockState misgenerateState, Direction facing, Vec3d crackSide) {
		this.overworldState = overworldState;
		this.misgenerateState = misgenerateState;
		this.facing = facing;
		this.crackSide = crackSide;
		markDirty();
	}

	public void setDepthOffset(float depthOffset) {
		this.depthOffset = depthOffset;
		markDirty();
	}

	public void setFacadeType(FacadeType type) {
		this.facadeType = type;
		markDirty();
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		if (overworldState != null) {
			nbt.put("OverworldState", NbtHelper.fromBlockState(overworldState));
		}
		if (misgenerateState != null) {
			nbt.put("MisgenerateState", NbtHelper.fromBlockState(misgenerateState));
		}
		if (facing != null) {
			nbt.putInt("Facing", facing.getId());
		}
		if (crackSide != null) {
			nbt.putDouble("CrackX", crackSide.x);
			nbt.putDouble("CrackY", crackSide.y);
			nbt.putDouble("CrackZ", crackSide.z);
		}
		nbt.putFloat("DepthOffset", depthOffset);
		nbt.putString("FacadeType", facadeType.name());
	}

	@Override
	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		if (nbt.contains("OverworldState")) {
			this.overworldState = NbtHelper.toBlockState(
					registryLookup.getWrapperOrThrow(RegistryKeys.BLOCK),
					nbt.getCompound("OverworldState")
			);
		}
		if (nbt.contains("MisgenerateState")) {
			this.misgenerateState = NbtHelper.toBlockState(
					registryLookup.getWrapperOrThrow(RegistryKeys.BLOCK),
					nbt.getCompound("MisgenerateState")
			);
		}
		if (nbt.contains("Facing")) {
			this.facing = Direction.byId(nbt.getInt("Facing"));
		}
		if (nbt.contains("CrackX")) {
			this.crackSide = new Vec3d(nbt.getDouble("CrackX"), nbt.getDouble("CrackY"), nbt.getDouble("CrackZ"));
		}
		if (nbt.contains("DepthOffset")) {
			this.depthOffset = nbt.getFloat("DepthOffset");
		}
		if (nbt.contains("FacadeType")) {
			try {
				this.facadeType = FacadeType.valueOf(nbt.getString("FacadeType"));
			} catch (IllegalArgumentException e) {
				this.facadeType = FacadeType.LEGACY;
			}
		}
	}

	@Nullable
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
		NbtCompound nbt = new NbtCompound();
		writeNbt(nbt, registryLookup);
		return nbt;
	}
}
