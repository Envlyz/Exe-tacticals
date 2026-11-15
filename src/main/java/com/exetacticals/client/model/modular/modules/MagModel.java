package com.exetacticals.client.model.modular.modules;

import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.ModularModel;
import com.exetacticals.client.model.modular.state.IStateViewer;
import com.exetacticals.client.model.modular.state.IStateViewerModel;
import com.exetacticals.client.model.modular.state.stateViewers.CommonMagViewer;
import com.exetacticals.modularSys.modules.views.IAmmoSourceView;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagModel extends ModularModel implements IStateViewerModel<IAmmoSourceView> {
    protected CommonMagViewer viewer;

    public MagModel(MeshModelData root, ResourceLocation name, CommonMagViewer viewer) {
        super(root, name);
        this.viewer = viewer;
    }

    @Override
    public IStateViewer<IAmmoSourceView> getViewer() {
        return viewer;
    }
}
