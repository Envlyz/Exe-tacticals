package com.exetacticals.modularSys.task.reload;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.Utils;
import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.AnimationRegister;
import com.exetacticals.client.animation.command.MaskMagAmmo;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.modularSys.modules.IAmmoSource;
import com.exetacticals.modularSys.modules.guns.ak.AK;
import com.exetacticals.modularSys.task.GunTask;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.network.c2s.GunReloadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.exetacticals.network.GCRNetwork;

import java.awt.*;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class AKReloadTask extends GunTask<AK> {
    private String animationName;
    private IAmmoSource magAttachment;
    public int sendPacketDelay;

    public AKReloadTask(ItemStack itemStack, AK gun) {
        super(itemStack, gun);
        CompoundTag states = gun.rootNodeTag(itemStack);
        boolean hasMagAttachment = gun.hasMagAttachment(states);
        if (!hasMagAttachment) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("exe_tacticals.overlay.require_mag").withStyle(style -> style.withColor(Color.RED.getRGB())));
            }
            length = 0;
            return;
        }
        int ammoLeft = gun.getGunAmmoLeft(itemStack);

        if (ammoLeft > 0) {
            animationName = "mag_reload";
        } else {
            animationName = "mag_reload_empty";
        }

        Map<String, Float> taskTimers = gun.baseProperties.taskTimers;
        String sendPacketDelayKey = animationName + "_length";
        sendPacketDelay = Utils.secondToTick(taskTimers.getOrDefault(sendPacketDelayKey, 1.0f));
        magAttachment = gun.getMagAttachment(itemStack);
        //TODO:将task长度写入属性中而不是根据动画名字推算
        AnimationDef animationDef = AnimationRegister.get(GCR.RL(gun.getID().split(":")[1] + "_" + animationName));
        if (animationDef != null) {
            this.length = Math.max(Utils.secondToTick(animationDef.lengthInSeconds() - 0.1f), sendPacketDelay);
        }
    }

    @Override
    public boolean equals(IGunTask<?> other) {
        if (!(other instanceof AKReloadTask otherTask) || !Objects.equals(otherTask.animationName, this.animationName)) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public void start() {
        if (animationName != null) {
            Client.getGunRenderer().dispatchAnimationEvent(EventType.RELOAD, Map.of("animation_name", animationName));
        }
    }

    @Override
    public void onTick(Player player) {
        super.onTick(player);
        if (tick == sendPacketDelay) {
            GCRNetwork.sendToServer(new GunReloadPacket());
        }
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onComplete() {

    }


    @Override
    public TaskType getType() {
        return TaskType.RELOAD;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    protected int getPredictedAmmoCount() {
        return magAttachment == null ? 1 : magAttachment.getMaxCapacity();
    }

    @Override
    public int getCustomVariable(String variableName) {
        if (MaskMagAmmo.VARIABLE_KEY.equals(variableName)) {
            return getPredictedAmmoCount();
        }
        return -1;
    }
}
