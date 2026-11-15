package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.modules.IVoxelHandler;
import com.exetacticals.modularSys.modules.views.IGP25View;
import com.exetacticals.sound.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class GP25 extends GrenadeLauncher implements IGP25View {

    public GP25(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier,
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
        return ModSounds.GP25_FIRE.get();
    }

    @Override
    protected int getProjectileModelType() {
        return 1;
    }

    @Override
    protected String getChamberStatusAfterFire() {
        return CHAMBER_EMPTY;
    }
}
