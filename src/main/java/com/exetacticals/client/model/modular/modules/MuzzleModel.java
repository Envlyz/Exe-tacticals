package com.exetacticals.client.model.modular.modules;

import com.exetacticals.client.model.MeshModelData;
import com.exetacticals.client.model.modular.MuzzleEntry;
import com.exetacticals.client.model.modular.MuzzleFlashRenderer;
import com.exetacticals.client.render.fx.muzzleFlash.MuzzleFlash;
import com.exetacticals.client.render.fx.muzzleSmoke.fast.FastMuzzleSmoke;
import com.exetacticals.client.render.fx.muzzleSmoke.slow.SlowSmokeEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MuzzleModel extends MuzzleFlashRendererModel {

    public MuzzleModel(MeshModelData root, float scale, MuzzleFlash muzzleFlash, float smokeScale, FastMuzzleSmoke muzzleSmoke, float flashLightIntensity) {
        super(root, new MuzzleFlashRenderer(
                new MuzzleEntry("no1", "MUZZLE_FLASH", null, scale, muzzleFlash, smokeScale, muzzleSmoke, flashLightIntensity)
        ));

    }

    public MuzzleModel(MeshModelData root, float scale, MuzzleFlash muzzleFlash, float smokeScale, FastMuzzleSmoke muzzleSmoke, float flashLightIntensity, SlowSmokeEntry slowSmokeEntry) {
        super(root, new MuzzleFlashRenderer(
                new MuzzleEntry("no1", "MUZZLE_FLASH", null, scale, muzzleFlash, smokeScale, muzzleSmoke, flashLightIntensity).withSlowSmokeEntry(slowSmokeEntry)
        ));

    }



}