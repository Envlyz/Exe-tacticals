package com.exetacticals.client.model.modular.modules;

import com.exetacticals.GCR;
import com.exetacticals.client.GunEffect;
import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.IMuzzleFlashRenderer;
import com.exetacticals.client.model.modular.IMuzzleFlashRendererModel;
import com.exetacticals.client.model.modular.ModularModel;
import com.exetacticals.client.model.modular.MuzzleFlashRenderer;
import com.exetacticals.client.render.ModuleRenderContext;

public class MuzzleFlashRendererModel extends ModularModel implements IMuzzleFlashRendererModel {
    protected final MuzzleFlashRenderer muzzleFlashRenderer;

    public MuzzleFlashRendererModel(MeshModelData root, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, GCR.RL(""));
        this.muzzleFlashRenderer = muzzleFlashRenderer;
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        renderMuzzleFlash(context);
    }

    protected void renderMuzzleFlash(ModuleRenderContext context) {
        muzzleFlashRenderer.onRender(context, this, GunEffect.SHOOT, context.root.id);
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
