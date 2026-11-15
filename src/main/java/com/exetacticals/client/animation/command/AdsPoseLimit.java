package com.exetacticals.client.animation.command;

import com.exetacticals.Client;
import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.animation.IAnimationSequence;
import com.exetacticals.client.model.Bone;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdsPoseLimit extends Command{
    private final String boneName;
    private final float transScale;
    private final float rotScale;

    public AdsPoseLimit(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 3) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        boneName = args.get(0);
        transScale = Float.parseFloat(args.get(1));
        rotScale = Float.parseFloat(args.get(2));
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        float aimingProgress = Client.getAimingProgress(context.partialTicks);
        if (aimingProgress > 0) {
            IModularModel model = context.root.model;
            Bone bone = model.getBone(boneName);
            if (bone != null) {
                float trans = Mth.lerp(aimingProgress, 1, transScale);
                float rot = Mth.lerp(aimingProgress, 1, rotScale);
                bone.x *= trans;
                bone.y *= trans;
                bone.z *= trans;
                bone.xRot *= rot;
                bone.yRot *= rot;
                bone.zRot *= rot;
            }
        }
    }
}