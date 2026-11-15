package com.exetacticals.client.model.modular.animation.controllers;

import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.modules.GP25Model;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * GP25 的动画控制器。动画资源与 M203 不同，其余行为继承
 * {@link GrenadeLauncherController}。
 */
@OnlyIn(Dist.CLIENT)
public class GP25Controller extends GrenadeLauncherController<GP25Model> {

    @Override
    protected String getCheckAnimationPath() {
        return "exe_tacticals:ak74m_check_grenade_gp25.g";
    }

    @Override
    protected String getReloadAnimationPath() {
        return "exe_tacticals:ak74m_reload_grenade_gp25.g";
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof GP25Model;
    }
}
