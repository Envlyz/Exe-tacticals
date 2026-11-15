package com.exetacticals.client.model.modular.modules;

import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.MuzzleFlashRenderer;
import com.exetacticals.client.model.modular.state.stateViewers.GP25Viewer;
import com.exetacticals.modularSys.modules.views.IGP25View;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GP25Model extends GrenadeLauncherModel<IGP25View> {

    public GP25Model(MeshModelData root, GP25Viewer viewer, MuzzleFlashRenderer muzzleFlashRenderer) {
        super(root, viewer, muzzleFlashRenderer);
    }
}
