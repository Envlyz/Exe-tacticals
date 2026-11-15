package com.exetacticals.client.render;

import com.exetacticals.GCR;
import com.exetacticals.mixin.RenderSystemAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.joml.Vector3f;

import java.util.Arrays;

public final class LightDiag {
    private static long last;
    public static int fpLight = -1;

    private LightDiag() {}

    public static void capture(ShaderInstance shader) {
        long now = System.currentTimeMillis();
        if (now - last < 2000) return;
        last = now;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        BlockPos eye = BlockPos.containing(mc.player.getEyePosition());
        String biome = mc.level.getBiome(eye).unwrapKey().map(k -> k.location().toString()).orElse("?");
        Vector3f[] dirs = RenderSystemAccessor.getShaderLightDirections();
        GCR.LOGGER.warn("[GCR LightDiag] biome={} fpLight=block {} sky {} | world block={} sky={} skyDarken={} rain={} | "
                        + "color={} | fog start={} end={} color={} shape={} camFluid={} | L0={} L1={} | lightmapTex={}",
                biome,
                fpLight < 0 ? -1 : LightTexture.block(fpLight), fpLight < 0 ? -1 : LightTexture.sky(fpLight),
                mc.level.getBrightness(LightLayer.BLOCK, eye), mc.level.getBrightness(LightLayer.SKY, eye),
                mc.level.getSkyDarken(), mc.level.getRainLevel(1f),
                Arrays.toString(RenderSystem.getShaderColor()),
                RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd(),
                Arrays.toString(RenderSystem.getShaderFogColor()), RenderSystem.getShaderFogShape(),
                mc.gameRenderer.getMainCamera().getFluidInCamera(),
                dirs[0], dirs[1], RenderSystem.getShaderTexture(2));
    }
}
