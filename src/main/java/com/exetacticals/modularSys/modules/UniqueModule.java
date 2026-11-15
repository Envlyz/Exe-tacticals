package com.exetacticals.modularSys.modules;

import com.google.gson.JsonObject;
import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.builder.IAccessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.builder.ValidateResult;
import com.exetacticals.modularSys.modules.impl.AttachmentModule;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class UniqueModule extends AttachmentModule {
    public UniqueModule(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
        addTags("unique");
    }

    @Override
    public void validate(IAccessor accessor, Unit thisUnit, ValidateResult result) {
        accessor.first(unit -> unit != thisUnit && unit.getModuleId().equals(thisUnit.getModuleId()) && unit.hasTag("unique"))
                .ifPresent(unit -> {
                    String moduleId = unit.getModuleId();
                    String name = Component.translatable(moduleId).getString();
                    String msg = Component.translatable("validate.result.unique").getString().replace("$name", name);
                    result.recordError(unit, msg);
                });
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }
}
