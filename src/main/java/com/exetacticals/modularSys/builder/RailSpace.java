package com.exetacticals.modularSys.builder;

import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ModuleRegister;
import com.exetacticals.modularSys.IVoxel;
import com.exetacticals.modularSys.modules.IVoxelHandlerModule;
import com.exetacticals.modularSys.slot.IRail;
import com.exetacticals.modularSys.slot.MLokRail;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public final class RailSpace {
    private static final float MIN_FOOTPRINT = 0.085f;
    private static final float CLEARANCE = 0.008f;
    private static final float EPS = 0.0001f;

    private RailSpace() {}

    public static boolean hasRoom(WorkSpace workspace, SlotInstance slot) {
        if (slot == null || slot.isHidden()
                || workspace.getNodes(slot).size() >= slot.getSlot().maxCapacity()) return false;
        if (!(slot.getSlot() instanceof IRail)) return true;
        for (IModular module : ModuleRegister.all().values()) {
            if (module.getBindItem() != null && slot.accepts(module)
                    && findPosition(workspace, slot, module) >= 0f) return true;
        }
        return false;
    }

    public static float findPosition(WorkSpace workspace, SlotInstance slot, IModular module) {
        if (slot == null || module == null || !slot.accepts(module)
                || workspace.getNodes(slot).size() >= slot.getSlot().maxCapacity())
            return -1f;
        if (!(slot.getSlot() instanceof IRail rail)) return 0f;

        if (workspace.getNodes(slot).isEmpty())
            return rail.getNormalizedOriginOffest();

        WorkSpace probe = workspace.deepCopy();
        Node parent = workspace.getSlotParent(slot);
        if (parent == null) return -1f;
        Unit copiedParent = probe.findUnitById(parent.getID()).orElse(null);
        Node copiedParentNode = copiedParent == null ? null : probe.getNode(copiedParent);
        SlotInstance copiedSlot = copiedParentNode == null
                ? null : copiedParentNode.getSlotByName(slot.slotName());
        if (copiedSlot == null) return -1f;

        Unit candidate = probe.addChild(copiedSlot, module).orElse(null);
        if (candidate == null) return -1f;
        CollisionHandler collision = new CollisionHandler(probe);
        Accessor writable = new Accessor(probe, true);
        Accessor readOnly = new Accessor(probe);

        float[] positions = positions(rail);
        for (float position : positions) {
            rail.setChildPosition(candidate, writable, position);
            probe.increaseMutateId();
            collision.update();
            Pair<Boolean, Boolean> boundary = collision.collisionWithRailBoundary(candidate);
            if (boundary.getLeft() || boundary.getRight()
                    || !collision.collision(candidate).isEmpty()) continue;
            if (overlapsRailFootprints(candidate, probe.getNodes(copiedSlot),
                    rail, readOnly)) continue;
            return position;
        }
        return -1f;
    }

    private static float[] positions(IRail rail) {
        if (rail instanceof MLokRail mlok)
            return mlok.getNormalizedDiscreteNodes().clone();

        List<Float> values = new ArrayList<>();
        float origin = rail.getNormalizedOriginOffest();
        values.add(origin);
        for (int i = 0; i <= 64; i++) {
            float p = i / 64f;
            if (Math.abs(p - origin) > EPS) values.add(p);
        }
        values.sort((a, b) -> Float.compare(Math.abs(a - origin), Math.abs(b - origin)));
        float[] output = new float[values.size()];
        for (int i = 0; i < values.size(); i++) output[i] = values.get(i);
        return output;
    }

    private static boolean overlapsRailFootprints(Unit candidate, List<Node> siblings,
                                                   IRail rail, Accessor accessor) {
        float[] candidateSpan = span(candidate, accessor);
        for (Node existing : siblings) {
            Unit other = existing.getUnit();
            if (other == candidate) continue;
            float[] otherSpan = span(other, accessor);
            if (candidateSpan[0] < otherSpan[1] + CLEARANCE
                    && candidateSpan[1] > otherSpan[0] - CLEARANCE)
                return true;
        }
        float half = rail.getLength();
        float rear = rail.getNormalizedOriginOffest() * half;
        float front = -(1f - rail.getNormalizedOriginOffest()) * half;
        return candidateSpan[0] < front - EPS || candidateSpan[1] > rear + EPS;
    }

    private static float[] span(Unit unit, Accessor accessor) {
        float min = -MIN_FOOTPRINT * .5f;
        float max = MIN_FOOTPRINT * .5f;
        if (unit.getModule() instanceof IVoxelHandlerModule voxelModule
                && voxelModule.getHandler() != null) {
            IVoxel voxel = voxelModule.getHandler().getVoxel(unit, accessor);
            if (voxel != null) {
                AABB box = voxel.getBoundingBox();
                if (Double.isFinite(box.minZ) && Double.isFinite(box.maxZ)) {
                    min = (float) box.minZ;
                    max = (float) box.maxZ;
                    float center = (min + max) * .5f;
                    if (max - min < MIN_FOOTPRINT) {
                        min = center - MIN_FOOTPRINT * .5f;
                        max = center + MIN_FOOTPRINT * .5f;
                    }
                }
            }
        }
        return new float[] { min + unit.getZOffset(), max + unit.getZOffset() };
    }
}
