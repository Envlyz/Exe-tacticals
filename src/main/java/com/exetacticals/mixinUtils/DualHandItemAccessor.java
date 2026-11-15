package com.exetacticals.mixinUtils;

import net.minecraft.world.item.ItemStack;

public interface DualHandItemAccessor {
    ItemStack getMainHandItem();
    void setMainHandItem(ItemStack stack);

//    ItemStack getOffHandItem();
//    void setOffHandItem(ItemStack stack);
}