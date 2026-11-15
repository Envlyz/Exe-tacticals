package com.exetacticals.client.animation;

import com.exetacticals.client.animation.command.Command;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class SingleAnimationSequence implements IAnimationSequence {

    private final AnimationInstance instance;

    private long startTime;
    private Consumer<IAnimationSequence> onRemoved;

    private float speed = 1.0f;

    public SingleAnimationSequence(AnimationInstance instance) {
        this.instance = instance;
        this.startTime = System.currentTimeMillis();
        this.instance.timeStamp = startTime;
    }

    public SingleAnimationSequence(AnimationDef definition) {
        this.instance = definition.asInstance();
        this.startTime = System.currentTimeMillis();
        this.instance.timeStamp = startTime;
    }

    public float getSpeed() {
        return speed;
    }

    public SingleAnimationSequence setSpeed(float speed) {
        this.speed = Math.max(0f, speed);
        return this;
    }

    @Override
    public IAnimationSequence append(AnimationInstance instance) {
        return this;
    }

    @Override
    public IAnimationSequence finishBuild() {
        return this;
    }

    @Override
    public IAnimationSequence prepare() {
        instance.reset();
        instance.looped = 0;
        startTime = System.currentTimeMillis();
        instance.timeStamp = startTime;
        return this;
    }

    @Override
    public void apply(IAnimated root, ModuleRenderContext context) {

        float finalSpeed = this.speed * instance.getSpeed();

        KeyframeAnimator._animate(
                root,
                this.instance.animation,
                this.instance.timeStamp,
                0L,
                this.instance.scales.x,
                this.instance.scales.y,
                this.instance.scales.z,
                instance.loop(),
                instance.keepOnLastFrame,
                finalSpeed
        );

        if (this.instance.onPlaying != null && applying()) {
            this.instance.onPlaying.accept(getCurrentAnimatingProgress());
        }

        List<Command> commands = this.instance.animation.getCommands();
        if (!commands.isEmpty()) {
            for (Command command : commands) {
                command.onFrame(root, this, context);
            }
        }
    }

    @Override
    public boolean applying() {

        if (instance.keepOnLastFrame) {
            return true;
        }

        float res = KeyframeAnimator.dist(
                instance.timeStamp,
                0L,
                instance.loop(),
                false,
                instance.animation,
                this.speed * instance.getSpeed()
        );
        return !Float.isNaN(res);
    }

    @Override
    public boolean tick() {
        long now = System.currentTimeMillis();

        long dist = (long) ((now - startTime) * speed);

        long instanceLength = (long) (
                instance.length() / Math.max(1e-6f, instance.getSpeed())
        );

        if (instance.shouldLoop()) {
            if (dist > instanceLength * (instance.looped + 1)) {
                instance.onLooped();
            }
            instance.onClientTick();
        } else {

            if (instance.keepOnLastFrame) {
                instance.onClientTick();
                return false;
            }

            instance.onClientTick();
        }

        return dist > getTotalLength();
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public float getLength() {
        return getTotalLength() / 1000f;
    }

    private long getTotalLength() {
        return (long) (
                instance.length()
                        * (instance.loop() ? instance.loopTimes : 1)
                        / Math.max(1e-6f, instance.getSpeed())
        );
    }

    @Override
    public float getProgress() {
        long now = System.currentTimeMillis();
        float elapsed = (now - startTime) * speed;
        return elapsed / (float) getTotalLength();
    }

    @Override
    public float getCurrentAnimatingProgress() {
        long now = System.currentTimeMillis();

        long instanceLength = (long) (
                instance.length() / Math.max(1e-6f, instance.getSpeed())
        );

        long dist = (long) ((now - startTime) * speed)
                - (long) (instance.looped * instanceLength);

        return dist / (float) instanceLength;
    }

    @Override
    public AnimationInstance getCurrentAnimating() {
        return this.instance;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public AnimationInstance get(int index) {
        return this.instance;
    }

    @Override
    public void onRemoved(Consumer<IAnimationSequence> onRemoved) {
        this.onRemoved = onRemoved;
    }

    @Override
    public void removed() {
        if (onRemoved != null) {
            onRemoved.accept(this);
        }
    }
}
