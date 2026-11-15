package com.exetacticals.modularSys.modules;

import com.exetacticals.modularSys.modules.states.Bool;
import com.exetacticals.modularSys.modules.views.IFlashLightView;
import net.minecraft.nbt.CompoundTag;

public interface IFlashLight extends IFlashLightView, IStateModular {
    Bool IS_ON = new Bool("is_on");

    @Override
    default void onInitStates(CompoundTag states, String nodeId, String moduleId) {
        IS_ON.init(states);
    }

    void setIsOn(boolean isOn, CompoundTag states);
}
