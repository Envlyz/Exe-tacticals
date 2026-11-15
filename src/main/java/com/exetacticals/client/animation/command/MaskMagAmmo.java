package com.exetacticals.client.animation.command;

import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.animation.IAnimationSequence;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.task.GunTaskHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MaskMagAmmo extends Command{
    public static final String VARIABLE_KEY = "mask_mag_ammo";
    public static final int MASK_AMMO_LEFT_KEY = 30001;

    private float startProgress;
    public MaskMagAmmo(String command, float timeStamp) {
        super(command, timeStamp);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        startProgress = timeStamp / def.lengthInSeconds();
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        if (sequence.getCurrentAnimatingProgress() >= startProgress) {
            int taskCustomVariable = GunTaskHandler.INSTANCE.getTaskCustomVariable(VARIABLE_KEY);
            if (taskCustomVariable != -1) {
                context.setLocalStorage(MASK_AMMO_LEFT_KEY, taskCustomVariable);
            }
        }
    }
}
