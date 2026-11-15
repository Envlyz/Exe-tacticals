package com.exetacticals.network.c2s;

import com.exetacticals.GCR;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.fire.IFireMode;
import com.exetacticals.modularSys.fire.AftermatchWallTuck;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.network.IPacket;
import com.exetacticals.network.s2c.GunFireAckPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.exetacticals.network.GCRNetwork;
import com.exetacticals.network.PacketContext;
import org.jetbrains.annotations.NotNull;

public class GunFirePacket implements IPacket<GunFirePacket> {
    public static final ResourceLocation ID = GCR.RL("gun_fire");

    public int shootId;
    public String gunId;
    public boolean stuck;
    public float gunKickPitch;
    public float gunKickYaw;
    public float motionPitch;
    public float motionYaw;
    public static final float MAX_MOTION_PITCH = 10f;
    public static final float MAX_MOTION_YAW = 20f;

    public GunFirePacket(int shootId, String gunId, boolean stuck,
                         float gunKickPitch, float gunKickYaw, float motionPitch, float motionYaw) {
        this.shootId = shootId;
        this.gunId = gunId;
        this.stuck = stuck;
        this.gunKickPitch = gunKickPitch;
        this.gunKickYaw = gunKickYaw;
        this.motionPitch = motionPitch;
        this.motionYaw = motionYaw;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(shootId);
        buf.writeUtf(gunId);
        buf.writeBoolean(stuck);
        buf.writeFloat(gunKickPitch);
        buf.writeFloat(gunKickYaw);
        buf.writeFloat(motionPitch);
        buf.writeFloat(motionYaw);
    }

    public static GunFirePacket decode(FriendlyByteBuf buf) {
        return new GunFirePacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    @Override
    public void onClient(GunFirePacket packet, PacketContext context) {

    }

    @Override
    public void onServer(GunFirePacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack heldItem = player.getMainHandItem();
            if (!(heldItem.getItem() instanceof GunItem gunItem)) {
                reject(player, packet, false);
                return;
            }
            IGun gunModule = gunItem.getGun();
            if (!packet.gunId.equals(gunModule.getIdentityID(heldItem))) {
                reject(player, packet, false);
                return;
            }
            IFireMode<?> fireMode = gunModule.getFireMode(heldItem);
            if (fireMode == null || !fireMode.getGunClass().isInstance(gunModule)) {
                reject(player, packet, gunModule.isStuck(heldItem));
                return;
            }
            if (!Float.isFinite(packet.motionPitch) || !Float.isFinite(packet.motionYaw)
                    || Math.abs(packet.motionPitch) > MAX_MOTION_PITCH
                    || Math.abs(packet.motionYaw) > MAX_MOTION_YAW) {
                reject(player, packet, gunModule.isStuck(heldItem));
                return;
            }
            if (AftermatchWallTuck.check(player, 1f).fireBlocked()) {
                reject(player, packet, gunModule.isStuck(heldItem));
                return;
            }
            invokeServerShoot(fireMode, player, heldItem, gunModule, GunFirePacket.this);
        });
    }

    private static void reject(ServerPlayer player, GunFirePacket packet, boolean stuck) {
        GCRNetwork.sendToPlayer(
                player,
                new GunFireAckPacket(packet.gunId, -1, packet.shootId, stuck, false)
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends IGun> void invokeServerShoot(
            IFireMode<T> fireMode, ServerPlayer player, ItemStack stack, IGun gunModule, GunFirePacket packet) {
        fireMode.triggerServerShoot(player, stack, (T) gunModule, packet);
    }}

