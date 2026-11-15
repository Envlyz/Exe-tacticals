package com.exetacticals.client.screen;

import com.exetacticals.client.render.Shaders;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
public final class GuiPanelRender {
    public static int ACCENT = 0xFF009091;
    public static int ACCENT_SOFT = 0x55009091;
    public static final int DANGER = 0xFFFF5D5D;
    public static final int OK = 0xFF7BE0A0;
    public static final int TEXT = 0xFFEDEDF0;
    public static final int TEXT_DIM = 0xFF9A9AA6;
    public static final int PANEL_TINT = 0xB0101014;
    public static final int PANEL_BORDER = 0x2EFFFFFF;
    public static final int SHADOW = 0x70000000;
    public static final float PANEL_RADIUS = 7f;
    public static final float PANEL_BLUR = 9f;

    private static TextureTarget frameCopy;

    public static void setAccent(int rgb) {
        ACCENT = 0xFF000000 | (rgb & 0xFFFFFF);
        ACCENT_SOFT = 0x55000000 | (rgb & 0xFFFFFF);
    }

    public static int remapTextColor(int rgb) {
        return (rgb & 0xFFFFFF) == 0xFFAA00 ? ACCENT & 0xFFFFFF : rgb;
    }

    private GuiPanelRender() {
    }

    public static int frameCopyTexture() {
        return frameCopy == null ? 0 : frameCopy.getColorTextureId();
    }

    public static void captureFrame() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (frameCopy == null) {
            frameCopy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            frameCopy.setFilterMode(GL11.GL_LINEAR);
        } else if (frameCopy.width != main.width || frameCopy.height != main.height) {
            frameCopy.resize(main.width, main.height, Minecraft.ON_OSX);
            frameCopy.setFilterMode(GL11.GL_LINEAR);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, frameCopy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, frameCopy.width, frameCopy.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);
    }

    public static void fill(GuiGraphics g, float x, float y, float width, float height, float radius, int color) {
        draw(g, x, y, width, height, radius, 0f, 0f, 0f, color, color);
    }

    public static void gradient(GuiGraphics g, float x, float y, float width, float height, float radius, int top, int bottom) {
        draw(g, x, y, width, height, radius, 0f, 0f, 0f, top, bottom);
    }

    public static void circle(GuiGraphics g, float cx, float cy, float r, int color) {
        fill(g, cx - r, cy - r, r * 2, r * 2, r, color);
    }

    public static void ring(GuiGraphics g, float cx, float cy, float r, float width, int color) {
        stroke(g, cx - r, cy - r, r * 2, r * 2, r, width, color);
    }

    public static void stroke(GuiGraphics g, float x, float y, float width, float height, float radius,
                              float strokeWidth, int color) {
        draw(g, x, y, width, height, radius, strokeWidth, 0f, 0f, color, color);
    }

    public static void shadow(GuiGraphics g, float x, float y, float width, float height, float radius,
                              float spread, int color) {
        draw(g, x - spread, y - spread, width + spread * 2, height + spread * 2, radius + spread,
                0f, 0f, spread * 2, color, color);
    }

    public static void glass(GuiGraphics g, float x, float y, float width, float height, float radius,
                             float blur, int tint) {
        if (frameCopy == null) {
            fill(g, x, y, width, height, radius, tint);
            return;
        }
        float blurPixels = blur * (float) Minecraft.getInstance().getWindow().getGuiScale();
        draw(g, x, y, width, height, radius, 0f, Math.max(blurPixels, 0.001f), 0f, tint, tint);
    }

    public static void panel(GuiGraphics g, float x, float y, float width, float height) {
        panel(g, x, y, width, height, PANEL_RADIUS);
    }

    public static void panel(GuiGraphics g, float x, float y, float width, float height, float radius) {
        shadow(g, x, y + 1, width, height, radius, 6f, SHADOW);
        glass(g, x, y, width, height, radius, PANEL_BLUR, PANEL_TINT);
        gradient(g, x, y, width, Math.min(height, 14f), radius, 0x14FFFFFF, 0x00FFFFFF);
        stroke(g, x, y, width, height, radius, 0.5f, PANEL_BORDER);
    }

    public static void blurFrame(float blur, int tint) {
        if (Shaders.guiPanel == null) {
            return;
        }
        captureFrame();
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        float blurPixels = blur * (float) Minecraft.getInstance().getWindow().getGuiScale();

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0f, 1f, 1f, 0f, -1f, 1f), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        drawQuad(Shaders.guiPanel, new Matrix4f(), 0f, 0f, 1f, 1f, main.width, main.height, 0f, 0f,
                Math.max(blurPixels, 0.001f), 0f, tint, tint);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void draw(GuiGraphics g, float x, float y, float width, float height, float radius,
                             float strokeWidth, float blurPixels, float softness, int top, int bottom) {
        if (width <= 0 || height <= 0) {
            return;
        }
        g.flush();
        ShaderInstance shader = Shaders.guiPanel;
        if (shader == null) {
            if (strokeWidth <= 0 && softness <= 0) {
                g.fillGradient((int) x, (int) y, (int) (x + width), (int) (y + height), top, bottom);
            }
            return;
        }
        drawQuad(shader, g.pose().last().pose(), x, y, width, height, width, height,
                radius, strokeWidth, blurPixels, softness, top, bottom);
    }

    private static void drawQuad(ShaderInstance shader, Matrix4f matrix, float x, float y, float width, float height,
                                 float shapeWidth, float shapeHeight, float radius, float strokeWidth,
                                 float blurPixels, float softness, int top, int bottom) {
        shader.safeGetUniform("Size").set(shapeWidth, shapeHeight);
        shader.safeGetUniform("Radius").set(radius);
        shader.safeGetUniform("StrokeWidth").set(strokeWidth);
        shader.safeGetUniform("BlurRadius").set(blurPixels);
        shader.safeGetUniform("Softness").set(softness);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, blurPixels > 0 ? frameCopy.getColorTextureId() : 0);
        RenderSystem.setShader(() -> shader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vertex(buffer, matrix, x, y, 0, 0, top);
        vertex(buffer, matrix, x, y + height, 0, 1, bottom);
        vertex(buffer, matrix, x + width, y + height, 1, 1, bottom);
        vertex(buffer, matrix, x + width, y, 1, 0, top);
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float u, float v, int color) {
        buffer.vertex(matrix, x, y, 0).uv(u, v)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, color >>> 24).endVertex();
    }

    public static int mix(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round((from >>> 24) + ((to >>> 24) - (from >>> 24)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int alpha(int color, float factor) {
        int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static class Fade {
        private final float speed;
        private float value;
        private long lastTime;

        public Fade() {
            this(12f);
        }

        public Fade(float speed) {
            this.speed = speed;
        }

        public float update(boolean active) {
            long now = System.nanoTime();
            float delta = lastTime == 0 ? 1f : Math.min((now - lastTime) / 1.0e9f, 0.1f);
            lastTime = now;
            float target = active ? 1f : 0f;
            value += (target - value) * Math.min(1f, delta * speed);
            if (Math.abs(target - value) < 0.004f) {
                value = target;
            }
            return value;
        }

        public float get() {
            return value;
        }
    }
}
