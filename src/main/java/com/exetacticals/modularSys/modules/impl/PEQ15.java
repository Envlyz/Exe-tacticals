package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.builder.IWriteableAccessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.IVoxelHandlerModule;
import com.exetacticals.modularSys.modules.MLokFitVoxelHandler;
import net.minecraft.resources.ResourceLocation;

public class PEQ15 extends FlashLightModule implements IVoxelHandlerModule {
    private final MLokFitVoxelHandler voxelHandler;
    public PEQ15(ResourceLocation id, float weight, float luminance, float range, float angle, Direction direction, MLokFitVoxelHandler voxelHandler) {
        super(id, false, weight, luminance, range, angle, direction);
        this.voxelHandler = voxelHandler;
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {
        super.onMutated(accessor, thisUnit);
        accessor.getBelongsTo(thisUnit).ifPresent(belongsTo -> {
            if (belongsTo.getSlot().hasTag("m_lok_rail")) {
                accessor.writeCustomParam(thisUnit, IVoxelHandler.VOXEL_INDEX_PARAM_KEY, 1);
            }
        });
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }
}
