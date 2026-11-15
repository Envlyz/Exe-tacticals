package com.exetacticals.client.model.modular.modules;

import com.exetacticals.GCR;
import com.exetacticals.client.GunEffect;
import com.exetacticals.client.model.Bone;
import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.*;
import com.exetacticals.client.model.modular.state.stateViewers.AKViewer;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.client.render.fx.bulletShell.BulletShellDisplay;
import com.exetacticals.client.render.fx.muzzleFlash.MuzzleFlash;
import com.exetacticals.client.render.fx.muzzleSmoke.fast.FastMuzzleSmoke;
import com.exetacticals.client.render.fx.muzzleSmoke.slow.SlowSmokeEntry;
import com.exetacticals.modularSys.modules.views.AKView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AKModel extends ArmHandlerModel<AKView> implements IBulletShellHandlerModel<AKView>, ISightModel, IGunModel, IMuzzleFlashRendererModel  {
    private final AssaultRifleBulletShellHandler bulletShellHandler;
    private final Bone handHoldPivot;
    private final MuzzleFlashRenderer muzzleFlashRenderer;

    public AKModel(MeshModelData root, BulletShellDisplay display, AKViewer viewer, float muzzleFlashScale, MuzzleFlash muzzleFlash, float smokeScale, FastMuzzleSmoke muzzleSmoke, float flashLightIntensity, SlowSmokeEntry slowSmokeEntry) {
        super(root, viewer, GCR.RL("ak"));
        this.bulletShellHandler = new AssaultRifleBulletShellHandler(this, display);
        this.handHoldPivot = getOrThrow(DEFAULT_HAND_ROT_PIVOT_NAME);
        getOrThrow(ISightModel.DEFAULT_BONE_NAME);
        muzzleFlashRenderer = new MuzzleFlashRenderer(
                new MuzzleEntry("no1", "MUZZLE_FLASH", "MUZZLE", muzzleFlashScale, muzzleFlash, smokeScale, muzzleSmoke, flashLightIntensity)
                        .withSlowSmokeEntry(slowSmokeEntry)
        );
    }

    @Override
    public void render(ModuleRenderContext context) {
        super.render(context);
        muzzleFlashRenderer.onRender(context, this, GunEffect.SHOOT, context.root.id);
    }

    @Override
    public void afterAllRendered(ModuleRenderContext context) {
        super.afterAllRendered(context);
        muzzleFlashRenderer.onAfterAllRendered(context);
    }

    @Override
    public IBulletShellHandler<?> getBulletShellHandler() {
        return bulletShellHandler;
    }

    @Override
    public String getFarthestSightZName(ModuleRenderContext context) {
        return DEFAULT_FARTHEST_SIGHT_Z_NAME;
    }

    @Override
    public String getFarthestSideSightZName(ModuleRenderContext context) {
        return DEFAULT_FARTHEST_SIDE_SIGHT_Z_NAME;
    }

    @Override
    public Bone getHandRotPivot() {
        return handHoldPivot;
    }


    @Override
    public IMuzzleFlashRenderer getRenderer() {
        return muzzleFlashRenderer;
    }
}

