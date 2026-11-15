package com.exetacticals.client.render.fx;

import com.exetacticals.client.render.Shaders;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class GunOutline {
    private static TextureTarget mask;
    private static boolean dirty;

    private GunOutline() {
    }

    public static boolean available() {
        return Shaders.gunOutline != null;
    }

    public static void beginMask() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (mask == null) {
            mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            mask.setClearColor(0f, 0f, 0f, 0f);
            mask.clear(Minecraft.ON_OSX);
        } else if (mask.width != main.width || mask.height != main.height) {
            mask.resize(main.width, main.height, Minecraft.ON_OSX);
            mask.clear(Minecraft.ON_OSX);
        }
        if (!dirty) {
            mask.copyDepthFrom(main);
            dirty = true;
        }
        mask.bindWrite(false);
    }

    public static void endMask() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    private static float width() {
        return Math.max(1.5f, Minecraft.getInstance().getMainRenderTarget().height / 540f);
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || !dirty || mask == null) {
            return;
        }
        dirty = false;
        ShaderInstance shader = Shaders.gunOutline;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (shader != null) {
            main.bindWrite(false);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, mask.getColorTextureId());
            shader.safeGetUniform("OutlineColor").set(1f, 1f, 1f, 1f);
            shader.safeGetUniform("OutlineWidth").set(width());
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buffer.vertex(-1, -1, 0).endVertex();
            buffer.vertex(1, -1, 0).endVertex();
            buffer.vertex(1, 1, 0).endVertex();
            buffer.vertex(-1, 1, 0).endVertex();
            BufferUploader.drawWithShader(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
        mask.clear(Minecraft.ON_OSX);
        main.bindWrite(false);
    }
}
