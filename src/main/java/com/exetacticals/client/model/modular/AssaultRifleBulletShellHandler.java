package com.exetacticals.client.model.modular;

import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.client.render.fx.bulletShell.BulletShellDisplay;
import com.exetacticals.modularSys.modules.views.IGunView;

public class AssaultRifleBulletShellHandler extends BulletShellHandler<IGunView>{
    public AssaultRifleBulletShellHandler(IModularModel model, BulletShellDisplay display) {
        super(model, display);
    }

    @Override
    public boolean shouldThrowBulletShell(IGunView view, ReadOnlyTag states) {
        return !view.stuck(states);
    }
}
