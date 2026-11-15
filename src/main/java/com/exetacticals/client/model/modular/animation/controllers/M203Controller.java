package com.exetacticals.client.model.modular.animation.controllers;

import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.modules.M203Model;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class M203Controller extends GrenadeLauncherController<M203Model> {

    @Override
    protected String getCheckAnimationPath() {
        return "exe_tacticals:m4a1_check_grenade_m203.g";
    }

    @Override
    protected String getReloadAnimationPath() {
        return "exe_tacticals:m4a1_reload_grenade_m203.g";
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof M203Model;
    }
}
