package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class SlowSmokeTextureSet {
    private final List<SmokeTexture> textures = new ArrayList<>();
    private float baseSize = 1f;
    private float baseAlpha = 1f;

    public SlowSmokeTextureSet() {
    }

    public SlowSmokeTextureSet(ResourceLocation... locations) {
        add(locations);
    }

    public SlowSmokeTextureSet add(ResourceLocation location) {
        textures.add(new SmokeTexture(location).set(baseSize, baseAlpha));
        return this;
    }

    public SlowSmokeTextureSet add(ResourceLocation... locations) {
        for (ResourceLocation location : locations) {
            add(location);
        }
        return this;
    }

    public SlowSmokeTextureSet add(SmokeTexture texture) {
        textures.add(texture);
        return this;
    }

    public int size() {
        return textures.size();
    }

    public boolean isEmpty() {
        return textures.isEmpty();
    }

    public SmokeTexture get(int index) {
        return textures.get(index);
    }

    public List<SmokeTexture> textures() {
        return textures;
    }

    public float baseSize() {
        return baseSize;
    }

    public float baseAlpha() {
        return baseAlpha;
    }

    public SlowSmokeTextureSet setBaseSize(float baseSize) {
        this.baseSize = Math.max(0f, baseSize);
        for (SmokeTexture texture : textures) {
            texture.setBaseSize(this.baseSize);
        }
        return this;
    }

    public SlowSmokeTextureSet setBaseAlpha(float baseAlpha) {
        this.baseAlpha = Math.max(0f, baseAlpha);
        for (SmokeTexture texture : textures) {
            texture.setBaseAlpha(this.baseAlpha);
        }
        return this;
    }

    public SlowSmokeTextureSet set(float baseSize, float baseAlpha) {
        return setBaseSize(baseSize).setBaseAlpha(baseAlpha);
    }

    public SlowSmokeTextureSet set(Vector2f sizeAndAlpha) {
        return set(sizeAndAlpha.x, sizeAndAlpha.y);
    }

    public SmokeTexture pick(int index) {
        if (textures.isEmpty()) {
            return null;
        }
        return textures.get(Mth.clamp(index, 0, textures.size() - 1));
    }

    public SmokeTexture pickBySeed(int seed) {
        if (textures.isEmpty()) {
            return null;
        }
        return pick(Math.floorMod(seed, textures.size()));
    }

    public int indexBySeed(int seed) {
        return textures.isEmpty() ? 0 : Math.floorMod(seed, textures.size());
    }

    public SlowSmokeTextureSet copy() {
        SlowSmokeTextureSet set = new SlowSmokeTextureSet();
        set.baseSize = baseSize;
        set.baseAlpha = baseAlpha;
        for (SmokeTexture texture : textures) {
            set.textures.add(texture.copy());
        }
        return set;
    }
}
