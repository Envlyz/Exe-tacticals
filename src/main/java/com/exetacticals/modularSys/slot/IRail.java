package com.exetacticals.modularSys.slot;

import com.exetacticals.modularSys.builder.Accessor;
import com.exetacticals.modularSys.builder.IWriteableAccessor;
import com.exetacticals.modularSys.builder.Unit;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

public interface IRail {
    float getLength();

    float getNormalizedOriginOffest();

    float getNormalizedChildPosition(Unit instance);

    Pair<Boolean, Boolean> collideWithBoundary(Unit child, Matrix4f slotPose, Accessor accessor);

    void setChildPosition(Unit selectedNode, IWriteableAccessor accessor, float normalizedPos);
}
