package com.exetacticals.modularSys.slot;

import com.exetacticals.modularSys.IModular;

@FunctionalInterface
public interface ISlotFilter {
    boolean test(IModular modular);

    default ISlotFilter and(ISlotFilter other) {
        return m -> this.test(m) && other.test(m);
    }

    default ISlotFilter or(ISlotFilter other) {
        return m -> this.test(m) || other.test(m);
    }

    default ISlotFilter negate() {
        return m -> !this.test(m);
    }
}
