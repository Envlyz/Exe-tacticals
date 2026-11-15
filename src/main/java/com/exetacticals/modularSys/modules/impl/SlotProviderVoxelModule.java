package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.ISlotProvider;
import com.exetacticals.modularSys.ISlotProviderModular;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SlotProviderVoxelModule extends AttachmentModule implements ISlotProviderModular, IVoxelHandlerModule{
    private ISlotProvider slotProvider;
    private IVoxelHandler handler;
    public SlotProviderVoxelModule(ResourceLocation id, boolean fixedPosition, float weight, Direction direction, ISlotProvider slotProvider, IVoxelHandler handler) {
        super(id, fixedPosition, weight, direction);
        this.slotProvider = slotProvider;
        this.handler = handler;
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

    @Override
    public IVoxelHandler getHandler() {
        return handler;
    }
}
