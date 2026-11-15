package com.exetacticals.client.render;

import com.exetacticals.GCR;
import com.exetacticals.client.model.BufferedBoneMeshModel;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.ModuleModelRegister;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.items.GunItem;
import com.exetacticals.items.MagazineItem;
import com.exetacticals.items.ModuleItem;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.modules.Mag;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class ModuleItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final float FIT_SIZE = 0.9f;
    public static final float WORLD_SCALE = 0.15f;
    public static final float MIN_WORLD_SIZE = 0.2f;
    private static final String NODE_ID = "exe_tacticals_item_preview";

    private static ModuleItemRenderer instance;

    private final Map<IModularModel, float[]> boundsCache = new WeakHashMap<>();
    private final Set<String> warned = new HashSet<>();

    private ModuleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static ModuleItemRenderer get() {
        if (instance == null) {
            instance = new ModuleItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack,
                             @NotNull MultiBufferSource bufferSource, int light, int overlay) {
        if (!(stack.getItem() instanceof ModuleItem<?> item) || item instanceof GunItem) {
            return;
        }
        IModular module = item.getModule();
        IModularModel model = ModuleModelRegister.get(module);
        float[] b = model == null ? null : getBounds(model);
        if (b == null) {
            if (warned.add(module.getID())) {
                GCR.LOGGER.warn("No 3D model/bounds for module item {}", module.getID());
            }
            return;
        }
        float sizeX = b[3] - b[0], sizeY = b[4] - b[1], sizeZ = b[5] - b[2];
        float extent = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (extent <= 1e-6f) {
            return;
        }
        boolean fit = displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED;
        float scale = fit ? FIT_SIZE / extent : Math.max(WORLD_SCALE, MIN_WORLD_SIZE / extent);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-(b[0] + b[3]) * 0.5f, -(b[1] + b[4]) * 0.5f, -(b[2] + b[5]) * 0.5f);

        ModuleRenderNode node = new ModuleRenderNode(model, NODE_ID, module.getID(), false);
        if (item instanceof MagazineItem magItem && module instanceof Mag mag) {
            CompoundTag states = new CompoundTag();
            mag.setAmmoLeft(magItem.getRounds(stack), states);
            node.setStates(new ReadOnlyTag(states));
        }
        float partialTicks = Minecraft.getInstance().getFrameTime();
        ModuleRenderContext context = new ModuleRenderContext(null, node, stack, partialTicks, light, overlay,
                ItemDisplayContext.NONE, null, poseStack, bufferSource);
        try {
            context.startRender();
        } finally {
            node.dfsTravel(n -> n.model.afterAllRendered(context));
            poseStack.popPose();
        }
    }

    private float[] getBounds(IModularModel model) {
        if (!(model instanceof BufferedBoneMeshModel mesh)) {
            return null;
        }
        float[] cached = boundsCache.get(model);
        if (cached == null && !boundsCache.containsKey(model)) {
            model.resetPose();
            cached = mesh.computeBounds();
            boundsCache.put(model, cached);
        }
        return cached;
    }
}
