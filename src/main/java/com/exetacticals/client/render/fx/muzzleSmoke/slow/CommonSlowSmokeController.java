package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommonSlowSmokeController implements ISlowSmokeController {
    private float startScale = 0.45f;
    private float endScale = 2.2f;
    private float growCurve = 3f;
    private float scaleRandom = 0.15f;

    private float travel = 1f;
    private float travelCurve = 2f;
    private float turnCurve = 1.6f;
    private float rise = 0f;
    private float travelRandom = 0.25f;
    private float turnRandom = 0.25f;
    private float riseRandom = 0.15f;
    private float zRandom = 0.25f;

    private float startAlpha = 1f;
    private float fadeStart = 0.5f;
    private float fadeCurve = 2f;

    @Override
    public void update(float progress, int seed, TransformRes result) {
        float p = Mth.clamp(progress, 0f, 1f);

        float grow = 1f - (float) Math.pow(1f - p, growCurve);
        float scale = Mth.lerp(grow, startScale, endScale) * around(random01(seed, 7919), scaleRandom);

        float distance = travel * (1f - (float) Math.pow(1f - p, travelCurve))
                * around(random01(seed, 104729), travelRandom);

        float turn = Mth.clamp((float) Math.pow(p, turnCurve) * around(random01(seed, 15485863), turnRandom), 0f, 1f);
        float dirZ = -(1f - turn);
        float dirY = rise + (random01(seed, 32452843) - 0.5f) * 2f * riseRandom;
        float len = Mth.sqrt(turn * turn + dirY * dirY + dirZ * dirZ);
        if (len < 1.0E-6f) {
            len = 1f;
        }
        float offsetScale = distance / len;

        float baseZ = -(0.25f + random01(seed, 86028121) * 0.75f) * zRandom;

        float alpha = startAlpha;
        if (p > fadeStart) {
            float t = (p - fadeStart) / (1f - fadeStart);
            alpha = startAlpha * (1f - (float) Math.pow(t, fadeCurve));
        }

        result.set(turn * offsetScale, dirY * offsetScale, dirZ * offsetScale + baseZ,
                Mth.clamp(alpha, 0f, 1f), Math.max(0f, scale), false);
    }

    public CommonSlowSmokeController setScale(float startScale, float endScale) {
        this.startScale = Math.max(0f, startScale);
        this.endScale = Math.max(0f, endScale);
        return this;
    }

    public CommonSlowSmokeController setGrowCurve(float growCurve) {
        this.growCurve = Math.max(0.01f, growCurve);
        return this;
    }

    public CommonSlowSmokeController setTravel(float travel) {
        this.travel = Math.max(0f, travel);
        return this;
    }

    public CommonSlowSmokeController setTravelCurve(float travelCurve) {
        this.travelCurve = Math.max(0.01f, travelCurve);
        return this;
    }

    public CommonSlowSmokeController setTurnCurve(float turnCurve) {
        this.turnCurve = Math.max(0.01f, turnCurve);
        return this;
    }

    public CommonSlowSmokeController setRise(float rise) {
        this.rise = rise;
        return this;
    }

    public CommonSlowSmokeController setAlpha(float startAlpha, float fadeStart) {
        this.startAlpha = Mth.clamp(startAlpha, 0f, 1f);
        this.fadeStart = Mth.clamp(fadeStart, 0f, 1f);
        return this;
    }

    public CommonSlowSmokeController setFadeCurve(float fadeCurve) {
        this.fadeCurve = Math.max(0.01f, fadeCurve);
        return this;
    }

    public CommonSlowSmokeController setRandom(float travelRandom, float scaleRandom, float turnRandom, float riseRandom) {
        this.travelRandom = Math.max(0f, travelRandom);
        this.scaleRandom = Math.max(0f, scaleRandom);
        this.turnRandom = Math.max(0f, turnRandom);
        this.riseRandom = Math.max(0f, riseRandom);
        return this;
    }

    public CommonSlowSmokeController setZRandom(float zRandom) {
        this.zRandom = Math.max(0f, zRandom);
        return this;
    }

    public float zRandom() {
        return zRandom;
    }

    public float startScale() {
        return startScale;
    }

    public float endScale() {
        return endScale;
    }

    public float travel() {
        return travel;
    }

    public float startAlpha() {
        return startAlpha;
    }

    public float fadeStart() {
        return fadeStart;
    }

    private static float around(float seed01, float amount) {
        return 1f + (seed01 - 0.5f) * 2f * amount;
    }

    private static float random01(int seed, int salt) {
        int h = seed * 0x9E3779B9 + salt * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return (h >>> 8) / (float) 0x1000000;
    }
}
