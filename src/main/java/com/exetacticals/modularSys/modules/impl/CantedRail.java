package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.ISlotProvider;
import com.exetacticals.modularSys.builder.IWriteableAccessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.ISight;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class CantedRail extends SlotProviderVoxelModule {
    public CantedRail(ResourceLocation id, IVoxelHandler voxelHandler, ISlotProvider provider, float weight) {
        super(id, false, weight, Direction.UPPER, provider, voxelHandler);
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        List<Unit> sights = accessor.getSlotChildren(thisUnit, "SIGHT");
        for (Unit sight : sights) {
            accessor.writeCustomParam(sight, ISight.ON_SIDE_POSITION, 1);
        }
    }
}