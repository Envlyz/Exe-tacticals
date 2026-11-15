package com.exetacticals.network.s2c;

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

public class GunStuckSyncPacket implements IPacket<GunStuckSyncPacket> {
    public static final ResourceLocation ID = GCR.RL("gun_stuck_sync");

    public String gunId;
    public boolean stuck;

    public GunStuckSyncPacket(String gunId, boolean stuck) {
        this.gunId = gunId;
        this.stuck = stuck;
    }

    public static GunStuckSyncPacket decode(FriendlyByteBuf buf) {
        return new GunStuckSyncPacket(buf.readUtf(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
        buf.writeBoolean(stuck);
    }

    @Override
    public void onClient(GunStuckSyncPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ClientGunStuckCache.get().onServerSync(packet.gunId, packet.stuck);

            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player == null) {
                return;
            }
            ItemStack mainHand = player.getMainHandItem();
            if (!(mainHand.getItem() instanceof GunItem gunItem)) {
                return;
            }
            IGun gun = gunItem.getGun();
            if (packet.gunId.equals(gun.getIdentityID(mainHand))) {
                gun.setStuck(packet.stuck, gun.rootNodeTag(mainHand));
            }
        });
    }

    @Override
    public void onServer(GunStuckSyncPacket packet, PacketContext context) {

    }}
