package com.exetacticals.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.exetacticals.GCR;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

public class Shaders {
    private static final java.util.Set<String> DIAG_SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Logs a render-path diagnostic message once per key (avoids per-frame spam). */
    public static void diagOnce(String key, String message) {
        if (DIAG_SEEN.add(key)) {
            GCR.LOGGER.warn("[GCR RenderDiag] {}", message);
        }
    }


    /**
     * Сплошной цвет для {@code entity_cutout_ubo} (RGBA; альфа 0 — обычная отрисовка). Ставится на время прохода силуэта
     * для обводки ({@code client.render.GunWorkbenchRenderer}) и сразу сбрасывается.
     */
    public static final float[] solidColor = {0f, 0f, 0f, 0f};

    public static ShaderInstance entityCutOutUBO;
    /** Скруглённые панели GUI (заливка, обводка, стекло с блюром), см. {@code client.screen.GuiPanelRender}. */
    public static ShaderInstance guiPanel;
    /** MSDF-текст (шрифт тултипов, {@code client.tooltip.MsdfFont}). */
    public static ShaderInstance msdfFont;
    /** Белая обводка оружия на столе верстака ({@code client.render.fx.GunOutline}). */
    public static ShaderInstance gunOutline;
    /** Круговое меню магазинов ({@code client.screen.MagazineWheelScreen}): лепестки, подсветка, стекло. */
    public static ShaderInstance radialWheel;

    public static ShaderInstance getEntityCutOutUBO() {
        return entityCutOutUBO;
    }

    public static void init(ResourceProvider provider) {
        try {
            entityCutOutUBO = new ShaderInstance(provider, GCR.RL(GCR.MODID, "entity_cutout_ubo"), DefaultVertexFormat.NEW_ENTITY);
            GCR.LOGGER.info("[GCR] entity_cutout_ubo shader loaded, program id {}", entityCutOutUBO.getId());
        } catch (Exception e) {
            GCR.LOGGER.error("[GCR] failed to load entity_cutout_ubo shader", e);
        }
        try {
            guiPanel = new ShaderInstance(provider, GCR.RL(GCR.MODID, "gui_panel"), DefaultVertexFormat.POSITION_TEX_COLOR);
        } catch (Exception e) {
            guiPanel = null;
            GCR.LOGGER.error("[GCR] failed to load gui_panel shader", e);
        }
        try {
            gunOutline = new ShaderInstance(provider, GCR.RL(GCR.MODID, "gun_outline"), DefaultVertexFormat.POSITION);
        } catch (Exception e) {
            gunOutline = null;
            GCR.LOGGER.error("[GCR] failed to load gun_outline shader", e);
        }
        try {
            radialWheel = new ShaderInstance(provider, GCR.RL(GCR.MODID, "radial_wheel"), DefaultVertexFormat.POSITION_TEX_COLOR);
        } catch (Exception e) {
            radialWheel = null;
            GCR.LOGGER.error("[GCR] failed to load radial_wheel shader", e);
        }
        try {
            msdfFont = new ShaderInstance(provider, GCR.RL(GCR.MODID, "mtsdf_font"), DefaultVertexFormat.POSITION_TEX_COLOR);
        } catch (Exception e) {
            msdfFont = null;
            GCR.LOGGER.error("[GCR] failed to load mtsdf_font shader", e);
        }
    }
}
