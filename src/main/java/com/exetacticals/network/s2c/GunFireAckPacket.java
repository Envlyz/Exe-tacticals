package com.exetacticals.network.s2c;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.client.stuck.ClientGunStuckCache;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.network.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.exetacticals.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class GunFireAckPacket implements IPacket<GunFireAckPacket> {
    public static final ResourceLocation ID = GCR.RL("gun_fire_ack");

    public String gunId;
    public int ammoLeft;
    public int shootId;
    public boolean stuck;
    public boolean fired;

    public GunFireAckPacket(String gunId, int ammoLeft, int shootId, boolean stuck, boolean fired) {
        this.gunId = gunId;
        this.ammoLeft = ammoLeft;
        this.shootId = shootId;
        this.stuck = stuck;
        this.fired = fired;
    }

    public static GunFireAckPacket decode(FriendlyByteBuf buf) {
        return new GunFireAckPacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeInt(ammoLeft);
        buf.writeInt(shootId);
        buf.writeBoolean(stuck);
        buf.writeBoolean(fired);
    }

    @Override
    public void onClient(GunFireAckPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ClientGunStuckCache.get().onServerAck(packet.gunId, packet.shootId, packet.stuck, packet.fired);

            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
                ItemStack mainHand = player.getMainHandItem();
                IGun gun = gunItem.getGun();
                if (packet.gunId.equals(gun.getIdentityID(mainHand))) {
                    Client.serverShootAck(packet, mainHand, gun);
                }
            }
        });
    }

    @Override
    public void onServer(GunFireAckPacket packet, PacketContext context) {

    }}
