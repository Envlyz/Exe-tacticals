package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.ISlotProvider;
import com.exetacticals.modularSys.ISlotProviderModular;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AKDustCover extends AKSimpleDustCover implements IVoxelHandlerModule, ISlotProviderModular {
    private ISlotProvider slotProvider;
    private IVoxelHandler voxelHandler;
    public AKDustCover(ResourceLocation id, float weight, float minStuckRateDec, float maxStuckRateDec, ISlotProvider slotProvider, IVoxelHandler voxelHandler) {
        super(id, weight, minStuckRateDec, maxStuckRateDec);
        this.slotProvider = slotProvider;
        this.voxelHandler = voxelHandler;
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }
}
