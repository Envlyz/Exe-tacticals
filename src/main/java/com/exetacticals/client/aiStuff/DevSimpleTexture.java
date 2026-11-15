package com.exetacticals.client.aiStuff;

import com.mojang.blaze3d.platform.NativeImage;
import com.exetacticals.GCR;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@OnlyIn(Dist.CLIENT)
public class DevSimpleTexture extends SimpleTexture {

    public DevSimpleTexture(ResourceLocation location) {
        super(location);
    }

    public static AbstractTexture registerFromDisk(ResourceLocation location) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        textureManager.register(location, new DevSimpleTexture(location));
        return textureManager.getTexture(location, MissingTextureAtlasSprite.getTexture());
    }

    @Override
    protected TextureImage getTextureImage(ResourceManager resourceManager) {
        if (DevAssetResolver.hasSourceFile(this.location)) {
            try (InputStream in = DevAssetResolver.openOrNull(this.location, resourceManager)) {
                if (in != null) {
                    NativeImage image = NativeImage.read(in);
                    return new TextureImage(null, image);
                }
            } catch (IOException e) {
                GCR.LOGGER.warn("[GCR HotReload] failed to read dev texture {}, falling back to pack copy: {}",
                        this.location, e.toString());
            } catch (UncheckedIOException ignored) {
            }
        }
        return super.getTextureImage(resourceManager);
    }
}
