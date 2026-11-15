package com.exetacticals.client.model.modular.animation.controllers;

import com.exetacticals.client.GunEffect;
import com.exetacticals.client.GunEffectManager;
import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.KeyframeAnimator;
import com.exetacticals.client.animation.SingleAnimationSequence;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.model.modular.modules.AKModel;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.views.AKView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class AKController extends GunController<AKModel> {
    private SingleAnimationSequence shoot;
    private SingleAnimationSequence shootLast;
    private SingleAnimationSequence shootStuck;
    private AnimationDef thirdPersonShoot;
    private Consumer<AKController> animationRegister;

    public AKController(Consumer<AKController> animationRegister) {
        this.animationRegister = animationRegister;
    }

    @Override
    public void firstPersonSubscriptions(AKModel model) {
        super.firstPersonSubscriptions(model);
        AKView view = model.getView();

        shoot = new SingleAnimationSequence(anim("shoot").coverState());
        shootLast = new SingleAnimationSequence(anim("shoot_last").coverState());
        shootStuck = new SingleAnimationSequence(anim("shoot_stuck").coverState());

        thirdPersonShoot = anim("shoot").animation;

        subscribe(EventType.SHOOT, 0, (context) -> {
            SingleAnimationSequence animation = shoot;
            if (isShootStuck(context, view)) {
                animation = shootStuck;
            } else if (isShootLastRound(context, view)) {
                animation = shootLast;
            }
            SHOOT.play(animation.prepare());
        });

        subscribe(EventType.CHECK_CHAMBER, 0, (context) -> {
            ReadOnlyTag states = context.getStates();
            if (isTrackClear(MAIN)) {
                if (view.stuck(states)) {
                    CHECK.play(anim("check_chamber_simple"));
                } else {
                    CHECK.play(anim("check_chamber").coverStateExclude("ammo"));
                }
            }
        });
    }

    @Override
    public void initAnimation(AKModel model) {
        animationRegister.accept(this);
    }

    @Override
    public void customThirdPersonAnimation(AKModel model, ModuleRenderContext context) {
        if (context.entity == null) {
            return;
        }
        long startTime = GunEffectManager.getEffectTimestamp(
                context.entity.getId(),
                GunEffect.SHOOT,
                context.currentRenderNode().id
        );
        if (startTime != -1) {
            KeyframeAnimator.animate(model, thirdPersonShoot, startTime, 0.9f);
        }
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof AKModel;
    }
}
