package com.exetacticals.client.tooltip;

import com.exetacticals.GCR;
import com.exetacticals.client.render.Shaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MSDF-шрифт (атласы из ExeInventory: {@code textures/font/sf_regular|sf_medium.png/json}, шейдер {@code mtsdf_font}).
 * Чёткий на любом масштабе GUI. Рисует строки и {@link FormattedCharSequence} с цветами из стилей.
 * Размер — em в единицах GUI, y — базовая линия.
 */
@OnlyIn(Dist.CLIENT)
public final class MsdfFont {
    private static final Map<String, MsdfFont> CACHE = new HashMap<>();

    public static MsdfFont regular() {
        return get("sf_regular");
    }

    public static MsdfFont medium() {
        return get("sf_medium");
    }

    /** null, если атлас не загрузился — тогда вызывающий рисует ванильным шрифтом. */
    public static MsdfFont get(String name) {
        return CACHE.computeIfAbsent(name, MsdfFont::load);
    }

    /** Сбросить кэш (при перезагрузке ресурсов). */
    public static void clear() {
        CACHE.clear();
    }

    private record Glyph(float advance, float u0, float v0, float u1, float v1, float pl, float pb, float pr, float pt) {
        boolean hasQuad() {
            return u1 > u0 && v1 > v0;
        }
    }

    private final ResourceLocation texture;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Long, Float> kerning = new HashMap<>();
    private final float distanceRange;
    private final float glyphSize;
    private final float ascender;
    private boolean filterSet;

