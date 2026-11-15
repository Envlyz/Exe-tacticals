package com.exetacticals.modularSys;

import com.google.gson.JsonObject;
import com.exetacticals.GCR;
import com.exetacticals.modularSys.slot.ISlot;
import com.exetacticals.modularSys.util.PivotMap;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class EmptySlotProvider implements ISlotProvider{
    public static final EmptySlotProvider INSTANCE = new EmptySlotProvider();

    @Override
    public ResourceLocation getAssetsPath() {
        return GCR.RL("");
    }

    @Override
    public void setPivotMap(PivotMap pivotMap) {

    }

    @Override
    public PivotMap getPivotMap() {
        return PivotMap.EMPTY;
    }

    @Override
    public List<ISlot> getSlots() {
        return List.of();
    }

    @Override
    public ISlotProvider addSlot(ISlot slotDef) {
        return this;
    }

    @Override
    public boolean hasSlot(String slotName) {
        return false;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }
}
