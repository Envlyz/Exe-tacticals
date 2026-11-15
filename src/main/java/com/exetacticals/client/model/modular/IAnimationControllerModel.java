package com.exetacticals.client.model.modular;

import com.exetacticals.client.model.modular.animation.eventSys.IAnimationController;
import com.exetacticals.client.render.FirstPersonRenderContext;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public interface IAnimationControllerModel {
    Optional<IAnimationController<?>> getController();

    void bindController(IAnimationController<?> controller);

    void callInitEventSubscriptions();

    void callInitAnimation();

    void callInitTrack();

    void applyFirstPersonAnimation(ModuleRenderContext context);

    void applyThirdPersonAnimation(ModuleRenderContext context);

    void applyCustomFirstPersonAnimation(FirstPersonRenderContext firstPersonRenderContext);
}
