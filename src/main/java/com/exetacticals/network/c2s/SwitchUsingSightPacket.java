package com.exetacticals.network.c2s;

import com.exetacticals.GCR;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.modules.guns.ISlottedGun;
import com.exetacticals.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.exetacticals.network.PacketContext;

public class SwitchUsingSightPacket implements IPacket<SwitchUsingSightPacket> {
    public static final ResourceLocation ID = GCR.RL("switch_using_sight");

    public void encode(FriendlyByteBuf buf) {

    }

    public static SwitchUsingSightPacket decode(FriendlyByteBuf buf) {
        return new SwitchUsingSightPacket();
    }

    @Override
    public void onClient(SwitchUsingSightPacket packet, PacketContext context) {

    }

    @Override
    public void onServer(SwitchUsingSightPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem && gunItem.getGun() instanceof ISlottedGun slottedGun) {
                if (slottedGun.switchUsingSight(heldItem)) {
                    gunItem.getGun().notifyDataChanged(heldItem);
                }
            }
        });
    }}
