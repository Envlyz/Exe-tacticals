package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ISlotProvider;
import com.exetacticals.modularSys.ModuleRegister;
import com.exetacticals.modularSys.builder.IAccessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.builder.ValidateResult;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ZenitcoUpperHandguard extends SlotProviderVoxelModule{
    static List<String> allSuitable = null;

    public ZenitcoUpperHandguard(ResourceLocation id, boolean fixedPosition, float weight, Direction direction, ISlotProvider slotProvider, IVoxelHandler handler) {
        super(id, fixedPosition, weight, direction, slotProvider, handler);
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();
        if (allSuitable != null) {
            return;
        }
        allSuitable = new ArrayList<>();
        Collection<IModular> values = ModuleRegister.all().values();
        for (IModular m : values) {
            if (m.hasTag("zenitco")) {
                allSuitable.add(m.getID());
            }
        }
    }

    @Override
    public void validate(IAccessor accessor, Unit thisUnit, ValidateResult result) {
        super.validate(accessor, thisUnit, result);
        accessor.getSlot(accessor.root(), "HANDGUARD_LOWER").ifPresent(slot -> {
            List<Unit> slotChildren = accessor.getSlotChildren(slot);
            boolean found = false;
            for (Unit unit : slotChildren) {
                if (unit.hasTag("zenitco")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String string = Component.translatable("validate.result.requires").getString();
                String thisName = Component.translatable(getID()).getString();
                StringBuilder list = new StringBuilder();
                for (String s : allSuitable) {
                    list.append(Component.translatable(s).getString()).append(", ");
                }
                String msg = string.replace("$name", thisName).replace("$list", list.toString());
                result.recordError(msg);
            }
        });
    }
}
