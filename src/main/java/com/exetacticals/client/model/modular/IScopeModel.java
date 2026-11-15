package com.exetacticals.client.model.modular;

import com.exetacticals.client.model.Bone;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public interface IScopeModel extends ISightModel{
    @NotNull
    Bone getRearLensBone();

    @NotNull
    Bone getCrosshairBone();

    @NotNull
    Bone getCrosshairZBone();

    ResourceLocation getCrosshairTexture();

    @Deprecated
    default float getFovModify() {
        return 70f;
    }
}
