package com.exetacticals.client.render;

import com.exetacticals.Client;
import com.exetacticals.client.model.BufferedBoneMeshModel;
import com.exetacticals.client.render.fx.GunOutline;
import com.exetacticals.GCRModules;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.ModuleModelRegister;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.modules.Mag;
import net.minecraft.nbt.CompoundTag;
import com.exetacticals.modularSys.ModuleHandler;
import com.exetacticals.workbench.GunWorkbenchBlock;
import com.exetacticals.workbench.GunWorkbenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GunWorkbenchRenderer implements BlockEntityRenderer<GunWorkbenchBlockEntity> {
    private static final float CENTER_X = 0f;
    private static final float CENTER_Z = 6.6f / 16f;
    private static final float TABLE_Y = 15.5f / 16f;
    private static final float GUN_LENGTH = 1.35f;
    private static final java.util.Map<Object, float[]> BOUNDS = new java.util.WeakHashMap<>();
    private static final java.util.Map<ItemStack, ItemStack> RENDER_COPIES = new java.util.WeakHashMap<>();
    private static final java.util.Map<ItemStack, float[]> TREE_BOUNDS = new java.util.WeakHashMap<>();
    private static final float DIAGONAL = 10f;
    private static final float MAT_THICKNESS = 0.05f / 16f;
    private static final float MAG_X = -8.4f / 16f;
    private static final float MAG_Z = 3.0f / 16f;
    private static final float MAG_YAW = 20f;
    private static final float MAG_SIZE = 4.6f / 16f;
    private static final float FOOT_HALF_LEN = 10.9f / 16f;
    private static final float FOOT_HALF_H = 3.7f / 16f;
    private static final float FLIP = 0f;
    private static final MultiBufferSource DISCARD = type -> NoopVertexConsumer.INSTANCE;

    public GunWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull GunWorkbenchBlockEntity be, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = be.crafted();
        Minecraft mc = Minecraft.getInstance();
        Direction facing = be.getBlockState().getValue(GunWorkbenchBlock.FACING);
        int topLight = be.getLevel() != null ? LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above()) : light;
        pose.pushPose();
        transform(pose, facing);
        drawSpareMag(pose, buffers, topLight, overlay);
        if (stack.isEmpty()) {
            pose.popPose();
            return;
        }
        Shaders.diagOnce("wb_render", "workbench table: rendering " + stack + " at " + be.getBlockPos());
        try {
            draw(stack, pose, buffers, topLight, overlay, false);
        } catch (RuntimeException e) {
            Shaders.diagOnce("workbench_gun", "workbench table gun render failed: " + e);
        }
        if (mc.player != null && com.exetacticals.client.WorkbenchPickupInput.isTargeted(be) && be.outlineVisibleFor(mc.player)) {
            drawOutline(mc, stack, pose, topLight, overlay);
        }
        pose.popPose();
    }

    private static void transform(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(CENTER_X, TABLE_Y, CENTER_Z);
    }

    private static void draw(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean silhouette) {
        if (stack.getItem() instanceof GunItem gun) {
            drawGun(stack, gun, pose, silhouette ? DISCARD : buffers, light, overlay);
        } else {
            pose.pushPose();
            pose.translate(0, 0.03, 0);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.scale(0.5f, 0.5f, 0.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay,
                    pose, buffers, null, 0);
            pose.popPose();
        }
    }

    private static void drawGun(ItemStack crafted, GunItem gun, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = renderStack(crafted, gun);
        ModuleRenderNode node = ModuleHandler.buildRenderTree(stack);
        if (node == null || !(node.model instanceof BufferedBoneMeshModel mesh)) {
            Shaders.diagOnce("wb_gun_node", "workbench table: no mesh root for " + stack + ", node="
                    + (node == null ? "null" : node.model));
            return;
        }
        float[] b = TREE_BOUNDS.get(stack);
        boolean measure = b == null;
        if (measure) {
            b = modelBounds(mesh);
            if (b == null) {
                return;
            }
        }
        float sizeX = b[3] - b[0], sizeY = b[4] - b[1], sizeZ = b[5] - b[2];
        float extent = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (extent <= 1e-6f) {
            return;
        }
        float scale = Math.min(GUN_LENGTH / extent, Math.min(2f * FOOT_HALF_LEN / sizeZ, 2f * FOOT_HALF_H / sizeY));
        float cx = (b[0] + b[3]) * 0.5f, cy = (b[1] + b[4]) * 0.5f, cz = (b[2] + b[5]) * 0.5f;
        pose.pushPose();
        pose.translate(0, sizeX * scale * 0.5f + MAT_THICKNESS + 0.003f, 0);
        pose.mulPose(Axis.YP.rotationDegrees(DIAGONAL + FLIP));
        pose.mulPose(Axis.ZP.rotationDegrees(90f));
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(scale, scale, scale);
        org.joml.Matrix4f inverse = new org.joml.Matrix4f(pose.last().pose()).invert();
        pose.translate(-cx, -cy, -cz);
        float partialTicks = Minecraft.getInstance().getFrameTime();
        ModuleRenderContext context = measure
                ? new MeasuringContext(node, stack, partialTicks, light, overlay, gun, pose, buffers, inverse)
                : new ModuleRenderContext(null, node, stack, partialTicks, light, overlay,
                ItemDisplayContext.NONE, gun.getGun(), pose, buffers);
        try {
            context.startRender();
        } finally {
            node.dfsTravel(n -> n.model.afterAllRendered(context));
            pose.popPose();
        }
        if (context instanceof MeasuringContext m && m.bounds[0] <= m.bounds[3]) {
            TREE_BOUNDS.put(stack, new float[]{m.bounds[0] + cx, m.bounds[1] + cy, m.bounds[2] + cz,
                    m.bounds[3] + cx, m.bounds[4] + cy, m.bounds[5] + cz});
        }
    }

    private static float[] modelBounds(BufferedBoneMeshModel mesh) {
        float[] b = BOUNDS.get(mesh);
        if (b == null) {
            mesh.resetPose();
            b = mesh.computeBounds();
            if (b != null) {
                BOUNDS.put(mesh, b);
            }
        }
        return b;
    }

    private static final class MeasuringContext extends ModuleRenderContext {
        final float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        private final org.joml.Matrix4f inverse;

        MeasuringContext(ModuleRenderNode root, ItemStack stack, float partialTicks, int light, int overlay, GunItem gun,
                         PoseStack pose, MultiBufferSource buffers, org.joml.Matrix4f inverse) {
            super(null, root, stack, partialTicks, light, overlay, ItemDisplayContext.NONE, gun.getGun(), pose, buffers);
            this.inverse = inverse;
        }

        @Override
        protected void handleRender(com.exetacticals.client.model.modular.IModularModel model) {
            if (model instanceof BufferedBoneMeshModel mesh) {
                org.joml.Matrix4f toGun = new org.joml.Matrix4f(inverse).mul(poseStack.last().pose());
                float[] b = modelBounds(mesh);
                if (b != null) {
                    org.joml.Vector3f v = new org.joml.Vector3f();
                    for (int i = 0; i < 8; i++) {
                        v.set((i & 1) == 0 ? b[0] : b[3], (i & 2) == 0 ? b[1] : b[4], (i & 4) == 0 ? b[2] : b[5]);
                        toGun.transformPosition(v);
                        bounds[0] = Math.min(bounds[0], v.x);
                        bounds[1] = Math.min(bounds[1], v.y);
                        bounds[2] = Math.min(bounds[2], v.z);
                        bounds[3] = Math.max(bounds[3], v.x);
                        bounds[4] = Math.max(bounds[4], v.y);
                        bounds[5] = Math.max(bounds[5], v.z);
                    }
                }
            }
            super.handleRender(model);
        }
    }

    private static void drawSpareMag(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        IModular module = GCRModules.AKM_STEEL_MAG;
        IModularModel model = ModuleModelRegister.get(module);
        if (!(model instanceof BufferedBoneMeshModel mesh)) {
            Shaders.diagOnce("wb_mag", "workbench table: no model for spare magazine " + module.getID());
            return;
        }
        float[] b = modelBounds(mesh);
        if (b == null) {
            return;
        }
        float sizeX = b[3] - b[0], sizeY = b[4] - b[1], sizeZ = b[5] - b[2];
        float extent = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (extent <= 1e-6f) {
            return;
        }
        float scale = MAG_SIZE / extent;
        pose.pushPose();
        pose.translate(MAG_X - CENTER_X, sizeX * scale * 0.5f + MAT_THICKNESS + 0.003f, MAG_Z - CENTER_Z);
        pose.mulPose(Axis.YP.rotationDegrees(MAG_YAW));
        pose.mulPose(Axis.ZP.rotationDegrees(90f));
        pose.scale(scale, scale, scale);
        pose.translate(-(b[0] + b[3]) * 0.5f, -(b[1] + b[4]) * 0.5f, -(b[2] + b[5]) * 0.5f);
        ModuleRenderNode node = new ModuleRenderNode(model, "exe_tacticals_workbench_mag", module.getID(), false);
        if (module instanceof Mag mag) {
            CompoundTag states = new CompoundTag();
            mag.setAmmoLeft(mag.getMaxCapacity(), states);
            node.setStates(new ReadOnlyTag(states));
        }
        ModuleRenderContext context = new ModuleRenderContext(null, node, ItemStack.EMPTY,
                Minecraft.getInstance().getFrameTime(), light, overlay, ItemDisplayContext.NONE, null, pose, buffers);
        try {
            context.startRender();
        } catch (RuntimeException e) {
            Shaders.diagOnce("wb_mag_fail", "workbench table: spare magazine render failed: " + e);
        } finally {
            node.dfsTravel(n -> n.model.afterAllRendered(context));
            pose.popPose();
        }
    }

    private static ItemStack renderStack(ItemStack crafted, GunItem gun) {
        if (crafted.getTag() != null && !crafted.getTag().isEmpty()) {
            return crafted;
        }
        ItemStack cached = RENDER_COPIES.get(crafted);
        if (cached == null) {
            cached = crafted.copy();
            gun.getGun().checkAndGetRaw(cached);
            RENDER_COPIES.put(crafted, cached);
        }
        return cached;
    }

    private static void drawOutline(Minecraft mc, ItemStack stack, PoseStack pose, int light, int overlay) {
        if (!(stack.getItem() instanceof GunItem) || !GunOutline.available()) {
            return;
        }
        if (Client.isUsingIrisShader) {
            Shaders.diagOnce("wb_outline_iris", "workbench table: outline skipped (Iris shaders)");
            return;
        }
        try {
            GunOutline.beginMask();
            Shaders.solidColor[0] = 1f;
            Shaders.solidColor[1] = 1f;
            Shaders.solidColor[2] = 1f;
            Shaders.solidColor[3] = 1f;
            draw(stack, pose, DISCARD, light, overlay, true);
        } catch (RuntimeException e) {
            Shaders.diagOnce("wb_outline_fail", "workbench table: outline failed: " + e);
        } finally {
            Shaders.solidColor[3] = 0f;
            GunOutline.endMask();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull GunWorkbenchBlockEntity be) {
        return true;
    }

    private static final class NoopVertexConsumer implements VertexConsumer {
        static final NoopVertexConsumer INSTANCE = new NoopVertexConsumer();

        @Override
        public @NotNull VertexConsumer vertex(double x, double y, double z) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer color(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
