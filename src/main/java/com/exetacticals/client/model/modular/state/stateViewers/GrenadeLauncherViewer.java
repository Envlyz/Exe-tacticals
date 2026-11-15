package com.exetacticals.client.model.modular.state.stateViewers;

import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.client.model.modular.state.StateViewer;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.views.IGrenadeLauncherView;

public class GrenadeLauncherViewer<T extends IGrenadeLauncherView> extends StateViewer<T> {

    protected static final String BASE_STATE = "base";
    protected static final String EMPTY_STATE = "empty";

    public GrenadeLauncherViewer(T view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        doPose(BASE_STATE, animated, context);
        if (IGrenadeLauncherView.CHAMBER_EMPTY.equals(getStateView().getChamberStatus(states))) {
            doPose(EMPTY_STATE, animated, context);
        }
    }
}
