package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.builder.IWriteableAccessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.FoldingIronSightVoxelHandler;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;

public class FoldingFarIronSight extends AttachmentModule implements IVoxelHandlerModule {
    private final FoldingIronSightVoxelHandler voxelHandler;
    public FoldingFarIronSight(ResourceLocation id, boolean fixedPosition, float weight, FoldingIronSightVoxelHandler voxelHandler) {
        super(id, fixedPosition, weight, Direction.UPPER);
        this.voxelHandler = voxelHandler;
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        FoldingRearIronSight.handleFolding(accessor, thisUnit);
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }
}
