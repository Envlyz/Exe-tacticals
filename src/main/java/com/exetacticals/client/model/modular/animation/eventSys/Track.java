package com.exetacticals.client.model.modular.animation.eventSys;

import com.exetacticals.client.animation.*;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class Track<T extends IModularModel> {
    private final String name;
    private boolean activated;
    private List<Consumer<IAnimationSequence>> onPlayedCallbacks;
    private List<AppliedCallback<T>> onAppliedCallbacks;

    private final ThreadLocal<String> currName = ThreadLocal.withInitial(() -> null);

    public Track(String name) {
        this.name = name;
        this.activated = true;
    }

    public Track<T> addOnPlayed(Consumer<IAnimationSequence> onPlayed) {
        if (onPlayed == null) {
            return this;
        }
        if (onPlayedCallbacks == null) {
            onPlayedCallbacks = new ArrayList<>();
        }
        onPlayedCallbacks.add(onPlayed);
        return this;
    }

    public Track<T> addOnApplied(AppliedCallback<T> onApplied) {
        if (onApplied == null) {
            return this;
        }
        if (onAppliedCallbacks == null) {
            onAppliedCallbacks = new ArrayList<>();
        }
        onAppliedCallbacks.add(onApplied);
        return this;
    }

    public boolean isActivated() {
        return activated;
    }

    public void activate() {
        this.activated = true;
    }

    public void deactivate() {
        this.activated = false;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getName() {
        return name;
    }

    public void clear() {
        AnimationHandler.INSTANCE.clearAnimation(getCurrName());
    }

    public boolean hasAnimation() {
        return AnimationHandler.INSTANCE.has(getCurrName());
    }

    public void play(IAnimationSequence sequence) {
        AnimationHandler.INSTANCE.startAnimation(getCurrName(), sequence);
        handlePlayedCallback(sequence);
    }

    public void play(AnimationInstance instance) {
        SingleAnimationSequence sequence = new SingleAnimationSequence(instance);
        AnimationHandler.INSTANCE.startAnimation(getCurrName(), sequence);
        handlePlayedCallback(sequence);
    }

    public void play(AnimationInstance... instance) {
        AnimationSequence sequence = new AnimationSequence(instance);
        AnimationHandler.INSTANCE.startAnimation(getCurrName(), sequence);
        handlePlayedCallback(sequence);
    }

    private void handlePlayedCallback(IAnimationSequence sequence) {
        if (onPlayedCallbacks != null) {
            for (Consumer<IAnimationSequence> onPlayed : onPlayedCallbacks) {
                onPlayed.accept(sequence);
            }
        }
    }

    @Nullable
    public IAnimationSequence getAnimating() {
        return AnimationHandler.INSTANCE.get(getCurrName());
    }

    @Nullable
    public AnimationInstance getCurrentInstance() {
        IAnimationSequence animating = getAnimating();
        if (animating == null) {
            return null;
        }
        return animating.getCurrentAnimating();
    }

    @Nullable
    public AnimationDef getAnimatingDef() {
        AnimationInstance animatingInstance = getCurrentInstance();
        return animatingInstance != null ? animatingInstance.animation : null;
    }

    public float getProgress() {
        IAnimationSequence animating = getAnimating();
        if (animating == null) {
            return 0;
        } else {
            return animating.getProgress();
        }
    }

    public float getCurrentAnimatingProgress() {
        IAnimationSequence animating = getAnimating();
        if (animating == null) {
            return 0;
        } else {
            return animating.getCurrentAnimatingProgress();
        }
    }

    public void applyToModel(T model, ModuleRenderContext context) {
        if (activated && hasAnimation()) {
            AnimationHandler.INSTANCE.apply(model, getCurrName(), context);
            if (onAppliedCallbacks != null) {
                for (AppliedCallback<T> onApplied : onAppliedCallbacks) {
                    onApplied.onApplied(context, model);
                }
            }
        }
    }

    public String getCurrName() {
        String bound = currName.get();
        return bound == null ? name : bound;
    }

    public void clearNodeBinding() {
        currName.remove();
    }

    public void onUsingNode(String id) {
        currName.set(id + getName());
    }

    public interface AppliedCallback<E extends IModularModel> {
        void onApplied(ModuleRenderContext context, E model);
    }
}