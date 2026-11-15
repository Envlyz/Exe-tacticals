package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public final class SmokeTexture {
    private final ResourceLocation location;
    private float baseSize = 1f;
    private float baseAlpha = 1f;

    public SmokeTexture(ResourceLocation location) {
        this.location = location;
    }

    public ResourceLocation location() {
        return location;
    }

    public float baseSize() {
        return baseSize;
    }

    public float baseAlpha() {
        return baseAlpha;
    }

    public SmokeTexture setBaseSize(float baseSize) {
        this.baseSize = Math.max(0f, baseSize);
        return this;
    }

    public SmokeTexture setBaseAlpha(float baseAlpha) {
        this.baseAlpha = Math.max(0f, baseAlpha);
        return this;
    }

    public SmokeTexture set(float baseSize, float baseAlpha) {
        return setBaseSize(baseSize).setBaseAlpha(baseAlpha);
    }

    public SmokeTexture set(Vector2f sizeAndAlpha) {
        return set(sizeAndAlpha.x, sizeAndAlpha.y);
    }

    public SmokeTexture copy() {
        return new SmokeTexture(location).set(baseSize, baseAlpha);
    }
}
