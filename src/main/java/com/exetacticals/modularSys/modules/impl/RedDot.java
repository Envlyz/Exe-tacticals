package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import net.minecraft.resources.ResourceLocation;

public class RedDot extends Sight {
    public RedDot(ResourceLocation id, IVoxelHandler voxelHandler, float weight, boolean fixedPosition, float adsSpeedModifier) {
        super(id, voxelHandler, weight, fixedPosition, adsSpeedModifier);
    }

    @Override
    public int defaultSightPriority(Unit unit) {
        return RED_DOT;
    }
}
