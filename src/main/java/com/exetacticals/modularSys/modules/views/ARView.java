package com.exetacticals.modularSys.modules.views;

import net.minecraft.nbt.CompoundTag;

public interface ARView extends IGunView {
    boolean boltLocked(CompoundTag states);
}
