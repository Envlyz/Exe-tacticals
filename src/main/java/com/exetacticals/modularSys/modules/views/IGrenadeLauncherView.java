package com.exetacticals.modularSys.modules.views;

import com.exetacticals.modularSys.modules.states.Str;
import net.minecraft.nbt.CompoundTag;

public interface IGrenadeLauncherView extends IStateView {
    String CHAMBER_EMPTY = "empty";
    String CHAMBER_LOADED = "loaded";

    Str CHAMBER_STATUS = new Str("chamber_status", CHAMBER_EMPTY);

    String getChamberStatus(CompoundTag states);
}
