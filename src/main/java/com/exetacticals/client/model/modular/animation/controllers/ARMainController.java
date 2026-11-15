package com.exetacticals.client.model.modular.animation.controllers;

import com.exetacticals.client.GunEffect;
import com.exetacticals.client.GunEffectManager;
import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.KeyframeAnimator;
import com.exetacticals.client.animation.SingleAnimationSequence;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.model.modular.modules.ARMainModel;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.views.ARView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ARMainController extends GunController<ARMainModel> {
    private SingleAnimationSequence shoot;
    private SingleAnimationSequence shootLast;
    private SingleAnimationSequence shootStuck;

    private AnimationDef thirdPersonShoot;
    private Consumer<ARMainController> animationRegister;

    public ARMainController(Consumer<ARMainController> animationRegister) {
        this.animationRegister = animationRegister;
    }

    @Override
    public void firstPersonSubscriptions(ARMainModel model) {
        super.firstPersonSubscriptions(model);
        ARView view = model.getView();

        shoot = new SingleAnimationSequence(anim("shoot").coverState());
        shootLast = new SingleAnimationSequence(anim("shoot_last").coverState());
        shootStuck = new SingleAnimationSequence(anim("shoot_stuck").coverState());

        thirdPersonShoot = anim("shoot").animation;

        subscribe(EventType.SHOOT, 0, (context) -> {
            SingleAnimationSequence animation = shoot;
            // 卡壳/最后一发由开火线程算好随事件参数带过来（见 GunController#isShootStuck），
            // 不再回读物品 states：开火线程与渲染线程不同，NBT 跨线程读可能滞后。
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
                if (view.stuck(states) || view.boltLocked(states)) {
                    CHECK.play(anim("check_chamber_simple"));
                } else {
                    CHECK.play(anim("check_chamber").coverStateExclude("ammo"));
                }
            }
        });

    }

    @Override
    public void customThirdPersonAnimation(ARMainModel model, ModuleRenderContext context) {
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
    public void initAnimation(ARMainModel model) {
        animationRegister.accept(this);
    }

    @Override
    public boolean assertCompatible(IModularModel model) {
        return model instanceof ARMainModel;
    }


}
