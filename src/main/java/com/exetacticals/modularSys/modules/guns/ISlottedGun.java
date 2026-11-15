package com.exetacticals.modularSys.modules.guns;

import com.exetacticals.modularSys.modules.IAmmoSource;
import com.exetacticals.modularSys.modules.ISight;
import com.exetacticals.modularSys.slot.ISlot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface ISlottedGun {

    SlottedGunMainPart addSlot(ISlot slotDef);

    boolean switchUsingSight(ItemStack itemStack);

    int getGunAmmoLeft(ItemStack itemStack);

    int getMagAmmoLeft(ItemStack itemStack);

    void setGunAmmoLeft(ItemStack itemStack, int ammoLeft);

    void setMagAmmoLeft(ItemStack itemStack, int ammoLeft);

    @Nullable
    IAmmoSource getMagAttachment(ItemStack itemStack);

    @Nullable
    ISight getScopeAttachment(ItemStack itemStack);

}
