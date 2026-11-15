package com.exetacticals.modularSys.task.other;

import com.exetacticals.Client;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.modularSys.fire.IFireMode;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.task.GunTask;
import com.exetacticals.network.c2s.SyncGunStatusPacket;
import net.minecraft.world.item.ItemStack;

import java.util.Map;


public class FireModeSwitchTask extends GunTask<IGun> {
    public FireModeSwitchTask(ItemStack itemStack, IGun gun) {
        super(itemStack, gun, 1);
    }

    @Override
    public void start() {
        IFireMode<?> fireMode = gun.getFireMode(itemStack);
        gun.toNextFireMode(itemStack);
        IFireMode<?> afterMode = gun.getFireMode(itemStack);
        if (afterMode != fireMode) {
            SyncGunStatusPacket.toServer(gun, itemStack, IGun.FIRE_MODEL_ID);
            Client.getGunRenderer().dispatchAnimationEvent(
                    EventType.SWITCH_FIRE_MODE,
                    Map.of("before", fireMode.getName(), "after", afterMode.getName())
            );
        }

    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public boolean blockShoot() {
        return false;
    }

    @Override
    public TaskType getType() {
        return TaskType.SWITCH_FIRE_MODE;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean blockSprinting() {
        return false;
    }
}
