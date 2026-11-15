package com.exetacticals.client.model.modular.modules;

import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.ISightModel;
import com.exetacticals.client.model.modular.ModularModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SightModel extends ModularModel implements ISightModel {
    public SightModel(MeshModelData root, ResourceLocation name) {
        super(root, name);
        getOrThrow(ISightModel.DEFAULT_BONE_NAME);
    }
}
