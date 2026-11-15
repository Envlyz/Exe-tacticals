package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SlowSmokeEntry {
    public static final float DEFAULT_FADE_SECONDS = 2.5f;

    private float fadeSeconds = DEFAULT_FADE_SECONDS;
    private float localScale = 1f;
    private boolean followFlyDir = true;
    private boolean randomRotate = true;
    private ISlowSmokeController controller = new CommonSlowSmokeController();
    private SlowSmoke slowSmoke;

    public SlowSmokeEntry() {
    }

    public SlowSmokeEntry(float fadeSeconds, float localScale, SlowSmoke slowSmoke) {
        setFadeSeconds(fadeSeconds);
        setLocalScale(localScale);
        this.slowSmoke = slowSmoke;
    }

    public SlowSmokeEntry(float fadeSeconds, float localScale, ISlowSmokeController controller, SlowSmoke slowSmoke) {
        this(fadeSeconds, localScale, slowSmoke);
        setController(controller);
    }

    public SlowSmoke getSlowSmoke() {
        return slowSmoke;
    }

    public SlowSmokeEntry setSlowSmoke(SlowSmoke slowSmoke) {
        this.slowSmoke = slowSmoke;
        return this;
    }

    public SlowSmokeEntry fade(float seconds) {
        return setFadeSeconds(seconds);
    }

    public SlowSmokeEntry scale(float localScale) {
        return setLocalScale(localScale);
    }

    public SlowSmokeEntry controller(ISlowSmokeController controller) {
        return setController(controller);
    }

    public ISlowSmokeController controller() {
        return controller;
    }

    public SlowSmokeEntry setController(ISlowSmokeController controller) {
        this.controller = controller == null ? new CommonSlowSmokeController() : controller;
        return this;
    }

    public float fadeSeconds() {
        return fadeSeconds;
    }

    public int fadeDurationMillis() {
        return Math.max(1, Math.round(fadeSeconds * 1000f));
    }

    public SlowSmokeEntry setFadeSeconds(float seconds) {
        this.fadeSeconds = Math.max(0.001f, seconds);
        return this;
    }

    public SlowSmokeEntry setFadeDurationMillis(int millis) {
        return setFadeSeconds(millis * 0.001f);
    }

    public float localScale() {
        return localScale;
    }

    public SlowSmokeEntry setLocalScale(float localScale) {
        this.localScale = Math.max(0f, localScale);
        return this;
    }

    public boolean followFlyDir() {
        return followFlyDir;
    }

    public SlowSmokeEntry setFollowFlyDir(boolean followFlyDir) {
        this.followFlyDir = followFlyDir;
        return this;
    }

    public boolean randomRotate() {
        return randomRotate;
    }

    public SlowSmokeEntry setRandomRotate(boolean randomRotate) {
        this.randomRotate = randomRotate;
        return this;
    }

    public SlowSmokeEntry copy() {
        SlowSmokeEntry entry = new SlowSmokeEntry(fadeSeconds, localScale, controller, slowSmoke);
        entry.followFlyDir = followFlyDir;
        entry.randomRotate = randomRotate;
        return entry;
    }
}
