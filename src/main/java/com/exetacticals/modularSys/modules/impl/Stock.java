package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class Stock extends AttachmentModule {

    public Stock(ResourceLocation id, float weight, float recoilControlInc, float stabilityInc) {
        super(id, true, weight, Direction.NONE);
        defPropInc(BaseProperties.class, p -> p.recoilControl, recoilControlInc);
        defPropInc(BaseProperties.class, p -> p.stability, stabilityInc);
    }

}
