package com.exetacticals.client.model.modular.modules;

import com.exetacticals.GCR;
import com.exetacticals.client.model.Bone;
import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.ArmHandlerModel;
import com.exetacticals.client.model.modular.state.IStateViewer;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.views.IStateView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MLokFitGripModel extends ArmHandlerModel<IStateView> {
    private final Bone mLokBone;
    private final Bone main;

    public MLokFitGripModel(MeshModelData root) {
        super(root, IStateViewer.EMPTY, GCR.RL(""));
        mLokBone = getOrThrow("M_LOK");
        main = getOrThrow("MAIN");
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {
        int currentParam = context.getCurrentParam(IVoxelHandler.VOXEL_INDEX_PARAM_KEY);
        if (currentParam == 1) {
            main.y -= mLokBone.y;
        }
        super.updateBoneRenderStatus(context);
        if (currentParam != 1) {
            mLokBone.renderStatus.visible = false;
        }
    }
}
