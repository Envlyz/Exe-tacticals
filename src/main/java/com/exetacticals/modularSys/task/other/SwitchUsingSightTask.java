package com.exetacticals.modularSys.task.other;

import com.exetacticals.modularSys.modules.guns.SlottedGunMainPart;
import com.exetacticals.modularSys.task.GunTask;
import com.exetacticals.network.c2s.SwitchUsingSightPacket;
import net.minecraft.world.item.ItemStack;
import com.exetacticals.network.GCRNetwork;


public class SwitchUsingSightTask extends GunTask<SlottedGunMainPart> {
    public SwitchUsingSightTask(ItemStack itemStack, SlottedGunMainPart gun) {
        super(itemStack, gun, 1);
    }

    @Override
    public void start() {
        gun.switchUsingSight(itemStack);
        GCRNetwork.sendToServer(new SwitchUsingSightPacket());
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
        return TaskType.SWITCH_USING_SIGHT;
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
