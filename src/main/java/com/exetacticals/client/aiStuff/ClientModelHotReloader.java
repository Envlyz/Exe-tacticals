package com.exetacticals.client.aiStuff;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.ModelRegistrationManager;
import com.exetacticals.client.model.BufferedBoneMeshModel;
import com.exetacticals.client.model.gltf.io.GltfModelLoader;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.ModuleModelRegister;
import com.exetacticals.client.model.modular.state.IStateViewer;
import com.exetacticals.client.model.modular.state.IStateViewerModel;
import com.exetacticals.client.render.HeatMapTextureManager;
import com.exetacticals.client.render.RenderTypes;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ModuleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientModelHotReloader {

    private static boolean reloading;

    private ClientModelHotReloader() {
    }

    public static boolean reloadModel(String modularId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> reloadModel(modularId));
            return true;
        }
        if (reloading) {
            GCR.LOGGER.warn("[GCR HotReload] reload already in progress, request ignored");
            return false;
        }

        if (modularId == null || modularId.isBlank()) {
            notifyPlayer("用法: /" + ModelReloadCommand.COMMAND + " <" + ModelReloadCommand.ARG_MODULAR_ID + ">");
            return false;
        }
        IModular module = ModuleRegister.get(modularId);
        if (module == null) {
            notifyPlayer("找不到模块: " + modularId);
            return false;
        }
        if (!ModelRegistrationManager.hasRecipe(module)) {
            notifyPlayer("模块 " + modularId + " 没有登记客户端模型");
            return false;
        }

        reloading = true;
        long startedAt = System.nanoTime();
        Client.LOCK.lock();
        try {
            resetModel(module);

            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
            GCR.LOGGER.info("[GCR HotReload] reloaded client model of {} in {} ms", modularId, elapsedMs);
            notifyPlayer("已热重载模型: " + modularId + "，用时 " + elapsedMs + " ms");
            return true;
        } catch (Throwable t) {
            GCR.LOGGER.error("[GCR HotReload] failed to reload client model of {}", modularId, t);
            notifyPlayer("热重载 " + modularId + " 失败: " + t);
            return false;
        } finally {
            Client.LOCK.unlock();
            reloading = false;
        }
    }

    private static void resetModel(IModular module) {
        ResourceLocation modelPath = ModelRegistrationManager.getModelPath(module);
        ResourceLocation texturePath = ModelRegistrationManager.getModelTexture(module);

        IModularModel model = ModelRegistrationManager.createModel(module);
        if (model == null) {
            throw new IllegalStateException("module has no model recipe: " + module.getID());
        }

        IModularModel old = ModuleModelRegister.remove(module);
        ResourceLocation oldHeatMapPath = old == null ? null : old.getHeatMapTexPath();
        if (old instanceof BufferedBoneMeshModel buffered) {
            buffered.release();
        }

        ModuleModelRegister.register(module, model);

        if (texturePath != null) {
            DevSimpleTexture.registerFromDisk(texturePath);
        }
        HeatMapTextureManager.reloadCachedTexture(oldHeatMapPath);
        HeatMapTextureManager.reloadCachedTexture(model.getHeatMapTexPath());

        if (texturePath != null) {
            model.compile(RenderTypes.getMeshCutOut(texturePath));
        }

        if (model instanceof IStateViewerModel<?> stateModel) {
            IStateViewer<?> viewer = stateModel.getViewer();
            if (viewer != null) {
                viewer.onRegisterStateMapping();
            }
        }

        Client.getGunRenderer().invalidateRenderCache();
        Client.WEAPON_STATUS.invalidateModelCache();
    }

    private static void notifyPlayer(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(message));
        }
    }
}
