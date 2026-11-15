package com.exetacticals.modularSys.slot;


import com.google.common.collect.ImmutableSet;
import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.IModular;

public interface ISlot {
    String getName();
    boolean accepts(IModular modular);
    boolean allow(OperationType operationType);
    int maxCapacity();
    ImmutableSet<String> getTags();
    boolean hasTag(String tag);
    boolean defaultHidden();
    Direction getDirection();
    ISlotFilter getFilter();
    ISlot setFilter(ISlotFilter filter);

}
