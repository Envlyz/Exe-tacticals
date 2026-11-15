package com.exetacticals.client.render.fx.muzzleSmoke.slow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.exetacticals.client.render.RenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class SlowSmokeTask {
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final Quaternionf SCRATCH_ROTATION = new Quaternionf();
    private static final Vector3f SCRATCH_OFFSET = new Vector3f();
    private static final Vector3f SCRATCH_SORT = new Vector3f();
    private static final Quaternionf SCRATCH_CAM_ROT = new Quaternionf();

    private final PoseStack.Pose pose;
    private final long shootTime;
    private final SlowSmokeEntry entry;
    private final SlowSmokeTextureSet textureSet;
    private final int seed;
    private final int textureIndex;
    private final float rotationAngle;
    private final int light;
    private final Quaternionf spawnCameraRotation;
    private final Vec3 spawnCameraPos;
    private final Vector3f spawnWorldPosition;
    private final TransformRes transformRes = new TransformRes();
    private boolean cancelled;
    @Nullable
    private RenderType lastRenderType;

    public SlowSmokeTask(PoseStack.Pose pose, long shootTime, SlowSmokeEntry entry, SlowSmokeTextureSet textureSet) {
        this(pose, shootTime, entry, textureSet, LightTexture.FULL_BRIGHT);
    }

    public SlowSmokeTask(PoseStack.Pose pose, long shootTime, SlowSmokeEntry entry, SlowSmokeTextureSet textureSet, int light) {
        this(pose, shootTime, entry, textureSet, light, (int) (Math.random() * Integer.MAX_VALUE));
    }

    public SlowSmokeTask(PoseStack.Pose pose, long shootTime, SlowSmokeEntry entry, SlowSmokeTextureSet textureSet, int light, int seed) {
        this.pose = com.exetacticals.Utils.copyPose(pose);
        this.shootTime = shootTime;
        this.entry = entry;
        this.textureSet = textureSet;
        this.light = light;
        this.seed = seed;

        this.textureIndex = textureSet == null ? 0 : textureSet.indexBySeed(seed);
        this.rotationAngle = (random01(seed, 49979687) - 0.5f) * TAU;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        this.spawnCameraRotation = new Quaternionf(camera.rotation());
        this.spawnCameraPos = camera.getPosition();

        Vector3f localPosition = new Vector3f();
        this.pose.pose().getTranslation(localPosition);
        localPosition.rotate(spawnCameraRotation);
        this.spawnWorldPosition = localPosition.add(
                (float) spawnCameraPos.x,
                (float) spawnCameraPos.y,
                (float) spawnCameraPos.z
        );
    }

    public PoseStack.Pose getPose() {
        return pose;
    }

    public long getShootTime() {
        return shootTime;
    }

    public SlowSmokeEntry getEntry() {
        return entry;
    }

    public SlowSmokeTextureSet getTextureSet() {
        return textureSet;
    }

    public int getSeed() {
        return seed;
    }

    public int textureIndex() {
        return textureIndex;
    }

    public int getLight() {
        return light;
    }

    public TransformRes transformRes() {
        return transformRes;
    }

    public float progress() {
        return progress(System.currentTimeMillis());
    }

    public float progress(long now) {
        return Mth.clamp((now - shootTime) / (float) entry.fadeDurationMillis(), 0f, 1f);
    }

    public float ageSeconds() {
        return (System.currentTimeMillis() - shootTime) * 0.001f;
    }

    public boolean isFinished() {
        return isFinished(System.currentTimeMillis());
    }

    public boolean isFinished(long now) {
        return cancelled || now - shootTime >= entry.fadeDurationMillis();
    }

    public boolean handleRender(MultiBufferSource bufferSource) {
        lastRenderType = null;
        if (isFinished()) {
            return true;
        }
        SmokeTexture texture = textureSet == null ? null : textureSet.pick(textureIndex);
        if (texture == null) {
            return true;
        }

        float progress = progress();
        updateTransform(progress);
        if (transformRes.isCancel()) {
            cancelled = true;
            return true;
        }

        float alpha = transformRes.alpha() * (texture.baseAlpha());
        float size = texture.baseSize() * entry.localScale() * transformRes.scale();
        if (alpha <= 0f || size <= 0f) {
            return isFinished();
        }
        RenderType renderType = RenderTypes.getSlowMuzzleSmoke(texture.location());
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        render(vertexConsumer, texture, alpha, size);
        lastRenderType = renderType;
        return false;
    }

    @Nullable
    public RenderType lastRenderType() {
        return lastRenderType;
    }

    private void updateTransform(float progress) {
        entry.controller().update(progress, seed, transformRes);
    }

    public void render(VertexConsumer vertexConsumer, SmokeTexture texture, float alpha, float size) {
        PoseStack.Pose renderPose = com.exetacticals.Utils.copyPose(pose);
        applyViewTransform(renderPose);
        if (entry.randomRotate()) {
            SCRATCH_ROTATION.identity().rotateZ(rotationAngle);
            renderPose.pose().rotate(SCRATCH_ROTATION);
        }
        renderPose.pose().scale(size, size, 1f);
        draw(renderPose.pose(), vertexConsumer, alpha, texture);
    }

    private Vector3f currentWorldPosition(Vector3f dest) {
        if (entry.followFlyDir()) {
            dest.set(transformRes.positionOffset());
        } else {
            dest.set(0f, 0f, 0f);
        }
        dest.rotate(spawnCameraRotation);
        return dest.add(spawnWorldPosition);
    }

    private void currentViewPosition(Camera camera, Vector3f dest) {
        currentWorldPosition(dest);

        Vec3 cameraPos = camera.getPosition();
        dest.x -= (float) cameraPos.x;
        dest.y -= (float) cameraPos.y;
        dest.z -= (float) cameraPos.z;

        SCRATCH_CAM_ROT.set(camera.rotation()).conjugate();
        dest.rotate(SCRATCH_CAM_ROT);
    }

    private void applyViewTransform(PoseStack.Pose renderPose) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Matrix4f matrix = renderPose.pose();

        currentViewPosition(camera, SCRATCH_OFFSET);
        matrix.setTranslation(SCRATCH_OFFSET.x, SCRATCH_OFFSET.y, SCRATCH_OFFSET.z);
    }

    public float sortDepth() {
        updateTransform(progress());
        currentViewPosition(Minecraft.getInstance().gameRenderer.getMainCamera(), SCRATCH_SORT);
        return -SCRATCH_SORT.z;
    }

    protected void draw(Matrix4f matrix, VertexConsumer vertexConsumer, float alpha, SmokeTexture texture) {
        vertexConsumer.vertex(matrix, -0.5f, 0.5f, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,1,1).endVertex();
        vertexConsumer.vertex(matrix, 0.5f, 0.5f, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,1,1).endVertex();
        vertexConsumer.vertex(matrix, 0.5f, -0.5f, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,1,1).endVertex();
        vertexConsumer.vertex(matrix, -0.5f, -0.5f, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,1,1).endVertex();
    }

    private static float random01(int seed, int salt) {
        int h = seed * 0x9E3779B9 + salt * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return (h >>> 8) / (float) 0x1000000;
    }
}
