package com.exetacticals.modularSys.modules;

import com.exetacticals.modularSys.IVoxel;
import com.exetacticals.modularSys.MultiVoxel;
import com.exetacticals.modularSys.Voxel;
import com.exetacticals.modularSys.builder.Accessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.slot.ISlot;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class EmptyVoxelHandler implements IVoxelHandler{
    public static final EmptyVoxelHandler INSTANCE = new EmptyVoxelHandler();
    public static final IVoxel EMPTY_VOXEL = new Voxel(List.of());

    @Override
    public IVoxel getVoxel(Unit unit, Accessor accessor) {
        return EMPTY_VOXEL;
    }

    @Override
    public void setVoxelIfNull(MultiVoxel voxel) {

    }

    @Override
    public ResourceLocation getAssetPath() {
        return null;
    }

    @Override
    public Pair<Boolean, Boolean> ignoreBoundaryCollision(ISlot slot) {
        return Pair.of(true, true);
    }
}
