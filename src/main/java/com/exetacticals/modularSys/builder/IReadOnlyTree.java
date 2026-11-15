package com.exetacticals.modularSys.builder;

import net.minecraft.nbt.ListTag;

import java.util.List;
import java.util.Optional;

public interface IReadOnlyTree {
    Unit getRootUnit();
    Optional<Unit> findUnitById(String id);
    List<Unit> getSequencedUnits();
    ListTag write();
    String getId(Unit unit);
    Optional<Unit> getParentOf(Unit childUnit);

    List<Unit> getChildrenOf(SlotInstance slotInstance);

    ShadowNode getShadowTree();
}
