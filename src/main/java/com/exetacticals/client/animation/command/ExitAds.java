package com.exetacticals.client.animation.command;

import com.exetacticals.Client;
import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.animation.IAnimationSequence;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExitAds extends Command{

    public ExitAds(String command, float timeStamp) {
        super(command, timeStamp);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        Client.exitAds();
    }
}
