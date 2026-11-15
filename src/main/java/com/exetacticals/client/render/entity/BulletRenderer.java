package com.exetacticals.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.compat.IrisCompat;
import com.exetacticals.entity.projectile.BulletEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BulletRenderer extends EntityRenderer<BulletEntity> {
    private static final ResourceLocation TEXTURE = GCR.RL("textures/entity/tracers.png");
    private static final float TAIL = 10f;
    private static final float CORE_HALF = 0.022f;
    private static final float GLOW_HALF = 0.08f;
    private static final float HEAD_RADIUS = 0.13f;
    private static final float SCALE_FROM = 12f;

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BulletEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(@NotNull BulletEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (IrisCompat.isRenderingShadowPass() || !entity.isTracer()) {
            return;
        }
        boolean firstPerson = Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
        Vector3f translation = poseStack.last().pose().getTranslation(new Vector3f());
        if (firstPerson) {
            if (entity.getShooterId() != Client.LOCAL_PLAYER_ID && entity.tickCount < 2) {
                return;
            }
            if (translation.length() < 6.5f) {
                return;
            }
        } else {
            if (entity.tickCount < 2) {
                return;
            }
        }
        Vector3f velocity = entity.getEntityData().get(BulletEntity.EXACT_VELOCITY);
        if (velocity.length() < 0.001f) {
            return;
        }

        poseStack.pushPose();

        Camera mainCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        LocalPlayer player = Minecraft.getInstance().player;
        boolean doFPOffsetTrans = player != null && player.getId() == entity.getShooterId() && firstPerson;
        if (doFPOffsetTrans) {
            Vector3f visualFPMuzzlePos = Client.getGunRenderer().getGunLocalPos();
            if (visualFPMuzzlePos != null) {
                float dist = translation.length();
                float exp = (float) Math.exp((-dist) * 0.07f);
                if (exp > 0.001f) {
                    Vector3f offset = new Vector3f(visualFPMuzzlePos);
                    offset.rotate(mainCamera.rotation());
                    offset.mul(exp);
                    poseStack.translate(offset.x, offset.y, offset.z);
                }
            }
        }

        float scale = Math.max(1f, translation.length() / SCALE_FROM);
        VertexConsumer buf = bufferSource.getBuffer(RenderType.lightning());

        poseStack.pushPose();
        poseStack.mulPose(mainCamera.rotation());
        Matrix4f head = poseStack.last().pose();
        disc(buf, head, HEAD_RADIUS * scale, 1.0f, 0.45f, 0.15f, 0.55f);
        disc(buf, head, HEAD_RADIUS * 0.35f * scale, 1.0f, 0.92f, 0.75f, 1.0f);
        poseStack.popPose();

        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * (180F / Math.PI));
        float hDist = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float pitch = (float) (Mth.atan2(velocity.y, hDist) * (180F / Math.PI));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        Matrix4f m = poseStack.last().pose();
        for (int plane = 0; plane < 2; plane++) {
            tail(buf, m, plane, GLOW_HALF * scale, 0.35f);
            tail(buf, m, plane, CORE_HALF * scale, 1.0f);
        }

        poseStack.popPose();
    }

    private static void tail(VertexConsumer buf, Matrix4f m, int plane, float half, float alpha) {
        float[] z = {0f, TAIL * 0.12f, TAIL * 0.45f, TAIL};
        float[][] col = {
                {1.0f, 0.85f, 0.6f, 1.0f},
                {1.0f, 0.55f, 0.2f, 0.85f},
                {0.95f, 0.22f, 0.08f, 0.45f},
                {0.7f, 0.08f, 0.04f, 0f},
        };
        for (int i = 0; i < z.length - 1; i++) {
            float h0 = half * (1f + 0.6f * z[i] / TAIL);
            float h1 = half * (1f + 0.6f * z[i + 1] / TAIL);
            for (int s = -1; s <= 1; s += 2) {
                float[] c0 = col[i], c1 = col[i + 1];
                float a0 = c0[3] * alpha, a1 = c1[3] * alpha;
                float ex0 = plane == 0 ? s * h0 : 0, ey0 = plane == 1 ? s * h0 : 0;
                float ex1 = plane == 0 ? s * h1 : 0, ey1 = plane == 1 ? s * h1 : 0;
                v(buf, m, 0, 0, z[i], c0, a0);
                v(buf, m, ex0, ey0, z[i], c0, 0f);
                v(buf, m, ex1, ey1, z[i + 1], c1, 0f);
                v(buf, m, 0, 0, z[i + 1], c1, a1);

                v(buf, m, 0, 0, z[i + 1], c1, a1);
                v(buf, m, ex1, ey1, z[i + 1], c1, 0f);
                v(buf, m, ex0, ey0, z[i], c0, 0f);
                v(buf, m, 0, 0, z[i], c0, a0);
            }
        }
    }

    private static void disc(VertexConsumer buf, Matrix4f m, float r, float red, float green, float blue, float alpha) {
        int n = 12;
        float[] c = {red, green, blue};
        for (int i = 0; i < n; i++) {
            double t0 = Math.PI * 2 * i / n, t1 = Math.PI * 2 * (i + 1) / n;
            float x0 = (float) Math.cos(t0) * r, y0 = (float) Math.sin(t0) * r;
            float x1 = (float) Math.cos(t1) * r, y1 = (float) Math.sin(t1) * r;
            v(buf, m, 0, 0, 0, c, alpha);
            v(buf, m, 0, 0, 0, c, alpha);
            v(buf, m, x0, y0, 0, c, 0f);
            v(buf, m, x1, y1, 0, c, 0f);

            v(buf, m, 0, 0, 0, c, alpha);
            v(buf, m, x1, y1, 0, c, 0f);
            v(buf, m, x0, y0, 0, c, 0f);
            v(buf, m, 0, 0, 0, c, alpha);
        }
    }

    private static void v(VertexConsumer buf, Matrix4f m, float x, float y, float z, float[] c, float a) {
        buf.vertex(m, x, y, z).color(c[0], c[1], c[2], a).endVertex();
    }
}
