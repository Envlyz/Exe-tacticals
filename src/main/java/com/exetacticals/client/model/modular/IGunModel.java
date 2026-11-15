package com.exetacticals.client.model.modular;

import com.exetacticals.client.model.Bone;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IGunModel {
    String DEFAULT_FARTHEST_SIGHT_Z_NAME = "FARTHEST_SIGHT_Z";
    String DEFAULT_FARTHEST_SIDE_SIGHT_Z_NAME = "FARTHEST_SIDE_SIGHT_Z";
    String DEFAULT_HAND_ROT_PIVOT_NAME = "HAND_ROT_PIVOT";

    String getFarthestSightZName(ModuleRenderContext context);

    String getFarthestSideSightZName(ModuleRenderContext context);

    Bone getHandRotPivot();
}
