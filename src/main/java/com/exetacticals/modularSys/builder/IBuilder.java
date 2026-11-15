package com.exetacticals.modularSys.builder;

import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ISlotProviderModular;
import net.minecraft.nbt.ListTag;

import java.util.List;

public interface IBuilder {

    <T extends ISlotProviderModular & IModular> IBuilder root(T modular);
    IWorkSpace getWorkspace();

    IReadOnlyTree getWarehouse();

    ValidateResult checkWorkspace(boolean checkCollision);

    List<ValidateResult> commit();

    void revert();

    ICollisionHandler getCollisionHandler();

    void initAndClear(ListTag data);

    IAccessor getAccessor();

    IWriteableAccessor getWriteableAccessor();

    boolean diff();
}