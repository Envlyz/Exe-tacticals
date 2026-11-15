package com.exetacticals.client.animation;

import com.exetacticals.client.animation.command.Command;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class AnimationSequence implements IAnimationSequence {

    private final List<AnimationInstance> animations = new ArrayList<>();

    private long startTime;
    private long length;

    private boolean finished;
    private AnimationInstance current;

    private Consumer<IAnimationSequence> onRemoved;

    private float speed = 1.0f;

    public AnimationSequence() {}

    public AnimationSequence(AnimationInstance... instance) {
        this.animations.addAll(List.of(instance));
        this.finishBuild();
    }

    public float getSpeed() {
        return speed;
    }

    public AnimationSequence setSpeed(float speed) {
        this.speed = Math.max(0f, speed);
        return this;
    }

    @Override
    public IAnimationSequence append(AnimationInstance instance) {
        if (finished) return this;
        animations.add(instance);
        return this;
    }

    public AnimationSequence finishBuild() {
        startTime = System.currentTimeMillis();
        length = 0;

        for (int i = 0; i < animations.size(); i++) {
            AnimationInstance instance = animations.get(i);

            if (instance.keepOnLastFrame && i != animations.size() - 1) {
                throw new IllegalStateException("keepOnLastFrame instance must be the last in AnimationSequence");
            }

            instance.timeStamp = startTime + length;

            long realLength = (long) (
                    instance.length()
                            * (instance.loop() ? instance.loopTimes : 1)
                            / Math.max(1e-6f, instance.getSpeed())
            );

            length += realLength;
        }

        finished = true;
        return this;
    }

    public AnimationSequence prepare() {
        startTime = System.currentTimeMillis();
        for (AnimationInstance instance : animations) {
            instance.reset();
            instance.looped = 0;
        }
        return finishBuild();
    }

    @Override
    public void apply(IAnimated root, ModuleRenderContext context) {
        AnimationInstance inst = animations.get(getIndex());
        if (inst != null) {

            float finalSpeed = this.speed * inst.getSpeed();

            KeyframeAnimator._animate(
                    root,
                    inst.animation,
                    inst.timeStamp,
                    0L,
                    inst.scales.x,
                    inst.scales.y,
                    inst.scales.z,
                    inst.loop(),
                    inst.keepOnLastFrame,
                    finalSpeed
            );

            current = inst;

            if (inst.onPlaying != null) {
                inst.onPlaying.accept(getCurrentAnimatingProgress());
            }

            List<Command> commands = inst.animation.getCommands();
            if (!commands.isEmpty()) {
                for (Command command : commands) {
                    command.onFrame(root, this, context);
                }
            }
        }
    }

    @Override
    public boolean applying() {
        AnimationInstance inst = animations.get(getIndex());
        if (inst != null) {

            if (inst.keepOnLastFrame) {
                return true;
            }

            float res = KeyframeAnimator.dist(
                    inst.timeStamp,
                    0L,
                    inst.loop(),
                    false,
                    inst.animation,
                    this.speed * inst.getSpeed()
            );
            return !Float.isNaN(res);
        }
        return false;
    }

    @Override
    public boolean tick() {
        int index = getIndex();
        AnimationInstance inst = animations.get(index);

        long now = System.currentTimeMillis();

        long dist = (long) ((now - inst.timeStamp) * this.speed);

        long instanceLength = (long) (
                inst.length() / Math.max(1e-6f, inst.getSpeed())
        );

        if (inst.shouldLoop()) {
            if (dist > instanceLength * (inst.looped + 1)) {
                inst.onLooped();
            }
            inst.onClientTick();
        } else {
            if (dist > instanceLength) {

                if (inst == animations.get(animations.size() - 1) && inst.keepOnLastFrame) {
                    inst.onClientTick();
                    return false;
                }

                if (inst == animations.get(animations.size() - 1)) {
                    return true;
                }

                inst = animations.get(index + 1);
            }
            inst.onClientTick();
        }

        if (animations.get(animations.size() - 1).keepOnLastFrame) {
            return false;
        }

        return (long) ((now - startTime) * speed) > length;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public float getLength() {
        return length / 1000f;
    }

    @Override
    public float getProgress() {
        long now = System.currentTimeMillis();
        float elapsed = (now - startTime) * speed;
        return elapsed / (float) length;
    }

    @Override
    public float getCurrentAnimatingProgress() {
        if (current == null) return 0;

        long now = System.currentTimeMillis();

        long dist = (long) (
                (now - current.timeStamp) * speed
        ) - (long) (
                current.looped * (current.length() / Math.max(1e-6f, current.getSpeed()))
        );

        return dist / (float) (
                current.length() / Math.max(1e-6f, current.getSpeed())
        );
    }

    private int getIndex() {
        long virtualNow = startTime + (long) ((System.currentTimeMillis() - startTime) * speed);

        int index = Mth.binarySearch(
                0,
                animations.size(),
                (i) -> animations.get(i).timeStamp >= virtualNow
        ) - 1;

        return Math.max(0, Math.min(index, animations.size() - 1));
    }

    @Override
    public AnimationInstance getCurrentAnimating() {
        return current;
    }

    @Override
    public AnimationInstance get(int index) {
        return animations.get(index);
    }

    @Override
    public int size() {
        return animations.size();
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
