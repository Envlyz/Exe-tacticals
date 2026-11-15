package com.exetacticals.network.c2s;

import com.exetacticals.GCR;
import com.exetacticals.items.GunItem;
import com.exetacticals.network.c2s.PhysicalMagazinePacket;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.network.IPacket;
import com.exetacticals.network.s2c.GunStuckSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.exetacticals.network.GCRNetwork;
import com.exetacticals.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class RemoveStuckPacket implements IPacket<RemoveStuckPacket> {
    public static final ResourceLocation ID = GCR.RL("remove_stuck");

    public String gunId;

    public RemoveStuckPacket(String gunId) {
        this.gunId = gunId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(gunId);
    }

    public static RemoveStuckPacket decode(FriendlyByteBuf buf) {
        return new RemoveStuckPacket(buf.readUtf());
    }

    @Override
    public void onClient(RemoveStuckPacket packet, PacketContext context) {

    }

    @Override
    public void onServer(RemoveStuckPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            long started = player.getPersistentData().getLong(PhysicalMagazinePacket.JAM_START_KEY);
            String startedGun = player.getPersistentData().getString(PhysicalMagazinePacket.JAM_GUN_KEY);
            long elapsed = player.level().getGameTime() - started;
            ItemStack target = player.getMainHandItem();
            if (started <= 0L || elapsed < 7L || elapsed > 100L
                    || !packet.gunId.equals(startedGun)
                    || !(target.getItem() instanceof GunItem gunHeld)
                    || !packet.gunId.equals(gunHeld.getGun().getIdentityID(target))) {
                if (target.getItem() instanceof GunItem current) {
                    GCRNetwork.sendToPlayer(player,
                            new GunStuckSyncPacket(packet.gunId, current.getGun().isStuck(target)));
                }
                return;
            }
            player.getPersistentData().remove(PhysicalMagazinePacket.JAM_START_KEY);
            player.getPersistentData().remove(PhysicalMagazinePacket.JAM_GUN_KEY);
            IGun gun = ((GunItem) target.getItem()).getGun();
            if (gun.isStuck(target)) {
                gun.removeStuck(target);
                gun.notifyDataChanged(target);
            }

            GCRNetwork.sendToPlayer(player, new GunStuckSyncPacket(packet.gunId, gun.isStuck(target)));
        });
    }

    private static ItemStack findGun(ServerPlayer player, String gunId) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof GunItem gunItem
                && gunId.equals(gunItem.getGun().getIdentityID(mainHand))) {
            return mainHand;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof GunItem gunItem
                    && gunId.equals(gunItem.getGun().getIdentityID(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }}
