package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SlowSmoke {
    private final SlowSmokeTextureSet textureSet;

    public SlowSmoke(SlowSmokeTextureSet textureSet) {
        this.textureSet = textureSet;
    }

    public SlowSmokeTextureSet getTextureSet() {
        return textureSet;
    }

    public int textureCount() {
        return textureSet == null ? 0 : textureSet.size();
    }

    public boolean isExpired(long shootTime, @Nullable SlowSmokeEntry entry) {
        return System.currentTimeMillis() - shootTime >= durationMillis(entry);
    }

    public SlowSmokeTask createTask(PoseStack.Pose pose, long shootTime) {
        return createTask(pose, shootTime, null, LightTexture.FULL_BRIGHT);
    }

    public SlowSmokeTask createTask(PoseStack.Pose pose, long shootTime, int light) {
        return createTask(pose, shootTime, null, light);
    }

    public SlowSmokeTask createTask(PoseStack.Pose pose, long shootTime, @Nullable SlowSmokeEntry entry, int light) {
        SlowSmokeEntry config = entry == null ? new SlowSmokeEntry() : entry;
        return new SlowSmokeTask(pose, shootTime, config, textureSet, light);
    }

    private static int durationMillis(@Nullable SlowSmokeEntry entry) {
        return entry == null ? new SlowSmokeEntry().fadeDurationMillis() : entry.fadeDurationMillis();
    }
}
