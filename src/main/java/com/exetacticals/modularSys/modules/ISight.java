package com.exetacticals.modularSys.modules;

import com.exetacticals.modularSys.builder.Unit;

public interface ISight {
    String ON_SIDE_POSITION = "on_side_position";
    int IGNORE = -1;
    int SIDE = 0;
    int GUN_BASE = 1;
    int IRON_SIGHT = 2;
    int RED_DOT = 3;
    int SCOPE = 4;

    default int getSightPriority(Unit unit) {
        int customParam = unit.getCustomParam(ON_SIDE_POSITION);
        if (customParam == -1) {
            return defaultSightPriority(unit);
        } else {
            return SIDE;
        }
    }

    float getAdsSpeedModifier();

    default float getZCompensation() {
        return 1f;
    }

    int defaultSightPriority(Unit unit);
}
