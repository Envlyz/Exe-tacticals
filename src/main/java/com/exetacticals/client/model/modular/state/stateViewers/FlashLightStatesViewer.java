package com.exetacticals.client.model.modular.state.stateViewers;

import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.client.model.modular.state.StateViewer;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.views.IFlashLightView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlashLightStatesViewer extends StateViewer<IFlashLightView> {
    public FlashLightStatesViewer(IFlashLightView view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {

    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {

    }
}
