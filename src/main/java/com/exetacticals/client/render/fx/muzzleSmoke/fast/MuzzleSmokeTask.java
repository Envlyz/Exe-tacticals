package com.exetacticals.client.render.fx.muzzleSmoke.fast;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MuzzleSmokeTask {
    public PoseStack.Pose pose;
    public long lastShoot;
    public FastMuzzleSmoke effect;
    private final int randomSeed;
    private final int light;
    private final float scale;
    @Nullable
    private RenderType lastRenderType;

    public MuzzleSmokeTask(PoseStack.Pose pose, long lastShoot, FastMuzzleSmoke effect, int light, float scale)  {
        this.pose = pose;
        this.lastShoot = lastShoot;
        this.effect = effect;
        this.randomSeed = (int) (Math.random() * 1000);
        this.light = light;
        this.scale = scale;
    }

    public boolean handleRender(MultiBufferSource bufferSource) {
        boolean finished = isFinished();
        this.lastRenderType = null;
        if (!finished) {
            PoseStack.Pose copy = com.exetacticals.Utils.copyPose(pose);
            copy.pose().scale(scale, scale, 1);
            this.lastRenderType = effect.render(lastShoot, copy, bufferSource, randomSeed, light);
        }
        return finished;
    }

    @Nullable
    public RenderType lastRenderType() {
        return lastRenderType;
    }

    public boolean isFinished() {
        return System.currentTimeMillis() - lastShoot > effect.length;
    }
}
