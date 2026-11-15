package com.exetacticals.client.model.modular.animation.controllers;

import com.exetacticals.Client;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.animation.eventSys.Callback;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.model.modular.animation.eventSys.Track;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.modularSys.modules.views.IGunView;

public abstract class  GunController<T extends IModularModel>  extends AnimationController<T> {
    protected Track<T> MAIN;
    protected Track<T> SHOOT;
    protected Track<T> DRAW;
    protected Track<T> CHECK;

    @Override
    public void firstPersonSubscriptions(T model) {
        super.firstPersonSubscriptions(model);
        subscribe(EventType.DRAW, 0, (context) -> {
            if (isTrackClear(MAIN)) {
                DRAW.play(anim("draw"));
            }
        });

        subscribe(EventType.HOLSTER, 0, (context) -> {
            if (isTrackClear(MAIN)) {
                DRAW.play(anim("holster").keepOnLastFrame());
            }
        });

        subscribe(EventType.RELOAD, 0, (context) -> {
            String name = context.getParam("animation_name");
            MAIN.play(anim(name).coverState());
        });

        subscribe(EventType.RELOAD_SUB_WEAPON, 10, (context) -> {
            String name = context.getParam("animation_name");
            MAIN.play(anim(name).coverState());
        });

        subscribe(EventType.CHECK_MAG, 0, (context) -> {
            if (isTrackClear(MAIN)) {
                CHECK.play(anim("check_mag").coverState());
            }
        });

        subscribe(EventType.CHECK_SUB_WEAPON, 10, (context) -> {
            if (isTrackClear(MAIN)) {
                String animationName = context.getParam("animation_name");
                CHECK.play(anim(animationName));
            } else {
                context.cancel();
            }
        });

        subscribe(EventType.REMOVE_STUCK, 0, (context) -> {
            String name = context.getParam("name");
            MAIN.play(anim(name).coverState());
        });

        subscribe(EventType.SWITCH_FIRE_MODE, 0, (context) -> {
            String after = context.getParam("after");
            MAIN.play(anim(after).coverState());
        });
    }

    @Override
    public void initTrack(T moduleModel) {
        MAIN = defineTrack("main").addOnPlayed(instance -> getTrack("check").clear());
        SHOOT = defineTrack("shoot").addOnPlayed(instance -> getTrack("check").clear());

        DRAW = defineTrack("draw").addOnPlayed(instance -> getTrack("check").clear());

        CHECK = defineTrack("check").addOnApplied((ctx, model) -> {
            if (Client.getAimingProgress() != 0) {
                getTrack("check").clear();
            }
        });
    }

    protected static boolean isShootStuck(Callback.EventContext context, IGunView view) {
        String param = context.getParam(EventType.PARAM_STUCK);
        if (param != null) {
            return Boolean.parseBoolean(param);
        }
        return view.stuck(context.getStates());
    }

    protected static boolean isShootLastRound(Callback.EventContext context, IGunView view) {
        String param = context.getParam(EventType.PARAM_LAST_ROUND);
        if (param != null) {
            return Boolean.parseBoolean(param);
        }
        ReadOnlyTag states = context.getStates();
        return view.getAmmoLeft(states) == 0 && view.hasMagAttachment(states);
    }
}
