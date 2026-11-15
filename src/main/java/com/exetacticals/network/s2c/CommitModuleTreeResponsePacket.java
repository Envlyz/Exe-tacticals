package com.exetacticals.network.s2c;

import com.exetacticals.GCR;
import com.exetacticals.client.screen.modify.GunModifyScreen;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.network.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.exetacticals.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class CommitModuleTreeResponsePacket implements IPacket<CommitModuleTreeResponsePacket> {
    public static final ResourceLocation ID = GCR.RL("set_module_tree_resp");

    public static CommitModuleTreeResponsePacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new CommitModuleTreeResponsePacket(
                (CompoundTag) friendlyByteBuf.readNbt(),
                friendlyByteBuf.readUtf(),
                friendlyByteBuf.readBoolean());
    }

    public CompoundTag data;
    public String msg;
    public boolean success;

    public CommitModuleTreeResponsePacket(CompoundTag data, String msg, boolean success) {
        this.data = data;
        this.msg = msg;
        this.success = success;
    }

    @Override
    public void onClient(CommitModuleTreeResponsePacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player != null && player.getMainHandItem().getItem() instanceof GunItem gunItem) {
                IGun gun = gunItem.getGun();
                gun.fullSyncFromServer(player.getMainHandItem(), packet.data);
            }
            if (instance.screen instanceof GunModifyScreen screen) {
                screen.onServerResp(packet.success, packet.msg);
            }
        });
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
        buf.writeUtf(msg);
        buf.writeBoolean(success);
    }

    @Override
    public void onServer(CommitModuleTreeResponsePacket packet, PacketContext context) {

    }}
