package com.exetacticals.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.exetacticals.client.aiStuff.DevSimpleTexture;
import com.exetacticals.client.model.modular.ModuleModelRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class HeatMapTextureManager {
    private static final Map<ResourceLocation, AbstractTexture> CACHE = new HashMap<>();
    private static int EMPTY_TEXTURE_ID;

    public static void handleTextureLoad() {
        CACHE.clear();

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();

        ModuleModelRegister.visitAll(model -> {
            ResourceLocation path = model.getHeatMapTexPath();

            if (path == null || CACHE.containsKey(path)) {
                return;
            }

            DevSimpleTexture texture = new DevSimpleTexture(path);

            textureManager.register(path, texture);

            System.out.println("load heat map texture: " + path + " model: " + model);

            CACHE.put(path, texture);

            RenderSystem.recordRenderCall(() -> {
                RenderSystem.bindTexture(texture.getId());
                texture.setFilter(true, false);
            });
        });
        RenderSystem.recordRenderCall(HeatMapTextureManager::initEmptyTexture);
    }

    public static void reloadCachedTexture(ResourceLocation path) {
        if (path == null || !CACHE.containsKey(path)) {
            return;
        }
        AbstractTexture texture = DevSimpleTexture.registerFromDisk(path);
        CACHE.put(path, texture);

        RenderSystem.recordRenderCall(() -> {
            RenderSystem.bindTexture(texture.getId());
            texture.setFilter(true, false);
        });
    }

    public static int getTexId(ResourceLocation heatMapTexPath) {
        if (heatMapTexPath != null) {
            AbstractTexture texture = CACHE.get(heatMapTexPath);

            if (texture == null || texture == MissingTextureAtlasSprite.getTexture()) {
                return EMPTY_TEXTURE_ID;
            }
            return texture.getId();
        }
        return EMPTY_TEXTURE_ID;
    }

    public static void initEmptyTexture() {
        EMPTY_TEXTURE_ID = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, EMPTY_TEXTURE_ID);

        ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.flip();

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                1,
                1,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                buffer
        );

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public static int getEmptyId() {
        return EMPTY_TEXTURE_ID;
    }

}
