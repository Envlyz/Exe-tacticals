package com.exetacticals.modularSys;

import com.exetacticals.IJsonSync;
import com.exetacticals.modularSys.slot.ISlot;
import com.exetacticals.modularSys.util.PivotMap;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface ISlotProvider extends IJsonSync {
    ResourceLocation getAssetsPath();
    void setPivotMap(PivotMap pivotMap);
    PivotMap getPivotMap();
    List<ISlot> getSlots();
    ISlotProvider addSlot(ISlot slotDef);
    boolean hasSlot(String slotName);
}
