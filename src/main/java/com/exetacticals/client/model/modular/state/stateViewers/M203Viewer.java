package com.exetacticals.client.model.modular.state.stateViewers;

import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.views.IM203View;

public class M203Viewer extends GrenadeLauncherViewer<IM203View> {

    protected static final String FULL_STATE = "full";
    protected static final String FIRED_STATE = "fired";

    public M203Viewer(IM203View view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping(BASE_STATE, "exe_tacticals:m203_base", DEFAULT_SCALE, 0);
        addStateMapping(FULL_STATE, "exe_tacticals:m203_full", DEFAULT_SCALE, 0);
        addStateMapping(EMPTY_STATE, "exe_tacticals:m203_empty", DEFAULT_SCALE, 0);
        addStateMapping(FIRED_STATE, "exe_tacticals:m203_fired", DEFAULT_SCALE, 0);
    }

    @Override
    public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {
        super.applyState(animated, context, states);
        String chamberStatus = getStateView().getChamberStatus(states);
        if (IM203View.CHAMBER_LOADED.equals(chamberStatus)) {
            doPose(FULL_STATE, animated, context);
        } else if (IM203View.CHAMBER_FIRED.equals(chamberStatus)) {
            doPose(FIRED_STATE, animated, context);
        }
    }
}
