package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ISlowSmokeController {
    void update(float progress, int seed, TransformRes result);
}
