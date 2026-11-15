package com.exetacticals.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public final class PacketContext {
    private final NetworkEvent.Context context;
    public PacketContext(NetworkEvent.Context context) { this.context = context; }
    public Player player() { return context.getSender(); }
    public void enqueueWork(Runnable work) { context.enqueueWork(work); }
}
