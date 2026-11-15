package com.exetacticals.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GunWorkbenchBlockEntity extends BlockEntity {
    public static final double OUTLINE_DISTANCE = 8.0;
    private ItemStack crafted = ItemStack.EMPTY;

    public GunWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModWorkbench.BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack crafted() {
        return crafted;
    }

    public boolean isOccupied() {
        return !crafted.isEmpty();
    }

    public void setCrafted(ItemStack stack) {
        crafted = stack;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public ItemStack take() {
        ItemStack stack = crafted;
        setCrafted(ItemStack.EMPTY);
        return stack;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Crafted", crafted.save(new CompoundTag()));
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        crafted = tag.contains("Crafted") ? ItemStack.of(tag.getCompound("Crafted")) : ItemStack.EMPTY;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        requestRerender();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        requestRerender();
    }

    private void requestRerender() {
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public double[] tableCenter() {
        BlockPos right = GunWorkbenchBlock.partPos(worldPosition, getBlockState().getValue(GunWorkbenchBlock.FACING),
                GunWorkbenchBlock.Part.RIGHT);
        return new double[]{(worldPosition.getX() + right.getX()) / 2.0 + 0.5, worldPosition.getY() + 1.0,
                (worldPosition.getZ() + right.getZ()) / 2.0 + 0.5};
    }

    public boolean outlineVisibleFor(Player player) {
        if (crafted.isEmpty()) {
            return false;
        }
        double[] c = tableCenter();
        return player.distanceToSqr(c[0], c[1], c[2]) <= OUTLINE_DISTANCE * OUTLINE_DISTANCE;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.5, 1.0, 1.5);
    }
}
