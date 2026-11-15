package com.exetacticals.modularSys.modules.guns;

import net.minecraft.world.item.ItemStack;

public interface BoltHoldOpenCapable {
    String BOLT_LOCKED_KEY = "gcr_bolt_locked";
    boolean isBoltLocked(ItemStack stack);

    void setBoltLocked(ItemStack stack, boolean locked);
}
