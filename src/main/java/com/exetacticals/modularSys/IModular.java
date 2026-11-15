package com.exetacticals.modularSys;

import com.exetacticals.IJsonSync;
import com.exetacticals.items.ModuleItem;
import com.exetacticals.modularSys.builder.IAccessor;
import com.exetacticals.modularSys.builder.IWriteableAccessor;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.builder.ValidateResult;
import com.exetacticals.modularSys.modules.gunProperties.PropertiesAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;


public interface IModular extends IJsonSync {

    void modifyProperties(PropertiesAccessor accessor);

    String getID();

    String getSimpleID();

    Set<String> getTags();

    boolean hasTag(String tag);

    boolean fixedPosition();

    void validate(IAccessor accessor, Unit thisUnit, ValidateResult result);

    void onMutated(IWriteableAccessor accessor, Unit thisUnit);

    boolean bindItem(ModuleItem<?> item);

    ModuleItem<?> getBindItem();

    Direction getDirection();

    void finalizeInit();

    float getWeight();

    void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltipComponents, TooltipFlag tooltipFlag);

    /** Характеристики для тултипа полосками ({@link com.exetacticals.items.tooltip.StatTooltip}). */
    default void appendStatRows(ItemStack stack, List<com.exetacticals.items.tooltip.StatRow> rows) {
    }
}
