package com.exetacticals.modularSys.modules;

import com.exetacticals.modularSys.IVoxel;
import com.exetacticals.modularSys.MultiVoxel;
import com.exetacticals.modularSys.builder.Accessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.slot.ISlot;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

public interface IVoxelHandler {
    String VOXEL_INDEX_PARAM_KEY = "voxel_index";

    IVoxel getVoxel(Unit unit, Accessor accessor);
    void setVoxelIfNull(MultiVoxel voxel);
    ResourceLocation getAssetPath();
    Pair<Boolean, Boolean> ignoreBoundaryCollision(ISlot slot);
}
