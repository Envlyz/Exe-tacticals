package com.exetacticals.modularSys.task.other;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.Utils;
import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.AnimationRegister;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.stuck.ClientGunStuckCache;
import com.exetacticals.modularSys.modules.guns.ak.AK;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.network.c2s.RemoveStuckPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.exetacticals.network.GCRNetwork;

import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class AKRemoveStuckTask extends RemoveStuckTask<AK>{
    public String animationName;
    public int sendPacketDelay;

    public AKRemoveStuckTask(ItemStack itemStack, AK gun) {
        super(itemStack, gun);
        animationName = "remove_stuck";
        int ammoLeft = gun.getAmmoLeft(itemStack);
        if (ammoLeft <= 0) {
            animationName += "_empty";
        }
        String sendPacketDelayKey = animationName + "_length";
        sendPacketDelay = Utils.secondToTick(gun.baseProperties.taskTimers.getOrDefault(sendPacketDelayKey, 1.0f));
        AnimationDef animationDef = AnimationRegister.get(GCR.RL("ak74m_" + animationName));
        if (animationDef != null) {
            this.length = Math.max(Utils.secondToTick(animationDef.lengthInSeconds() - 0.05f), sendPacketDelay);
        }
    }

    @Override
    public boolean equals(IGunTask<?> other) {
        if (!(other instanceof AKRemoveStuckTask otherTask) || !Objects.equals(otherTask.animationName, this.animationName)) {
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
