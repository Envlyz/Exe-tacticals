package com.exetacticals.modularSys.builder;

import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.slot.ISlot;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

public interface IWorkSpace extends IReadOnlyTree {

    Unit setRoot(IModular modular);

    Optional<Unit> addChild(Unit parentUnit, String parentSlotName, String childModuleID);

    Optional<Unit> addChild(Unit parent, ISlot parentSlot, Unit child);

    Optional<Unit> addChild(SlotInstance slot, Unit child);

    Optional<Unit> addChild(SlotInstance slot, IModular modular);

    boolean removeChild(Unit unitToRemove);

    Optional<Pair<Unit, List<Unit>>> replaceChild(Unit oldUnit, Unit newUnit);

    Optional<Pair<Unit, List<Unit>>> replaceChild(Unit oldUnit, IModular modular);

    boolean isEmpty();

    IWorkSpace deepCopy();

    int getMutateId();

    void increaseMutateId();

    void setOffset(Unit unit, float offset);

    String getUnitId(Unit unit);
}

