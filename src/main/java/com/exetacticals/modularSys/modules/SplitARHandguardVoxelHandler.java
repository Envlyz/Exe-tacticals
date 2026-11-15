package com.exetacticals.modularSys.modules;

import com.exetacticals.modularSys.IVoxel;
import com.exetacticals.modularSys.MultiVoxel;
import com.exetacticals.modularSys.builder.Accessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.impl.SplitARHandguard;
import net.minecraft.resources.ResourceLocation;

public class SplitARHandguardVoxelHandler extends VoxelHandler {
    private IVoxel total;
    private IVoxel upper;

    public SplitARHandguardVoxelHandler(ResourceLocation voxelAssetPath) {
        super(voxelAssetPath);
    }

    @Override
    public void setVoxelIfNull(MultiVoxel voxel) {
        super.setVoxelIfNull(voxel);
        total = voxel.getVoxelOrThrow("total");
        upper = voxel.getVoxelOrThrow("upper");
    }

    @Override
    public IVoxel getVoxel(Unit unit, Accessor accessor) {
        int customParam = unit.getCustomParam(SplitARHandguard.HIDE_LOWER_PART_PARAM_KEY);
        return customParam == -1 ? total : upper;
    }
}
