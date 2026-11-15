package com.exetacticals.client.model.modular;

import com.exetacticals.client.model.Bone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ILaserSightModel extends IModularModel {
    Bone getLaserPoseBone();

    LaserSighRenderer getRenderer();
}
