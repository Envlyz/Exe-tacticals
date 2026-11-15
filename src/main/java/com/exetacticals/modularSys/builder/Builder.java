package com.exetacticals.modularSys.builder;

import com.exetacticals.GCR;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ISlotProviderModular;
import com.exetacticals.modularSys.ModuleRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class Builder implements IBuilder{
    private WorkSpace workSpace;
    private WorkSpace warehouse;
    private CollisionHandler collisionHandler;

    public Builder() {
        workSpace = new WorkSpace();
    }

    public Builder(IModular modular) {
        if (modular instanceof ISlotProviderModular slotProviderModular) {
            root(slotProviderModular);
        } else {
            throw new IllegalArgumentException("root module must implement ISlotProviderModular");
        }
    }

    @Override
    public <T extends ISlotProviderModular & IModular> IBuilder root(T modular) {
        if (workSpace == null) {
            workSpace = new WorkSpace(modular);
            warehouse = workSpace.deepCopy();
            collisionHandler = new CollisionHandler(workSpace);
        } else if (workSpace.isEmpty()) {
            workSpace.setRoot(modular);
            warehouse = workSpace.deepCopy();
            collisionHandler = new CollisionHandler(workSpace);
        }
        return this;
    }

    @Override
    public IWorkSpace getWorkspace() {
        return workSpace;
    }

    @Override
    public IReadOnlyTree getWarehouse() {
        return warehouse;
    }

    @Override
    public ValidateResult checkWorkspace(boolean checkCollision) {
        if (workSpace == null) {
            throw new IllegalStateException("workspace is not setup");
        }
        Accessor accessor = new Accessor(workSpace);
        return check(accessor, checkCollision);
    }

    private ValidateResult check(Accessor accessor, boolean checkCollision) {
        accessor.setWriteable(false);
        if (checkCollision) {
            if (collisionHandler == null) {
                collisionHandler = new CollisionHandler(workSpace);
            }
            collisionHandler.update();
        }
        List<Unit> sequencedUnits = accessor.getSequencedUnits();
        ValidateResult result = new ValidateResult();
        for (Unit unit : sequencedUnits) {
            result.setSource(unit);
            if (unit != accessor.root()) {
                Optional<SlotInstance> belongsTo = accessor.getBelongsTo(unit);
                belongsTo.ifPresentOrElse(slot -> {
                    if (slot.isHidden()) {
                        result.recordError(unit, "belongs to hidden module");
                    }
                }, () -> result.recordError(unit, "belongs to null"));
            }
            if (checkCollision) {
                Pair<Boolean, Boolean> booleanBooleanPair = collisionHandler.collisionWithRailBoundary(unit);
                if (booleanBooleanPair.getLeft() || booleanBooleanPair.getRight()) {
                    String msg = Component.translatable("validate.result.out_of_boundary").getString().replace("$name", unit.getModuleId());
                    result.recordError(unit, msg);
                }
                List<Unit> collision = collisionHandler.collision(unit, accessor.getHide());
                for (Unit collisionUnit : collision) {
                    if (accessor.contains(collisionUnit)) {
                        String msg = Component.translatable("validate.result.collision").getString()
                                        .replace("$name1", unit.getModuleId())
                                        .replace("$name2", collisionUnit.getModuleId());
                        result.recordError(collisionUnit, msg);
                    }
                }
            }
            unit.getModule().validate(accessor, unit, result);
        }
        return result;
    }

    public Pair<IReadOnlyTree, List<ValidateResult>> pruneWorkspaceToCommittable() {
        Accessor accessor = new Accessor(workSpace);
        accessor.setWriteable(true);
        callMutatedHook(workSpace, accessor);
        ValidateResult validateResult = checkWorkspace(true);
        if (validateResult.isCommitAllowed()) {
            return Pair.of(workSpace.deepCopy(), List.of(validateResult));
        }
        Set<Unit> illegalSet = new HashSet<>();
        Map<Unit, Integer> timeLine = new HashMap<>();
        int index = 0;
        for (Unit unit : accessor.getSequencedUnits()) {
            timeLine.put(unit, index);
            index ++;
        }
        List<ValidateResult> checkResults = new ArrayList<>();
        while (true) {
            int illegalCountBefore = illegalSet.size();
            ValidateResult check = check(accessor, true);
            checkResults.add(check);
            if (check.isCommitAllowed()) {
                accessor.setWriteable(true);
                return Pair.of(createFromAccessor(accessor), checkResults);
            }
            List<ValidationError> issues = check.getIssues();
            for (ValidationError issue : issues) {
                if (issue.getLevel() == ErrorLevel.WARNING) {
                    continue;
                }
                Unit source = issue.source();
                Unit target = issue.target();
                if (source == accessor.root() && target == null) {
                    continue;
                }
                if (target == null) {
                    illegalSet.add(source);
                } else {
                    if (timeLine.get(source) > timeLine.get(target)) {
                        illegalSet.add(source);
                    } else {
                        illegalSet.add(target);
                    }
                }
            }
            if (illegalSet.size() == illegalCountBefore) {
                throw new IllegalStateException(
                        "can not cut workspace to committable, there are some conflicts or errors that can't be solved!");
            }
            accessor.setHideUnits(illegalSet);
            callMutatedHook(workSpace, accessor);
        }
    }

    private WorkSpace createFromAccessor(Accessor accessor) {
        List<Unit> sequencedUnits = accessor.getSequencedUnits();
        if (sequencedUnits.isEmpty()) {
            throw new IllegalStateException("root unit unfound");
        }
        WorkSpace workSpace = new WorkSpace();
        Map<Unit, Unit> oldToNew = new HashMap<>();
        for (Unit unit : sequencedUnits) {
            if (workSpace.getRootUnit() == null) {
                Unit root = workSpace.setRoot(unit.getModule());
                oldToNew.put(unit, root);
                continue;
            }
            accessor.getParent(unit).ifPresent(parent -> {
                Unit newParent = oldToNew.get(parent);
                accessor.getBelongsTo(unit).
                        flatMap(slot -> workSpace.addChild(newParent, slot.getSlot(), unit))
                        .ifPresent(res -> oldToNew.put(unit, res));
            });
        }
        if (workSpace.getRootUnit() == null) {
            throw new IllegalStateException("root unit unfound");
        }
        callMutatedHook(workSpace, accessor);
        return workSpace;
    }

    static void callMutatedHook(WorkSpace workSpace, IWriteableAccessor accessor) {
        Collection<Node> nodes = workSpace.getNodes();
        for (Node node : nodes) {
            node.getUnit().flush();
            node.getSlots().forEach(SlotInstance::flush);
        }
        for (Node node : nodes) {
            node.getModule().onMutated(accessor, node.getUnit());
        }
    }

    @Override
    public List<ValidateResult> commit() {
        if (workSpace != null) {
            Pair<IReadOnlyTree, List<ValidateResult>> res = pruneWorkspaceToCommittable();
            workSpace = (WorkSpace) res.getLeft();
            collisionHandler.setAndUpdate(workSpace);
            warehouse = workSpace.deepCopy();
            return res.getRight();
        } else {
            throw new IllegalStateException("workspace is not setup");
        }
    }

    @Override
    public void revert() {
        if (warehouse != null) {
            workSpace = warehouse.deepCopy();
            collisionHandler.setAndUpdate(workSpace);
        }
    }

    @Override
    public ICollisionHandler getCollisionHandler() {
        return collisionHandler;
    }

    @Override
    public void initAndClear(ListTag data) {
        if (data.isEmpty()) {
            return;
        }
        if (!workSpace.isEmpty()) {
            return;
        }
        Set<CompoundTag> remove = new HashSet<>();
        Map<String, Unit> idToUnit = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i);
            String moduleId = tag.getString(Unit.MODULE_ID);
            String parentId = tag.getString(Unit.PARENT_ID);
            String id = tag.getString(Unit.IN_TIME_ID);
            String slotName = tag.getString(Unit.SLOT_NAME);

            if (workSpace.isEmpty()) {
                IModular iModular = ModuleRegister.get(moduleId);
                if (iModular == null) {
                    GCR.LOGGER.warn("The module: {} does not exist", moduleId);
                    return;
                }
                if (!(iModular instanceof ISlotProviderModular slotProviderModular)) {
                    GCR.LOGGER.warn("The module: {} does not support slot", moduleId);
                    return;
                }
                Unit root = root(slotProviderModular).getAccessor().root();
                collisionHandler = new CollisionHandler(workSpace);
                root.read(tag);
                idToUnit.put(id, root);
                remove.add(tag);
            } else {
                Unit parent = idToUnit.get(parentId);
                if (parent == null) {
                    continue;
                }
                workSpace.addChild(parent, slotName, moduleId).ifPresent(unit -> {
                    unit.read(tag);
                    idToUnit.put(id, unit);
                    remove.add(tag);
                });
            }
        }
        data.removeAll(remove);
    }

    @Override
    public IAccessor getAccessor() {
        return new Accessor(this.workSpace);
    }

    @Override
    public IWriteableAccessor getWriteableAccessor() {
        Accessor accessor = new Accessor(this.workSpace);
        accessor.setWriteable(true);
        return accessor;
    }

    @Override
    public boolean diff() {
        if (workSpace == null && warehouse == null) {
            return false;
        }
        if (workSpace != null && warehouse != null) {
            List<Unit> warehouseUnits = warehouse.getSequencedUnits();
            List<Unit> workSpaceUnits = workSpace.getSequencedUnits();
            if (warehouseUnits.size() != workSpaceUnits.size()) {
                return true;
            }
            for (int i = 0; i < warehouseUnits.size(); i++) {
                Unit warehouseUnit = warehouseUnits.get(i);
                Unit workSpaceUnit = workSpaceUnits.get(i);
                Node warehouseNode = warehouse.getNode(warehouseUnit);
                Node workSpaceNode = workSpace.getNode(workSpaceUnit);
                if (Node.diff(warehouseNode, workSpaceNode)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public static Pair<List<CompoundTag>, List<CompoundTag>> diffByModule(ListTag original, ListTag fin) {
        Map<String, Integer> origCount = new HashMap<>(Math.max(8, original.size()));
        for (int i = 0; i < original.size(); i++) {
            CompoundTag tag = original.getCompound(i);
            if (!tag.contains("module")) {
                continue;
            }
            String module = tag.getString("module");
            origCount.merge(module, 1, Integer::sum);
        }

        Map<String, Integer> matchedCount = new HashMap<>();
        List<CompoundTag> inc = new ArrayList<>();

        for (int i = 0; i < fin.size(); i++) {
            CompoundTag tag = fin.getCompound(i);
            if (!tag.contains("module")) {
                inc.add(tag);
                continue;
            }
            String module = tag.getString("module");
            int available = origCount.getOrDefault(module, 0) - matchedCount.getOrDefault(module, 0);
            if (available > 0) {
                matchedCount.merge(module, 1, Integer::sum);
            } else {
                inc.add(tag);
            }
        }

        List<CompoundTag> dec = new ArrayList<>();
        Map<String, Integer> matchedRemaining = new HashMap<>(matchedCount);
        for (int i = 0; i < original.size(); i++) {
            CompoundTag tag = original.getCompound(i);
            if (!tag.contains("module")) {
                continue;
            }
            String module = tag.getString("module");
            int left = matchedRemaining.getOrDefault(module, 0);
            if (left > 0) {
                matchedRemaining.put(module, left - 1);
            } else {
                dec.add(tag);
            }
        }

        return Pair.of(inc, dec);
    }

}
