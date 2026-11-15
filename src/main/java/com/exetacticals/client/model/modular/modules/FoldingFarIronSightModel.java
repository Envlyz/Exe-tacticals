package com.exetacticals.client.model.modular.modules;

import com.exetacticals.client.model.Bone;
import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.ModularModel;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.FoldingIronSightVoxelHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FoldingFarIronSightModel extends ModularModel {
    private final Bone fold;
    private final float foldRot;

    public FoldingFarIronSightModel(MeshModelData root, ResourceLocation name, float foldRotDeg) {
        super(root, name);
        this.fold = getOrThrow("FOLD");
        this.foldRot = (float) Math.toRadians(foldRotDeg);
    }

    @Override
    public void updateBoneRenderStatus(ModuleRenderContext context) {
        int currentParam = context.getCurrentParam(FoldingIronSightVoxelHandler.FOLD_SIGHT_PARAM_KEY);
        if (currentParam == 1) {
            fold.xRot += foldRot;
        }
        super.updateBoneRenderStatus(context);
    }
}
