package com.exetacticals.modularSys.modules.impl;

import com.google.gson.JsonObject;
import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.modules.ISight;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.resources.ResourceLocation;

public abstract class Sight extends AttachmentModule implements IVoxelHandlerModule, ISight {
    private final IVoxelHandler voxelHandler;
    private final float adsSpeedModifier;
    public Sight(ResourceLocation id, IVoxelHandler voxelHandler, float weight, boolean fixedPosition, float adsSpeedModifier) {
        super(id, fixedPosition, weight, Direction.UPPER);
        this.voxelHandler = voxelHandler;
        this.adsSpeedModifier = adsSpeedModifier;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @Override
    public IVoxelHandler getHandler() {
        return voxelHandler;
    }

    @Override
    public float getAdsSpeedModifier() {
        return adsSpeedModifier;
    }
}
