package com.exetacticals.client.model.modular.modules;

import com.exetacticals.GCR;
import com.exetacticals.client.GunEffect;
import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.ArmHandlerModel;
import com.exetacticals.client.model.modular.IMuzzleFlashRenderer;
import com.exetacticals.client.model.modular.IMuzzleFlashRendererModel;
import com.exetacticals.client.model.modular.MuzzleFlashRenderer;
import com.exetacticals.client.model.modular.state.IStateViewer;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.views.IGrenadeLauncherView;

public class GrenadeLauncherModel<T extends IGrenadeLauncherView> extends ArmHandlerModel<T> implements IMuzzleFlashRendererModel {

    private final MuzzleFlashRenderer muzzleFlashRenderer;

    public GrenadeLauncherModel(MeshModelData root, IStateViewer<T> viewer, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, viewer, GCR.RL(""));
        this.muzzleFlashRenderer = muzzleFlashRenderer;
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        muzzleFlashRenderer.onRender(context, this, GunEffect.SHOOT, context.currentRenderNode().id);
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        super.afterAllRendered(context);
        muzzleFlashRenderer.onAfterAllRendered(context);
    }

    @Override
    public IMuzzleFlashRenderer getRenderer() {
        return muzzleFlashRenderer;
    }
}
