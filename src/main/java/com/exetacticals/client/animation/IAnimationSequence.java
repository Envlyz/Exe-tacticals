package com.exetacticals.client.animation;

import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public interface IAnimationSequence {

    IAnimationSequence append(AnimationInstance instance);

    void apply(IAnimated root, ModuleRenderContext context);

    boolean tick();

    boolean applying();

    long getStartTime();

    float getLength();

    IAnimationSequence finishBuild();

    @Nullable
    AnimationInstance getCurrentAnimating();

    float getProgress();

    float getCurrentAnimatingProgress();

    int size();

    AnimationInstance get(int index);

    void onRemoved(Consumer<IAnimationSequence> onRemoved);

    void removed();

    IAnimationSequence prepare();
}
