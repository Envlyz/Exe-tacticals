package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.views.IM203View;
import com.exetacticals.sound.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class M203 extends GrenadeLauncher implements IM203View {

    public M203(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier,
                float reloadLengthInSeconds, float reloadSendPacketDelayInSeconds,
                float impulseZ, float impulsePitch, float impulseYaw, float impulseRoll,
                float spread, float velocity, float explodeRadius) {
        super(id, weight, voxelHandler, modifier,
                reloadLengthInSeconds, reloadSendPacketDelayInSeconds,
                impulseZ, impulsePitch, impulseYaw, impulseRoll,
                spread, velocity, explodeRadius);
    }

    @Override
    protected SoundEvent getFireSound() {
        return ModSounds.M203_FIRE.get();
    }

    @Override
    protected int getProjectileModelType() {
        return 0;
    }

    @Override
    protected String getChamberStatusAfterFire() {
        return CHAMBER_FIRED;
    }
}
