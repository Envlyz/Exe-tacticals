package com.exetacticals.mixin;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Accessor("lightTexture")
    abstract DynamicTexture getLightTexture();

    @Inject(method = "updateLightTexture", at = @At("TAIL"))
    public void onUpdate(float partialTicks, CallbackInfo ci) {
    }
}
