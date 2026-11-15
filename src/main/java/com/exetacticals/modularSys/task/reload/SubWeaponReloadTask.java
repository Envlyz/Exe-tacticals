package com.exetacticals.modularSys.task.reload;

import com.exetacticals.Client;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.task.GunTask;
import com.exetacticals.network.c2s.SubWeaponReloadPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.exetacticals.network.GCRNetwork;

import java.util.Map;

public class SubWeaponReloadTask extends GunTask<IGun> {
    private final int sendPacketDelay;
    private final String nodeId;
    private final String gunId;
    public SubWeaponReloadTask(ItemStack itemStack, IGun gun, int lengthInTicks, int sendPacketDelay, String nodeId, String gunId) {
        super(itemStack, gun, lengthInTicks);
        this.sendPacketDelay = sendPacketDelay;
        this.nodeId = nodeId;
        this.gunId = gunId;
    }

    @Override
    public void start() {
        Client.getGunRenderer().dispatchAnimationEvent(EventType.RELOAD_SUB_WEAPON, Map.of("animation_name", "reload_grenade"));
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            GCRNetwork.sendToServer(new SubWeaponReloadPacket(gunId, nodeId));
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public TaskType getType() {
        return TaskType.RELOAD;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    public int getSendPacketDelay() {
        return sendPacketDelay;
    }
}
