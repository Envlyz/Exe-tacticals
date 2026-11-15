package com.exetacticals;

import net.minecraft.nbt.CompoundTag;

public interface INBTSync {
    void writeData(CompoundTag tag);
    void loadData(CompoundTag tag);
}
