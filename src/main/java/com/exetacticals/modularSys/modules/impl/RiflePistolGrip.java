package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.modules.UniqueModule;
import com.exetacticals.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

public class RiflePistolGrip extends UniqueModule {

    public RiflePistolGrip(ResourceLocation id,
                           float recoilControlInc,
                           float stabilityInc,
                           float agilityInc,
                           float weight) {
        super(id, true, weight, Direction.NONE);
        defPropInc(BaseProperties.class, p -> p.recoilControl, recoilControlInc);
        defPropInc(BaseProperties.class, p -> p.stability, stabilityInc);
        defPropInc(BaseProperties.class, p -> p.agility, agilityInc);

    }

}
