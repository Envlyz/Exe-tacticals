package com.exetacticals.client.model.modular;

import com.exetacticals.modularSys.IModular;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ModuleModelRegister {
    private static final Map<String, IModularModel> ID_TO_MODEL = new HashMap<>();
    private static final Map<IModular, IModularModel> MODULE_TO_MODEL = new HashMap<>();

    public static boolean register(IModular module, IModularModel model) {
        String id = module.getID();
        if (ID_TO_MODEL.containsKey(id) || MODULE_TO_MODEL.containsKey(module)) {
            return false;
        }
        ID_TO_MODEL.put(id, model);
        MODULE_TO_MODEL.put(module, model);
        return true;
    }

    @Nullable
    public static IModularModel remove(IModular module) {
        IModularModel removed = MODULE_TO_MODEL.remove(module);
        if (removed != null) {
            ID_TO_MODEL.remove(module.getID(), removed);
        }
        return removed;
    }

    public static void visitAll(Consumer<IModularModel> visitor) {
        ID_TO_MODEL.values().forEach(visitor);
    }

    public static IModularModel get(IModular module) {
        return MODULE_TO_MODEL.get(module);
    }

    public static IModularModel getByID(String id) {
        return ID_TO_MODEL.get(id);
    }

}
