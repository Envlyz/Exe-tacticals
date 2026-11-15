package com.exetacticals.modularSys.slot;

import com.google.common.collect.ImmutableSet;
import com.exetacticals.modularSys.Direction;

import java.util.Set;

public class SingleFixedSlot extends Slot {

    public SingleFixedSlot(String name, Direction direction, String... tags) {
        super(name, direction, Set.of(
                OperationType.ADD,
                OperationType.REMOVE,
                OperationType.REPLACE
        ), ImmutableSet.copyOf(tags));
    }

    public SingleFixedSlot(String name, String... tags) {
        this(name, Direction.NONE, tags);
    }

}
