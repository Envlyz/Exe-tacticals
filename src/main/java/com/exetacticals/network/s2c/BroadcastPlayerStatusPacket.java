package com.exetacticals.network.s2c;

import com.exetacticals.GCR;
import com.exetacticals.data.PlayerStatusEvents;
import com.exetacticals.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.exetacticals.network.PacketContext;

public class BroadcastPlayerStatusPacket implements IPacket<BroadcastPlayerStatusPacket> {
    public static final ResourceLocation ID = GCR.RL("broadcast_player_status");

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeLong(gcrCredit);
        buf.writeBoolean(isReloading);
    }

    public static BroadcastPlayerStatusPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new BroadcastPlayerStatusPacket(
                friendlyByteBuf.readInt(),
                friendlyByteBuf.readLong(),
                friendlyByteBuf.readBoolean());
    }

    public int entityID;
    public long gcrCredit;
    public boolean isReloading;

    public BroadcastPlayerStatusPacket(int entityID, long gcrCredit, boolean isReloading) {
        this.entityID = entityID;
        this.gcrCredit = gcrCredit;
        this.isReloading = isReloading;
    }

    @Override
    public void onClient(BroadcastPlayerStatusPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            PlayerStatusEvents.clientReceivedServerBroadcast(packet);
        });
    }

    @Override
    public void onServer(BroadcastPlayerStatusPacket packet, PacketContext context) {}}
