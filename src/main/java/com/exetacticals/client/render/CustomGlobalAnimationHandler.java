package com.exetacticals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.exetacticals.modularSys.modules.guns.IGun;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomGlobalAnimationHandler implements IGlobalAnimationHandler{
    @Override
    public void applyTransformPre(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player) {

    }

    @Override
    public void applyTransformPost(PoseStack poseStack, IGun gun, float partialTicks, LocalPlayer player) {

    }

    @Override
    public void clientTick(LocalPlayer player) {

    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void updateOnRenderTick(float particleTicks) {

    }
}
