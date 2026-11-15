package com.exetacticals.modularSys.task;

import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.modules.guns.IGun;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class GunTaskHandler {
    public static final GunTaskHandler INSTANCE = new GunTaskHandler();
    private IGunTask<?> task;

    public boolean hasTask() {
        return task != null;
    }

    public boolean blockShoot() {
        return  task != null && task.blockShoot();
    }

    public int prevTaskPriority() {
        return task == null ? -1 : task.getPriority();
    }

    public void setTask(IGunTask<?> task) {

        if (this.task != null && task.getPriority() <= prevTaskPriority()) {
            return;
        }
        setForceTask(task);
    }

    public void setForceTask(IGunTask<?> task) {
        if (task == null || task == this.task) {
            return;
        }
        if (this.task != null) {
            if (!this.task.equals(task)) {
                cancelTask();
                this.task = task;
                task.start();
            }
        } else {
            this.task = task;
            task.start();
        }
    }

    public void cancelTask() {
        System.out.println("task canceled");
        task.onCancel();
        task = null;
    }

    public void tick(Player player) {
        if (task != null) {
            ItemStack mainHandItem = player.getMainHandItem();
            if (!(mainHandItem.getItem() instanceof GunItem gunItem)) {
                cancelTask();
                return;
            } else {
                String currID = gunItem.getGun().getIdentityID(mainHandItem);
                String taskID = task.getGun().getIdentityID(task.getStack());
                if (!IGun.NONE.equals(currID) && !IGun.NONE.equals(taskID) && !Objects.equals(currID, taskID)) {
                    cancelTask();
                    return;
                }
            }
            task.onTick(player);
            if (task.isCompleted()) {
                task.onComplete();
                task = null;
            }
        }
    }

    public int getTaskCustomVariable(String variableName) {
        return task == null ? -1 : task.getCustomVariable(variableName);
    }

    public boolean allowSprinting() {
        if (task == null) {
            return true;
        }
        return !task.blockSprinting();
    }
}
