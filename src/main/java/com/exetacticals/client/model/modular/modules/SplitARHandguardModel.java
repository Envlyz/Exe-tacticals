package com.exetacticals.client.model.modular.modules;

import com.exetacticals.GCR;
import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.ArmHandlerModel;
import com.exetacticals.client.model.modular.state.IStateViewer;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.impl.SplitARHandguard;
import com.exetacticals.modularSys.modules.views.IStateView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplitARHandguardModel extends ArmHandlerModel<IStateView> {

    public SplitARHandguardModel(MeshModelData root) {
        super(root, IStateViewer.EMPTY, GCR.RL(""));
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {
        super.updateBoneRenderStatus(context);
        int currentParam = context.getCurrentParam(SplitARHandguard.HIDE_LOWER_PART_PARAM_KEY);
        getBone("lower").renderStatus.visible = currentParam != 1;
    }
}
