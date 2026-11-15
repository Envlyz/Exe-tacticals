package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.items.tooltip.StatRow;
import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.modules.Module;
import com.exetacticals.modularSys.modules.gunProperties.IProp;
import com.exetacticals.modularSys.modules.gunProperties.IProperties;
import com.exetacticals.modularSys.modules.gunProperties.PropertiesAccessor;
import com.exetacticals.modularSys.modules.gunProperties.PropertiesDummies;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AttachmentModule extends Module {
    protected Map<Class<? extends IProperties>, List<PropModifier>> modifiers = new HashMap<>();

    public AttachmentModule(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // вес и модификаторы показываются полосками: см. appendStatRows
    }

    @Override
    public void appendStatRows(ItemStack stack, List<StatRow> rows) {
        rows.add(StatRow.absolute("module", "weight", Component.translatable("exe_tacticals:prop.base.weight").getString(),
                getWeight(), String.format("%.2f", getWeight())));
        for (List<PropModifier> list : modifiers.values()) {
            for (PropModifier modifier : list) {
                if (Math.abs(modifier.value) <= 1e-5f) {
                    continue;
                }
                float percent = (modifier.inc ? modifier.value : -modifier.value) * 100f;
                rows.add(StatRow.delta(modifier.getKey(), Component.literal(modifier.getFullName()), percent));
            }
        }
    }

    @Override
    public void modifyProperties(PropertiesAccessor accessor) {
        super.modifyProperties(accessor);
        for (Class<? extends IProperties> clazz : modifiers.keySet()) {
            for (PropModifier entry : modifiers.get(clazz)) {
                accessor.using(clazz, prop -> {
                    IProp apply = entry.getter.apply(prop);
                    if (entry.inc) {
                        prop.inc(apply, entry.value);
                    } else {
                        prop.dec(apply, entry.value);
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends IProperties> void defPropInc(Class<T> clazz, Function<T, IProp> propGetter, float val) {
        modifiers
                .computeIfAbsent(clazz, k -> new ArrayList<>())
                .add(new PropModifier(
                        clazz,
                        (Function<IProperties, IProp>) propGetter,
                        val,
                        true
                ));
    }

    protected void clearPropModifiers() {
        modifiers.clear();
    }

    protected <T extends IProperties> void clearPropModifiers(Class<T> clazz) {
        modifiers.remove(clazz);
    }

    @SuppressWarnings("unchecked")
    protected <T extends IProperties> void defPropDec(Class<T> clazz, Function<T, IProp> propGetter, float val) {
        modifiers
                .computeIfAbsent(clazz, k -> new ArrayList<>())
                .add(new PropModifier(
                        clazz,
                        (Function<IProperties, IProp>) propGetter,
                        val,
                        false
                ));
    }

    protected static class PropModifier {
        final Class<?> type;
        final Function<IProperties, IProp> getter;
        final float value;
        final boolean inc;

        PropModifier(Class<?> type, Function<IProperties, IProp> getter, float value, boolean inc) {
            this.type = type;
            this.getter = getter;
            this.value = value;
            this.inc = inc;
        }

        public String getTooltipMsg() {
            String fullName = getFullName();
            float val = inc ? value : -value;
            val *= 100f;
            String format = String.format("%.2f", val);
            if (val >= 0) {
                format = "+" + format;
            }
            return Component.translatable("tooltip.prop.modifier").getString()
                    .replace("$name", fullName)
                    .replace("$amount", format);
        }

        public String getKey() {
            IProperties dummy = PropertiesDummies.getDummy(type);
            return dummy == null ? "" : getter.apply(dummy).getKey();
        }

        public String getFullName() {
            IProperties dummy = PropertiesDummies.getDummy(type);
            if (dummy != null) {
                IProperties properties = dummy;
                IProp apply = getter.apply(properties);
                return apply.getFullName();
            }
            return "UNKNOWN";
        }
    }
}
