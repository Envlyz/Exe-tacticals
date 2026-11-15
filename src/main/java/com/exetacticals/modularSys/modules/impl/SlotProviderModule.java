package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.ISlotProvider;
import com.exetacticals.modularSys.ISlotProviderModular;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SlotProviderModule extends AttachmentModule implements ISlotProviderModular {
    private final ISlotProvider slotProvider;

    public SlotProviderModule(ResourceLocation id, float weight, boolean fixedPosition, Direction direction, ISlotProvider slotProvider) {
        super(id, fixedPosition, weight, direction);
        this.slotProvider = slotProvider;
    }

    @Override
    public @NotNull ISlotProvider getSlotProvider() {
        return slotProvider;
    }

}
