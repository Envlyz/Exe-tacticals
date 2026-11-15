package com.exetacticals.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.exetacticals.Client;
import com.exetacticals.client.DrawHolsterHandler;
import com.exetacticals.client.GunEffect;
import com.exetacticals.client.GunEffectManager;
import com.exetacticals.client.animation.CameraAnimationHandler;
import com.exetacticals.client.events.RenderEvents;
import com.exetacticals.client.model.Bone;
import com.exetacticals.client.model.modular.*;
import com.exetacticals.client.model.modular.animation.eventSys.AnimationEventBus;
import com.exetacticals.client.model.modular.animation.eventSys.EventRegistry;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.client.recoil.RecoilHandler;
import com.exetacticals.client.render.fx.bulletShell.BulletShellDisplay;
import com.exetacticals.client.render.fx.bulletShell.BulletShellRenderer;
import com.exetacticals.compat.IrisCompat;
import com.exetacticals.items.DisplayData;
import com.exetacticals.items.GunItem;
import com.exetacticals.mixin.GameRendererAccessor;
import com.exetacticals.mixin.RenderSystemAccessor;
import com.exetacticals.mixinUtils.DualHandItemAccessor;
import com.exetacticals.modularSys.ModuleHandler;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.modules.views.IStateView;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class DefaultGunRenderer implements IGunRenderer {
    private static final Vector3f GUN_LOCAL_POS = new Vector3f();
    private static final Matrix4f FP_MODEL_VIEW_MAT = new Matrix4f();
    public static final int GUN_MODIFY_SCREEN_LIGHT = LightTexture.pack(12,15);
    private FirstPersonRenderContext cachedFPContext;
    private final PoseStack firstPersonPoseStack = new PoseStack();
    private final PoseStack gunModifyPoseStack = new PoseStack();
    private AnimationEventBus animationEventBus;
    private List<ModuleRenderNode> models;
    private IBulletShellHandlerModel<?> bulletShellHandlerModel;
    private String bulletShellHandlerNodeID = "";
    private int lastFPModifyID = -1;
    private String lastFPIdentityID = "";
    private String lastUsingSightID = "";
    private boolean hideFPRender = false;
    private long lastShootMain = 0;

    private static final BufferBuilder FP_COMMON_BUFFER = new BufferBuilder(256 * 256);
    private static final Map<RenderType, BufferBuilder> FP_IMMEDIATE_BUFFERS = new Object2ObjectLinkedOpenHashMap<>();
    private static final MultiBufferSource.BufferSource FP_BUFFER_SOURCE = MultiBufferSource
            .immediateWithBuffers(FP_IMMEDIATE_BUFFERS, FP_COMMON_BUFFER);

    private static final BufferBuilder GUN_MODIFY_BUFFER = new BufferBuilder(256 * 256);
    private static final MultiBufferSource.BufferSource GUN_MODIFY_BUFFER_SOURCE = MultiBufferSource.immediate(GUN_MODIFY_BUFFER);

    private static Runnable sightPoseUpdateTask = null;

    private static final Vector3f TMP_LIGHT0 = new Vector3f();
    private static final Vector3f TMP_LIGHT1 = new Vector3f();
    private static final Quaternionf TMP_INV_CAM_ROT = new Quaternionf();

    private final Vector3f currCameraRot = new Vector3f();
    private final List<DelayedEvent> delayedEvents = new ArrayList<>();
    private long fpRenderTimeStamp = 0;

    private record DelayedEvent(EventType type, @Nullable Map<String, String> params) {
    }

    public static PoseStack TEST_STACK = new PoseStack();

    @Override
    public void renderFirstPerson(LocalPlayer player, ItemStack itemStack, IGun gun, PoseStack poseStack, int light, int overlay) {
        if (hideFPRender || IrisCompat.isRenderingShadowPass()) {
            return;
        }
        String identityId = gun.getIdentityID(itemStack);
        int modifyId = gun.getModifyID(itemStack);
        boolean doUpdateCache = modifyId != lastFPModifyID || !Objects.equals(identityId, lastFPIdentityID);
        DisplayData displayData = gun.getDisplayData();
        float partialTicks = Minecraft.getInstance().getFrameTime();
        firstPersonPoseStack.setIdentity();
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().last().pose().set(firstPersonPoseStack.last().pose());
        RenderSystem.applyModelViewMatrix();
        FP_MODEL_VIEW_MAT.set(RenderSystem.getModelViewMatrix());
        setUpLightDir();
        IGunModel gunModel = (IGunModel) ModuleModelRegister.get(gun);
        if (Client.isUsingIrisShader) {
            firstPersonPoseStack.scale(0.25f, 0.25f, 0.25f);
        }
        HardCodeAnimationHandler.getInstance().applyTransformPre(firstPersonPoseStack, gun, partialTicks, player);

        if (cachedFPContext != null) {
            cachedFPContext.itemStack = itemStack;
        }

        firstPersonPoseStack.mulPose(new Quaternionf().rotateXYZ(currCameraRot.x, currCameraRot.y, currCameraRot.z));

        GunPoseHandler.INSTANCE.handleFirstPersonTransform(firstPersonPoseStack, displayData, partialTicks);
        firstPersonPoseStack.last().pose().getTranslation(GUN_LOCAL_POS);
        HardCodeAnimationHandler.getInstance().applyTransformPost(firstPersonPoseStack, gun, partialTicks, player);
        AftermatchWeaponMotion.INSTANCE.apply(firstPersonPoseStack, player, partialTicks);

        TEST_STACK.setIdentity();

        RecoilHandler.INSTANCE.applyTransformPost(TEST_STACK, Client.isAiming(), partialTicks, gunModel);

        firstPersonPoseStack.mulPoseMatrix(TEST_STACK.last().pose());

        if (cachedFPContext == null || doUpdateCache) {
            if (doUpdateCache) {
                clearCache();
            }
            ModuleRenderNode moduleRenderNode = ModuleHandler.buildRenderTree(itemStack);
            if (moduleRenderNode != null) {
                FirstPersonRenderContext context = new FirstPersonRenderContext(
                        player, moduleRenderNode, itemStack, partialTicks, light, overlay, gun, firstPersonPoseStack, FP_BUFFER_SOURCE);
                cachedFPContext = context;
                newFirstPersonContextInit();
                frameUpdate(light, overlay, partialTicks);
                if (DrawHolsterHandler.get().shouldDispatchDraw) {
                    Client.getGunRenderer().dispatchAnimationEvent(EventType.DRAW);
                    DrawHolsterHandler.get().shouldDispatchDraw = false;
                }
                for (DelayedEvent delayed : delayedEvents) {
                    dispatchAnimationEvent(delayed.type(), delayed.params());
                }
                delayedEvents.clear();
                renderFirstPerson(context);
                lastFPModifyID = modifyId;
                lastFPIdentityID = identityId;
            }
        } else {
            frameUpdate(light, overlay, partialTicks);
            renderFirstPerson(cachedFPContext);
        }

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private void frameUpdate(int light, int overlay, float partialTicks) {
        String usingSightID = cachedFPContext.gun.getUsingSightID(cachedFPContext.itemStack);
        if (!Objects.equals(lastUsingSightID, usingSightID)) {
            GunPoseHandler.INSTANCE.calculateSightPose(usingSightID, cachedFPContext, () -> renderFirstPerson(cachedFPContext));
        }
        lastUsingSightID = usingSightID;

        cachedFPContext.frameUpdate(firstPersonPoseStack, light, overlay, partialTicks);
    }

    private void setUpLightDir() {
        Vector3f[] shaderLightDirections = RenderSystemAccessor.getShaderLightDirections();
        TMP_LIGHT0.set(shaderLightDirections[0]);
        TMP_LIGHT1.set(shaderLightDirections[1]);
        Camera cam = ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).getMainCamera();
        cam.rotation().conjugate(TMP_INV_CAM_ROT);
        TMP_LIGHT0.rotate(TMP_INV_CAM_ROT);
        TMP_LIGHT1.rotate(TMP_INV_CAM_ROT);
        RenderSystem.setShaderLights(TMP_LIGHT0, TMP_LIGHT1);
    }

    @Override
    public void tick(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        ItemInHandRenderer itemInHandRenderer = mc.gameRenderer.itemInHandRenderer;
        DualHandItemAccessor accessor = (DualHandItemAccessor) itemInHandRenderer;
        ItemStack mainHandItem = accessor.getMainHandItem();
        if (mainHandItem.isEmpty()) {
            clearCache();
            return;
        }
        if (!(mainHandItem.getItem() instanceof GunItem)) {
            float equipProgress = DrawHolsterHandler.get().getEquipProgress();
            if (equipProgress <= 0) {
                clearCache();
            }
        }
    }

    @Override
    public void renderTickPre(float partialTicks) {

    }

    @Override
    public void renderTickPost(float partialTicks) {

    }

    @Override
    public void setHideFPRender(boolean hide) {
        hideFPRender = hide;
    }

    @Override
    public boolean isHideFPRender() {
        return hideFPRender;
    }

    @Override
    public void renderGunModifyScreen(ItemStack itemStack, IGun gun, ModuleRenderNode node,
                                      float x, float y, float rx, float ry, float scale, Consumer<ModifyScreenRenderContext> guiCallback) {
        if (node == null) {
            return;
        }
        DisplayData displayData = gun.getDisplayData();
        if (displayData != null) {
            if (Client.isUsingIrisShader) {
                RenderEvents.addDelayedEntityRenderTask(() -> {
                    RenderSystem.backupProjectionMatrix();
                    RenderSystem.setProjectionMatrix(Client.FIRST_PERSON_PROJECTION_MAT, VertexSorting.DISTANCE_TO_ORIGIN);
                    renderGunModifyScreen(itemStack, gun, node, x, y, rx, ry, scale, guiCallback, displayData);
                    RenderSystem.restoreProjectionMatrix();
                });
            } else {
                RenderEvents.setFinalStageDelayedRenderTask(() -> {
                    if (Minecraft.getInstance().screen instanceof com.exetacticals.client.screen.modify.GunModifyScreen) {
                        com.exetacticals.client.screen.GuiPanelRender.blurFrame(
                                com.exetacticals.client.screen.modify.GunModifyScreen.BACKGROUND_BLUR,
                                com.exetacticals.client.screen.modify.GunModifyScreen.BACKGROUND_TINT);
                    }
                    RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
                    renderGunModifyScreen(itemStack, gun, node, x, y, rx, ry, scale, guiCallback, displayData);
                });
            }
        }
    }

    @Override
    public float getSightPoseDistance() {
        return GunPoseHandler.INSTANCE.getPosZ();
    }

    @Override
    public Matrix4f firstPersonModelViewMat() {
        return FP_MODEL_VIEW_MAT;
    }

    @Override
    public Vector3f getGunLocalPos() {
        return new Vector3f(GUN_LOCAL_POS);
    }

    @Override
    public long getCurrFPRenderTimeStampNano() {
        if (fpRenderTimeStamp == 0) {
            return System.nanoTime();
        } else {
            return fpRenderTimeStamp;
        }
    }

    @Override
    public PoseStack getEnvDisturbance() {
        return TEST_STACK;
    }

    private void renderGunModifyScreen(ItemStack itemStack, IGun gun, ModuleRenderNode node,
                                       float x, float y, float rx, float ry, float scale, Consumer<ModifyScreenRenderContext> guiCallback, DisplayData displayData) {
        gunModifyPoseStack.setIdentity();
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().last().pose().set(gunModifyPoseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        displayData.applyGunModifyScreenTranslation(gunModifyPoseStack, x, y, rx, ry, scale);
        float partialTicks = Minecraft.getInstance().getFrameTime();

        ModifyScreenRenderContext context = new ModifyScreenRenderContext(node, itemStack, partialTicks, GUN_MODIFY_SCREEN_LIGHT, OverlayTexture.NO_OVERLAY, null, gun, gunModifyPoseStack, GUN_MODIFY_BUFFER_SOURCE);
        context.startRender();
        if (guiCallback != null) {
            guiCallback.accept(context);
        }
        node.dfsTravel((n -> {
            context.currentRenderNode = n;
            n.model.afterAllRendered(context);
        }));
        GUN_MODIFY_BUFFER_SOURCE.endBatch();

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private void clearCache() {
        cachedFPContext = null;
        animationEventBus = null;
        bulletShellHandlerModel = null;
        bulletShellHandlerNodeID = "";
    }

    @Override
    public void invalidateRenderCache() {
        clearCache();
        if (models != null) {
            models.clear();
        }
        lastFPModifyID = -1;
        lastFPIdentityID = null;
        lastUsingSightID = null;
        delayedEvents.clear();
    }

    private void newFirstPersonContextInit() {
        animationEventBus = new AnimationEventBus();

        if (models == null) {
            models = new ArrayList<>();
        } else {
            models.clear();
        }

        cachedFPContext.root.dfsTravel((node) -> {
            if (node.model instanceof IAnimationControllerModel controllerModel) {
                controllerModel.getController().ifPresent(controller -> {
                    for (EventRegistry eventRegistry : controller.getAllSubscriptions()) {
                        this.animationEventBus.register(eventRegistry, node, controller);
                    }
                });
            }

            models.add(node);
            if (bulletShellHandlerModel == null && node.model instanceof IBulletShellHandlerModel<?> model) {
                this.bulletShellHandlerModel = model;
                this.bulletShellHandlerNodeID = node.id;
            }
        });
        animationEventBus.finish();

        if (sightPoseUpdateTask != null) {
            sightPoseUpdateTask.run();
            sightPoseUpdateTask = null;
        }

        cachedFPContext.onContextInit();
        GunPoseHandler.INSTANCE
                .calculateSightPose(
                        cachedFPContext.gun.getUsingSightID(cachedFPContext.itemStack),
                        cachedFPContext, () -> renderFirstPerson(cachedFPContext));
    }
    protected void rawLight(FirstPersonRenderContext context) {
        float v = Client.distFromLastShoot();
        if (v < 0.03f) {
            float particleTick = context.partialTicks;
            LivingEntity entity = context.entity;
            int blockLight = entity.isOnFire() ? 15 :
                    entity.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(entity.getEyePosition(particleTick)));
            int lightInc = Client.WEAPON_STATUS.isSuppressed ? 3 : 5;
            context.light = LightTexture.pack((int) Math.min(15, blockLight + Math.min(lightInc, v * 200)), entity.level().getBrightness(LightLayer.SKY,
                    BlockPos.containing(entity.getEyePosition(particleTick))));
        }
    }
    protected void renderFirstPerson(FirstPersonRenderContext context) {
        rawLight(context);
        LightDiag.fpLight = context.light;
        context.calcPose();
        if (context.renderMode) {
            Bone camera = context.root.model.getBone("camera");
            if (camera != null) {
                currCameraRot.set(camera.xRot, camera.yRot, camera.zRot);
                currCameraRot.mul(DrawHolsterHandler.get().getEquipProgress(context.partialTicks));
                CameraAnimationHandler.INSTANCE.set(currCameraRot);
            }
            fpRenderTimeStamp = System.nanoTime();
        }
        context.startRender();
        if (context.renderMode) {
            handleBulletShell(context);
        }
        FP_BUFFER_SOURCE.endBatch();
        for (ModuleRenderNode node : models) {
            context.currentRenderNode = node;
            node.model.afterAllRendered(context);
        }

        FP_BUFFER_SOURCE.endBatch();
        context.clearLocalStorage();
        context.clearBufferCache();

    }

    @SuppressWarnings("unchecked")
    private <T extends IStateView> void handleBulletShell(FirstPersonRenderContext context) {
        long effectTimestamp = GunEffectManager.getEffectTimestamp(context.entity.getId(), GunEffect.SHOOT, context.root.id);
        if (bulletShellHandlerModel != null && lastShootMain != effectTimestamp) {
            IStateView view = bulletShellHandlerModel.getView();
            IBulletShellHandler<T> bulletShellHandler = (IBulletShellHandler<T>) bulletShellHandlerModel.getBulletShellHandler();
            boolean b = bulletShellHandler.shouldThrowBulletShell((T) view, context.getNodeStates(bulletShellHandlerNodeID));
            ReadOnlyTag.clear();
            if (!b) {
                return;
            }
            BulletShellDisplay bulletShellDisplay = bulletShellHandler.getBulletShellDisplay();
            PoseStack.Pose offsetPose = bulletShellHandler.getOffsetPose();
            BulletShellRenderer.push(
                    bulletShellDisplay,
                    bulletShellHandler.getModel(),
                    offsetPose,
                    effectTimestamp);
            lastShootMain = effectTimestamp;
        }
    }

    @Override
    public void dispatchAnimationEvent(EventType eventType) {
        dispatchAnimationEvent(eventType, (Map<String, String>) null);
    }

    @Override
    public void dispatchAnimationEvent(EventType eventType, String... params) {
        if (params == null || params.length < 2) {
            dispatchAnimationEvent(eventType);
            return;
        }
        int size = params.length - (params.length % 2);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i += 2) {
            map.put(params[i], params[i + 1]);
        }
        dispatchAnimationEvent(eventType, map);
    }

    @Override
    public void dispatchAnimationEvent(EventType eventType, @Nullable Map<String, String> params) {
        if (cachedFPContext != null) {
            animationEventBus.dispatch(eventType, cachedFPContext, params);
        } else {
            delayedEvents.add(new DelayedEvent(eventType, params));
        }
    }

    @Override
    public void renderOther(LivingEntity entity, ItemStack itemStack, IGun gun, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        DisplayData displayData = gun.getDisplayData();
        if (displayData == null) {
            return;
        }
        ModuleRenderNode moduleRenderNode = ModuleHandler.buildRenderTree(itemStack);
        if (moduleRenderNode != null) {
            float partialTicks = Minecraft.getInstance().getFrameTime();
            displayData.applyTranslation(poseStack, displayContext, partialTicks);
            ModuleRenderContext context = new ModuleRenderContext(entity, moduleRenderNode, itemStack, partialTicks, light, overlay, displayContext, gun, poseStack, bufferSource);
            context.startRender();
            moduleRenderNode.dfsTravel((node -> node.model.afterAllRendered(context)));
        }
    }
}
