package com.exetacticals.modularSys.modules.guns.ar;

import com.exetacticals.client.recoil.RecoilData;
import com.exetacticals.items.DisplayData;
import com.exetacticals.modularSys.fire.IFireMode;
import com.exetacticals.modularSys.modules.IAmmoSource;
import com.exetacticals.modularSys.modules.gunProperties.impl.BaseProperties;
import com.exetacticals.modularSys.modules.guns.SlottedGunMainPart;
import com.exetacticals.modularSys.modules.states.Bool;
import com.exetacticals.modularSys.modules.views.ARView;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.modularSys.task.other.ARRemoveStuckTask;
import com.exetacticals.modularSys.task.reload.ARReloadTask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AR extends SlottedGunMainPart implements ARView {
    protected Bool BOLT_LOCKED = new Bool("bolt_locked");

    public AR(ResourceLocation id, ResourceLocation pivotMapPath, BaseProperties baseDataModule, DisplayData displayData, RecoilData recoilData, List<IFireMode<?>> fireModes) {
        super(id, pivotMapPath, baseDataModule, displayData, recoilData, fireModes);
    }

    @Override
    protected IGunTask<?> getReloadTask(ItemStack itemStack) {
        return new ARReloadTask(itemStack, this);
    }

    @Override
    protected IGunTask<?> getRemoveStuckTask(ItemStack itemStack) {
        return new ARRemoveStuckTask(itemStack, this);
    }

    public void setBoltLocked(boolean locked, CompoundTag states) {
        BOLT_LOCKED.set(locked, states);
    }

    @Override
    public void reload(ItemStack itemStack, Player player) {
        IAmmoSource mag = getMagAttachment(itemStack);
        if (mag != null) {
            CompoundTag states = getAmmoSourceTag(itemStack);
            int ammoLeft = mag.getAmmoLeft(states);
            mag.setAmmoLeft(mag.getMaxCapacity(), states);
            if (ammoLeft < 1) {
                setGunAmmoLeft(itemStack, 1);
                mag.setAmmoLeft(mag.getAmmoLeft(states) - 1, states);
            }
            setBoltLocked(false, rootNodeTag(itemStack));
        } else {
            setGunAmmoLeft(itemStack, 1);
            setBoltLocked(false, rootNodeTag(itemStack));
        }
    }

    @Override
    public void removeStuck(ItemStack itemStack) {
        super.removeStuck(itemStack);
        IAmmoSource mag = getMagAttachment(itemStack);
        if (mag != null) {
            CompoundTag states = getAmmoSourceTag(itemStack);
            int ammoLeft = mag.getAmmoLeft(states);
            if (ammoLeft >= 1 && getGunAmmoLeft(itemStack) <= 0) {
                setGunAmmoLeft(itemStack, 1);
                mag.setAmmoLeft(mag.getAmmoLeft(states) - 1, states);
            }
        }
    }
    @Override
    public boolean boltLocked(CompoundTag states) {
        return BOLT_LOCKED.get(states);
    }
}
