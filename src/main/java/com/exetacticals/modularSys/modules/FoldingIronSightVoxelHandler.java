package com.exetacticals.modularSys.modules;

import com.exetacticals.modularSys.IVoxel;
import com.exetacticals.modularSys.MultiVoxel;
import com.exetacticals.modularSys.builder.Accessor;
import com.exetacticals.modularSys.builder.Unit;
import net.minecraft.resources.ResourceLocation;

public class FoldingIronSightVoxelHandler extends VoxelHandler {
    public static String FOLD_SIGHT_PARAM_KEY = "fold_sight";
    private IVoxel normal;
    private IVoxel fold;

    public FoldingIronSightVoxelHandler(ResourceLocation voxelAssetPath, boolean ignoreBoundaryCollisionRear, boolean ignoreBoundaryCollisionFar) {
        super(voxelAssetPath, ignoreBoundaryCollisionRear, ignoreBoundaryCollisionFar);
    }

    public FoldingIronSightVoxelHandler(ResourceLocation voxelAssetPath) {
        this(voxelAssetPath, false, false);
    }

    @Override
    public void setVoxelIfNull(MultiVoxel voxel) {
        super.setVoxelIfNull(voxel);
        normal = voxel.getVoxelOrThrow("normal");
        fold = voxel.getVoxelOrThrow("fold");
    }

    @Override
    public IVoxel getVoxel(Unit unit, Accessor accessor) {
        int customParam = unit.getCustomParam(FOLD_SIGHT_PARAM_KEY);
        return customParam == -1 ? normal : fold;
    }
}
