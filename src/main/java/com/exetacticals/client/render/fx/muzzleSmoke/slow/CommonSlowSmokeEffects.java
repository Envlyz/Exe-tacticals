package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import com.exetacticals.GCR;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommonSlowSmokeEffects {
    public static final SlowSmoke COMMON = new SlowSmoke(
            new SlowSmokeTextureSet()
                    .set(1.5f, 0.8f)
                    .add(GCR.RL("textures/fx/slow_smoke/smoke_0.png")));
}
