package com.exetacticals;

import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.AnimationRegister;
import com.exetacticals.client.animation.io.BedrockAnimationLoader;
import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.gltf.io.GltfModelLoader;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.ModuleModelRegister;
import com.exetacticals.client.render.RenderTypes;
import com.exetacticals.modularSys.IModular;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModelRegistrationManager {
    private static final List<Runnable> DEFERRED_COMPILE_TASKS = new ArrayList<>();

    private static final Map<IModular, ModelRecipe<?>> MODEL_RECIPES = new LinkedHashMap<>();

    public record ModelRecipe<T extends IModularModel>(
            ResourceLocation modelPath,
            ResourceLocation texturePath,
            Function<MeshModelData, T> modelFactory
    ) {
    }

    public static <T extends IModularModel> T registerModel(
            Object registryKey,
            String gltfPath,
            String texturePath,
            boolean immediateCompile,
            Function<MeshModelData, T> modelFactory
    ) {
        IModular module = (IModular) registryKey;
        ResourceLocation modelPath = GCR.RL("exe_tacticals", gltfPath);
        ResourceLocation texture = GCR.RL("exe_tacticals", texturePath);

        MeshModelData meshModelData = GltfModelLoader.loadModel(modelPath);

        T model = modelFactory.apply(meshModelData);

        ModuleModelRegister.register(module, model);

        MODEL_RECIPES.put(module, new ModelRecipe<>(modelPath, texture, modelFactory));

        if (immediateCompile) {
            DEFERRED_COMPILE_TASKS.add(() -> model.compile(RenderTypes.getMeshCutOut(texture)));
        }

        return model;
    }

    public static boolean hasRecipe(IModular module) {
        return MODEL_RECIPES.containsKey(module);
    }

    @Nullable
    public static ResourceLocation getModelPath(IModular module) {
        ModelRecipe<?> recipe = MODEL_RECIPES.get(module);
        return recipe == null ? null : recipe.modelPath();
    }

    @Nullable
    public static ResourceLocation getModelTexture(IModular module) {
        ModelRecipe<?> recipe = MODEL_RECIPES.get(module);
        return recipe == null ? null : recipe.texturePath();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static IModularModel createModel(IModular module) {
        ModelRecipe<IModularModel> recipe = (ModelRecipe<IModularModel>) MODEL_RECIPES.get(module);
        if (recipe == null) {
            return null;
        }
        MeshModelData meshModelData = GltfModelLoader.loadModel(recipe.modelPath());
        return recipe.modelFactory().apply(meshModelData);
    }

    public static void addDeferredCompileTask(Runnable task) {
        DEFERRED_COMPILE_TASKS.add(task);
    }

    public static Map<String, AnimationDef> loadAndRegisterAnimations(String jsonPath, Map<String, String> mapping) {
        Map<String, AnimationDef> anims = BedrockAnimationLoader.loadAnimationCollection(GCR.RL("exe_tacticals", jsonPath), true);
        mapping.forEach((jsonKey, regKey) -> {
            AnimationDef def = anims.get(jsonKey);
            if (def != null) {
                AnimationRegister.register(GCR.RL(regKey), def);
            }
        });
        return anims;
    }

    public static void compileAll() {
        for (Runnable task : DEFERRED_COMPILE_TASKS) {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
