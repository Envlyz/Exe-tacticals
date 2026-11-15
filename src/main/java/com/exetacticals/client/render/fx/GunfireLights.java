package com.exetacticals.client.render.fx;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class GunfireLights {
    private static final int MAX_LIGHTS = 8;
    private static final float DURATION_MS = 60f;
    public static float debugHoldMs = 0f;
    private static final float COLOR_R = 1.0f;
    private static final float COLOR_G = 0.72f;
    private static final float COLOR_B = 0.42f;

    private record Light(Vec3 pos, long startMs, float radius, float strength) {}

    private static final List<Light> LIGHTS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private static final Matrix4f invProj = new Matrix4f();
    private static final Matrix4f invViewRot = new Matrix4f();
    private static boolean matricesReady;

    private static int program = -1;
    private static int vao = -1;
    private static int locDepth, locFabulous, locInvProj, locInvView, locCount, locPos, locColor;
    private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer POS = BufferUtils.createFloatBuffer(MAX_LIGHTS * 4);
    private static final FloatBuffer COL = BufferUtils.createFloatBuffer(MAX_LIGHTS * 4);

    private GunfireLights() {
    }

    public static void onShot(Entity shooter, IGun gun, ItemStack weapon) {
        if (shooter == null) {
            return;
        }
        MuzzleFlashLightProfile profile = MuzzleFlashLightProfile.forGun(gun, weapon);
        if (profile.envRadius() <= 0f) {
            return;
        }
        Vec3 look = shooter.getViewVector(1f);
        Vec3 pos = shooter.getEyePosition(1f).add(look.scale(0.9)).add(0, -0.15, 0);
        float jitter = 0.85f + RANDOM.nextFloat() * 0.3f;
        Light light = new Light(pos, System.currentTimeMillis(), profile.envRadius(), profile.envStrength() * jitter);
        synchronized (LIGHTS) {
            if (LIGHTS.size() >= MAX_LIGHTS * 4) {
                LIGHTS.remove(0);
            }
            LIGHTS.add(light);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && hasLights() && Minecraft.useShaderTransparency()) {
            FabulousDepthTextureHandler.mergeDepthThisFrame();
        }
    }

    private static boolean hasLights() {
        synchronized (LIGHTS) {
            long now = System.currentTimeMillis();
            LIGHTS.removeIf(l -> now - l.startMs() > DURATION_MS + debugHoldMs);
            return !LIGHTS.isEmpty();
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            invProj.set(event.getProjectionMatrix()).invert();
            invViewRot.set(event.getPoseStack().last().pose()).invert();
            matricesReady = true;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            render(event.getCamera());
        }
    }

    private static void render(Camera camera) {
        if (!matricesReady || Client.isUsingIrisShader || !hasLights()) {
            return;
        }
        if (program == -1 && !init()) {
            return;
        }
        long now = System.currentTimeMillis();
        Vec3 cam = camera.getPosition();
        POS.clear();
        COL.clear();
        int count = 0;
        synchronized (LIGHTS) {
            for (Iterator<Light> it = LIGHTS.iterator(); it.hasNext() && count < MAX_LIGHTS; ) {
                Light l = it.next();
                float t = Math.max(0f, now - l.startMs() - debugHoldMs) / DURATION_MS;
                if (t < 0f || t > 1f) {
                    continue;
                }
                float k = (1f - t) * (1f - t) * l.strength();
                Vec3 rel = l.pos().subtract(cam);
                if (rel.lengthSqr() > 64 * 64) {
                    continue;
                }
                POS.put((float) rel.x).put((float) rel.y).put((float) rel.z).put(l.radius());
                COL.put(COLOR_R * k).put(COLOR_G * k).put(COLOR_B * k).put(1f);
                count++;
            }
        }
        if (count == 0) {
            return;
        }
        POS.flip();
        COL.flip();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        GL20.glUseProgram(program);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(FabulousDepthTextureHandler.getDepthTextureId());
        GL20.glUniform1i(locDepth, 0);
        GL20.glUniform1i(locFabulous, Minecraft.useShaderTransparency() && FabulousDepthTextureHandler.fabulousDepthMergeTarget != null ? 1 : 0);
        MAT.clear();
        GL20.glUniformMatrix4fv(locInvProj, false, invProj.get(MAT));
        MAT.clear();
        GL20.glUniformMatrix4fv(locInvView, false, invViewRot.get(MAT));
        GL20.glUniform1i(locCount, count);
        GL20.glUniform4fv(locPos, POS);
        GL20.glUniform4fv(locColor, COL);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);

        ProgramManager.glUseProgram(0);
        RenderSystem.bindTexture(0);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static boolean init() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        String vsh = load(rm, GCR.RL("shaders/core/muzzle_flash_env.vsh"));
        String fsh = load(rm, GCR.RL("shaders/core/gunfire_light.fsh"));
        if (vsh == null || fsh == null) {
            program = -2;
            return false;
        }
        int vs = compile(GL20.GL_VERTEX_SHADER, vsh);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, fsh);
        if (vs == 0 || fs == 0) {
            program = -2;
            return false;
        }
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("[GunfireLights] link failed: {}", GL20.glGetProgramInfoLog(p, 1024));
            program = -2;
            return false;
        }
        program = p;
        locDepth = GL20.glGetUniformLocation(p, "DepthSampler");
        locFabulous = GL20.glGetUniformLocation(p, "IsFabulous");
        locInvProj = GL20.glGetUniformLocation(p, "InvProjMat");
        locInvView = GL20.glGetUniformLocation(p, "InvViewRotMat");
        locCount = GL20.glGetUniformLocation(p, "LightCount");
        locPos = GL20.glGetUniformLocation(p, "LightPosRadius");
        locColor = GL20.glGetUniformLocation(p, "LightColor");

        float[] quad = {
                -1f, -1f, 0f, 0f, 0f,
                1f, -1f, 0f, 1f, 0f,
                -1f, 1f, 0f, 0f, 1f,
                1f, 1f, 0f, 1f, 1f
        };
        FloatBuffer buf = BufferUtils.createFloatBuffer(quad.length);
        buf.put(quad).flip();
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        int posLoc = GL20.glGetAttribLocation(p, "Position");
        int uvLoc = GL20.glGetAttribLocation(p, "UV0");
        GL20.glVertexAttribPointer(posLoc, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(posLoc);
        GL20.glVertexAttribPointer(uvLoc, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(uvLoc);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GCR.LOGGER.info("[GunfireLights] shader ready");
        return true;
    }

    private static int compile(int type, String source) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, source);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GCR.LOGGER.error("[GunfireLights] compile failed: {}", GL20.glGetShaderInfoLog(s, 1024));
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    private static String load(ResourceManager manager, ResourceLocation path) {
        return manager.getResource(path).map(res -> {
            try (InputStream in = res.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                return null;
            }
        }).orElse(null);
    }
}
