package com.exetacticals.modularSys.modules.gunProperties;

import com.exetacticals.IJsonSync;
import com.exetacticals.INBTSync;
import net.minecraft.nbt.CompoundTag;

public interface IProp extends IJsonSync, INBTSync {
    void inc(CompoundTag tag, float incRate);

    void dec(CompoundTag tag, float decRate);

    float get(CompoundTag properties);

    float getRatio(CompoundTag properties);

    String getKey();

    void setFullName(String fullName);

    String getFullNameKey();

    String getFullName();

    void setUpDefault(CompoundTag tag);
}