    private static MsdfFont load(String name) {
        ResourceLocation texture = GCR.RL(GCR.MODID, "textures/font/" + name + ".png");
        ResourceLocation metrics = GCR.RL(GCR.MODID, "textures/font/" + name + ".json");
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(metrics);
            if (res.isEmpty()) {
                GCR.LOGGER.error("MSDF metrics not found: {}", metrics);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                return new MsdfFont(texture, JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (Exception e) {
            GCR.LOGGER.error("Failed to load MSDF font {}", name, e);
            return null;
        }
    }

    private MsdfFont(ResourceLocation texture, JsonObject root) {
        this.texture = texture;
        JsonObject atlas = root.getAsJsonObject("atlas");
        float atlasW = atlas.get("width").getAsFloat();
        float atlasH = atlas.get("height").getAsFloat();
        this.distanceRange = atlas.has("distanceRange") ? atlas.get("distanceRange").getAsFloat() : 10f;
        this.glyphSize = atlas.has("size") ? atlas.get("size").getAsFloat() : 64f;
        JsonObject metrics = root.getAsJsonObject("metrics");
        this.ascender = metrics.has("ascender") ? metrics.get("ascender").getAsFloat() : 1f;

        for (JsonElement el : root.getAsJsonArray("glyphs")) {
            JsonObject g = el.getAsJsonObject();
            int code = g.get("unicode").getAsInt();
            float advance = g.get("advance").getAsFloat();
            if (!g.has("atlasBounds") || !g.has("planeBounds")) {
                glyphs.put(code, new Glyph(advance, 0, 0, 0, 0, 0, 0, 0, 0));
                continue;
            }
            JsonObject ab = g.getAsJsonObject("atlasBounds");
            JsonObject pb = g.getAsJsonObject("planeBounds");
            // yOrigin = bottom → V переворачивается
            glyphs.put(code, new Glyph(advance,
                    ab.get("left").getAsFloat() / atlasW, 1f - ab.get("top").getAsFloat() / atlasH,
                    ab.get("right").getAsFloat() / atlasW, 1f - ab.get("bottom").getAsFloat() / atlasH,
                    pb.get("left").getAsFloat(), pb.get("bottom").getAsFloat(),
                    pb.get("right").getAsFloat(), pb.get("top").getAsFloat()));
        }
        if (root.has("kerning")) {
            for (JsonElement el : root.getAsJsonArray("kerning")) {
                JsonObject k = el.getAsJsonObject();
                kerning.put(pack(k.get("unicode1").getAsInt(), k.get("unicode2").getAsInt()), k.get("advance").getAsFloat());
            }
        }
    }

    private static long pack(int a, int b) {
        return (((long) a) << 32) | (b & 0xffffffffL);
    }

    private Glyph glyph(int code) {
        Glyph g = glyphs.get(code);
        if (g == null) {
            g = glyphs.get((int) '?');
        }
        return g != null ? g : new Glyph(0.3f, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private float kern(int left, int right) {
        Float v = kerning.get(pack(left, right));
        return v != null ? v : 0f;
    }

    /** Высота заглавных над базовой линией, для вертикального центрирования. */
    public float ascent(float size) {
        return ascender * size;
    }

    public float width(String text, float size) {
        float w = 0f;
        int prev = -1;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (prev != -1) {
                w += kern(prev, cp) * size;
            }
            w += glyph(cp).advance * size;
            prev = cp;
        }
        return w;
    }

    public float width(FormattedCharSequence text, float size) {
        float[] w = {0f};
        int[] prev = {-1};
        text.accept((index, style, cp) -> {
            if (prev[0] != -1) {
                w[0] += kern(prev[0], cp) * size;
            }
            w[0] += glyph(cp).advance * size;
            prev[0] = cp;
            return true;
        });
        return w[0];
    }

    public void draw(GuiGraphics g, String text, float x, float baseline, float size, int argb) {
        draw(g, FormattedCharSequence.forward(text, net.minecraft.network.chat.Style.EMPTY), x, baseline, size, argb, false);
    }

    /**
     * Рисует текст; цвет символа — из его стиля, иначе {@code argb}.
     * {@code shadow} — мягкая тень на 0.6 единицы вниз-вправо.
     */
    public void draw(GuiGraphics g, FormattedCharSequence text, float x, float baseline, float size, int argb, boolean shadow) {
        ShaderInstance shader = Shaders.msdfFont;
        if (shader == null) {
            return;
        }
        g.flush();
        if (!filterSet) {
            Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(true, false);
            filterSet = true;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("PxRange").set(Math.max(distanceRange * size / glyphSize
                * (float) Minecraft.getInstance().getWindow().getGuiScale(), 1f));

        Matrix4f matrix = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        if (shadow) {
            emit(buf, matrix, text, x + 0.6f, baseline + 0.6f, size, 0x90000000, true);
        }
        emit(buf, matrix, text, x, baseline, size, argb, false);
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
    }

    private void emit(BufferBuilder buf, Matrix4f matrix, FormattedCharSequence text, float x, float baseline,
                      float size, int argb, boolean fixedColor) {
        float[] cursor = {x};
        int[] prev = {-1};
        int baseAlpha = argb >>> 24;
        text.accept((index, style, cp) -> {
            if (prev[0] != -1) {
                cursor[0] += kern(prev[0], cp) * size;
            }
            int color = argb;
            TextColor tc = style.getColor();
            if (!fixedColor && tc != null) {
                color = (baseAlpha << 24) | com.exetacticals.client.screen.GuiPanelRender.remapTextColor(tc.getValue() & 0xFFFFFF);
            }
            Glyph gl = glyph(cp);
            if (gl.hasQuad()) {
                int r = (color >> 16) & 0xFF;
                int gg = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                int a = color >>> 24;
                float x0 = cursor[0] + gl.pl * size;
                float x1 = cursor[0] + gl.pr * size;
                float y0 = baseline - gl.pt * size;
                float y1 = baseline - gl.pb * size;
                buf.vertex(matrix, x0, y1, 0).uv(gl.u0, gl.v1).color(r, gg, b, a).endVertex();
                buf.vertex(matrix, x1, y1, 0).uv(gl.u1, gl.v1).color(r, gg, b, a).endVertex();
                buf.vertex(matrix, x1, y0, 0).uv(gl.u1, gl.v0).color(r, gg, b, a).endVertex();
                buf.vertex(matrix, x0, y0, 0).uv(gl.u0, gl.v0).color(r, gg, b, a).endVertex();
            }
            cursor[0] += gl.advance * size;
            prev[0] = cp;
            return true;
        });
    }
}
