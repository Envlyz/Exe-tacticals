package com.exetacticals.client.model.modular.animation.controllers;

import com.exetacticals.Client;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.model.modular.modules.GrenadeLauncherModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class GrenadeLauncherController<T extends GrenadeLauncherModel<?>> extends AnimationController<T> {

    protected static final String CHECK_ANIMATION = "check";
    protected static final String RELOAD_ANIMATION = "reload";

    private static final String MAIN_TRACK = "main";
    private static final String CHECK_TRACK = "check";

    private static final String SHELL_BONE = "shell";

    protected abstract String getCheckAnimationPath();

    protected abstract String getReloadAnimationPath();

    @Override
    public void firstPersonSubscriptions(T model) {
        super.firstPersonSubscriptions(model);

        subscribe(EventType.CHECK_SUB_WEAPON, 0, (ctx) -> {
            if (isTrackClear(MAIN_TRACK)) {
                getTrack(CHECK_TRACK).play(anim(CHECK_ANIMATION));
            }
        });

        subscribe(EventType.RELOAD_SUB_WEAPON, 0, (ctx) -> {
            getTrack(MAIN_TRACK).play(anim(RELOAD_ANIMATION).coverStateExclude(SHELL_BONE));
        });

        subscribe(EventType.SHOOT, 0, (ctx) -> getTrack(CHECK_TRACK).clear());
    }

    @Override
    public void initAnimation(T model) {
        registerAnimations(
                CHECK_ANIMATION, getCheckAnimationPath(),
                RELOAD_ANIMATION, getReloadAnimationPath()
        );
    }

    @Override
    public void initTrack(T model) {
        defineTrack(MAIN_TRACK).addOnPlayed(instance -> getTrack(CHECK_TRACK).clear());
        defineTrack(CHECK_TRACK).addOnApplied((ctx, m) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack(CHECK_TRACK).clear();
            }
        });
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof GrenadeLauncherModel<?>;
    }
}
