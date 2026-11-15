package com.exetacticals.modularSys.task.other;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.Utils;
import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.AnimationRegister;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.stuck.ClientGunStuckCache;
import com.exetacticals.modularSys.modules.IAmmoSource;
import com.exetacticals.modularSys.modules.guns.ar.AR;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.network.c2s.RemoveStuckPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.exetacticals.network.GCRNetwork;

import java.util.Map;
import java.util.Objects;

public class ARRemoveStuckTask extends RemoveStuckTask<AR>{
    public String animationName;
    public int sendPacketDelay;

    public ARRemoveStuckTask(ItemStack itemStack, AR gun) {
        super(itemStack, gun);
        CompoundTag magStates = gun.getAmmoSourceTag(itemStack);
        IAmmoSource mag = gun.getMagAttachment(itemStack);
        if (mag == null) {
            animationName = "remove_stuck_empty";
        } else {
            int ammoLeft = mag.getAmmoLeft(magStates);
            if (ammoLeft >= 1) {
                animationName = "remove_stuck";
            } else {
                animationName = "remove_stuck_empty";
            }
        }
        String sendPacketDelayKey = animationName + "_length";
        sendPacketDelay = Utils.secondToTick(gun.baseProperties.taskTimers.getOrDefault(sendPacketDelayKey, 1.0f));
        AnimationDef animationDef = AnimationRegister.get(GCR.RL("m4a1_" + animationName));
        if (animationDef != null) {
            this.length = Math.max(Utils.secondToTick(animationDef.lengthInSeconds() - 0.05f), sendPacketDelay);
        }
    }

    @Override
    public boolean equals(IGunTask<?> other) {
        if (!(other instanceof ARRemoveStuckTask otherTask) || !Objects.equals(otherTask.animationName, this.animationName)) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            String gunId = gun.getIdentityID(itemStack);
            GCRNetwork.sendToServer(new RemoveStuckPacket(gunId));
        }
    }

    @Override
    public void start() {
        if (animationName != null) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.REMOVE_STUCK, Map.of("name", animationName));
        }
    }
}
