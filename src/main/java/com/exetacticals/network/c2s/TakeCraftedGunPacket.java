package com.exetacticals.network.c2s;

import com.exetacticals.network.IPacket;
import com.exetacticals.network.PacketContext;
import com.exetacticals.workbench.GunWorkbenchBlock;
import com.exetacticals.workbench.GunWorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class TakeCraftedGunPacket implements IPacket<TakeCraftedGunPacket> {
    private static final double MAX_DISTANCE = 6.0;
    public final BlockPos pos;

    public TakeCraftedGunPacket(BlockPos pos) {
        this.pos = pos;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static TakeCraftedGunPacket decode(FriendlyByteBuf buf) {
        return new TakeCraftedGunPacket(buf.readBlockPos());
    }

    @Override
    public void onClient(TakeCraftedGunPacket packet, PacketContext context) {
    }

    @Override
    public void onServer(TakeCraftedGunPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()
                    || !player.level().isLoaded(packet.pos)) {
                return;
            }
            GunWorkbenchBlockEntity table = GunWorkbenchBlock.blockEntity(player.level(), packet.pos);
            if (table == null || !table.isOccupied()) {
                return;
            }
            double[] c = table.tableCenter();
            if (player.distanceToSqr(c[0], c[1], c[2]) > MAX_DISTANCE * MAX_DISTANCE) {
                return;
            }
            ItemStack stack = table.take();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.level().playSound(null, packet.pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.1f);
        });
    }
}
