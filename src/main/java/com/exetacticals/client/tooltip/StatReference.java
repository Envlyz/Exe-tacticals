package com.exetacticals.client.tooltip;

import com.exetacticals.GCR;
import com.exetacticals.items.tooltip.StatRow;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ModuleRegister;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class StatReference {
    private static final float HEADROOM = 1.15f;
    private static Map<String, Float> max;

    private StatReference() {
    }

    static float max(String group, String key) {
        if (max == null) {
            max = build();
        }
        return max.getOrDefault(group + "/" + key, 0f);
    }

    private static Map<String, Float> build() {
        Map<String, Float> result = new HashMap<>();
        for (IModular module : ModuleRegister.all().values()) {
            if (module.getBindItem() == null) {
                continue;
            }
            List<StatRow> rows = new ArrayList<>();
            try {
                module.appendStatRows(new ItemStack(module.getBindItem()), rows);
            } catch (RuntimeException e) {
                GCR.LOGGER.debug("stat reference: {} failed", module.getID(), e);
                continue;
            }
            for (StatRow row : rows) {
                if (row.kind() == StatRow.Kind.ABSOLUTE) {
                    result.merge(row.group() + "/" + row.key(), Math.abs(row.value()) * HEADROOM, Math::max);
                }
            }
        }
        return result;
    }
}
