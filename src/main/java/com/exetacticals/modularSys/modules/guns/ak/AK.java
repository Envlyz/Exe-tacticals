package com.exetacticals.modularSys.modules.guns.ak;

import com.exetacticals.client.recoil.RecoilData;
import com.exetacticals.items.DisplayData;
import com.exetacticals.modularSys.fire.IFireMode;
import com.exetacticals.modularSys.modules.IAmmoSource;
import com.exetacticals.modularSys.modules.gunProperties.impl.BaseProperties;
import com.exetacticals.modularSys.modules.guns.SlottedGunMainPart;
import com.exetacticals.modularSys.modules.views.AKView;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.modularSys.task.other.AKRemoveStuckTask;
import com.exetacticals.modularSys.task.reload.AKReloadTask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AK  extends SlottedGunMainPart implements AKView {
    public AK(ResourceLocation id, ResourceLocation pivotMapPath, BaseProperties baseDataModule, DisplayData displayData, RecoilData recoilData, List<IFireMode<?>> fireModes) {
        super(id, pivotMapPath, baseDataModule, displayData, recoilData, fireModes);
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
        } else {
            setGunAmmoLeft(itemStack, 1);
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
    protected IGunTask<?> getReloadTask(ItemStack itemStack) {
        return new AKReloadTask(itemStack, this);
    }

    @Override
    protected IGunTask<?> getRemoveStuckTask(ItemStack itemStack) {
        return new AKRemoveStuckTask(itemStack, this);
    }
}
