package com.exetacticals.items;

import com.exetacticals.data.ModData;
import com.exetacticals.data.PlayerCommonStatus;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ArmPoseHandler implements IClientItemExtensions {

    public static final ArmPoseHandler ARM_POSE_HANDLER = new ArmPoseHandler();

    @Override
    public HumanoidModel.ArmPose getArmPose(@NotNull LivingEntity entityLiving, @NotNull InteractionHand hand, @NotNull ItemStack itemStack) {
        if (ModData.has(entityLiving)) {
            PlayerCommonStatus data = ModData.get(entityLiving);
            return data.isReloading() ?
                    HumanoidModel.ArmPose.CROSSBOW_CHARGE :
                    HumanoidModel.ArmPose.BOW_AND_ARROW;
        } else {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
    }
}