package com.exetacticals.client.model.modular.state.stateViewers;

import com.exetacticals.modularSys.modules.views.IGP25View;

public class GP25Viewer extends GrenadeLauncherViewer<IGP25View> {

    public GP25Viewer(IGP25View view) {
        super(view);
    }

    @Override
    public void onRegisterStateMapping() {
        addStateMapping(BASE_STATE, "exe_tacticals:gp25_base", DEFAULT_SCALE, 0);
        addStateMapping(EMPTY_STATE, "exe_tacticals:gp25_empty", DEFAULT_SCALE, 0);
    }
}
