package com.exetacticals.compat;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.entity.PartEntity;

public final class OwnMultipartHitFilter {
    private OwnMultipartHitFilter() {}

    public static boolean isOwnerOrOwnedPart(Entity candidate, Entity owner) {
        if (candidate == null || owner == null) {
            return false;
        }
        if (candidate == owner) {
            return true;
        }
        return candidate instanceof PartEntity<?> part && part.getParent() == owner;
    }
}
