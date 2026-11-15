package com.exetacticals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.exetacticals.Utils;
import com.exetacticals.client.model.BoneRenderStatus;
import com.exetacticals.client.model.modular.state.IStateViewer;
import com.exetacticals.client.model.modular.state.IStateViewerModel;
import com.exetacticals.client.model.modular.state.ReadOnlyTag;
import com.exetacticals.modularSys.modules.guns.IGun;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ModifyScreenRenderContext extends ModuleRenderContext implements IRenderStatusCacheContext {
    private final Map<ModuleRenderNode, Map<String, BoneRenderStatus>> tempPoseMap = new HashMap<>();
    private static final ReadOnlyTag EMPTY_STATES = new ReadOnlyTag(new CompoundTag());

    public ModifyScreenRenderContext(ModuleRenderNode root, ItemStack itemStack, float partialTicks, int light, int overlay, ItemDisplayContext displayContext, IGun gun, PoseStack poseStack, MultiBufferSource bufferSource) {
        super(null, root, itemStack, partialTicks, light, overlay, displayContext, gun, poseStack, bufferSource);
    }

    @Override
    protected void renderNodeTree(ModuleRenderNode root) {
        this.currentRenderNode = root;
        root.initTranslate(poseStack);
        handleStates();
        handleRender(root.model);
        Map<String, BoneRenderStatus> temp = new HashMap<>();
        root.model.getRootBone().deptFirstTravel(bone -> temp.put(bone.name, bone.renderStatus.copy()));
        tempPoseMap.put(root, temp);
        if (root.slots != null) {
            for (Map.Entry<String, List<ModuleRenderNode>> entry : root.slots.entrySet()) {
                String key = entry.getKey();
                PoseStack.Pose slotPose = root.model.getBonePose(key);
                if (slotPose != null) {
                    Utils.overridePose(poseStack, slotPose);
                    for (ModuleRenderNode node : entry.getValue()) {
                        poseStack.pushPose();
                        renderNodeTree(node);
                        poseStack.popPose();
                    }
                }
            }
        }
    }

    protected void handleStates() {
        if (currentRenderNode().model instanceof IStateViewerModel<?> stateViewerModel) {
            IStateViewer<?> viewer = stateViewerModel.getViewer();
            if (viewer != null) {
                viewer.applyState(currentRenderNode().model, this, EMPTY_STATES);
            }
            clearStateLockedBones();
        }
    }

    @Override
    public Map<ModuleRenderNode, Map<String, BoneRenderStatus>> getRenderStatusMap() {
        return tempPoseMap;
    }
}
