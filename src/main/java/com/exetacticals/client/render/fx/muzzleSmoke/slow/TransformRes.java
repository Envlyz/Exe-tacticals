package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class TransformRes {
    private final Vector3f positionOffset = new Vector3f();
    private float alpha = 1f;
    private float scale = 1f;
    private boolean cancel;

    public Vector3f positionOffset() {
        return positionOffset;
    }

    public float alpha() {
        return alpha;
    }

    public TransformRes setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public float scale() {
        return scale;
    }

    public TransformRes setScale(float scale) {
        this.scale = scale;
        return this;
    }

    public boolean isCancel() {
        return cancel;
    }

    public TransformRes setCancel(boolean cancel) {
        this.cancel = cancel;
        return this;
    }

    public TransformRes set(float offsetX, float offsetY, float offsetZ, float alpha, float scale, boolean cancel) {
        this.positionOffset.set(offsetX, offsetY, offsetZ);
        this.alpha = alpha;
        this.scale = scale;
        this.cancel = cancel;
        return this;
    }
}
