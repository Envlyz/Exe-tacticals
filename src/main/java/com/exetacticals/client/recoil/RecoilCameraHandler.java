package com.exetacticals.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.Utils;
import com.exetacticals.client.model.modular.IGunModel;
import com.exetacticals.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public class RecoilCameraHandler implements IRecoilCameraHandler {
    private static IRecoilCameraHandler INSTANCE;
    private static final float QUARTER_PI = (float) Math.PI / 4;
    private float scale = 1.22f;

    private float pitchToRecovery = 0f;
    private float pitchTotal = 0f;
    private float yawToRecovery = 0f;

    private float lastPlayerXRot;
    private float lastPlayerYRot;

    private boolean isFirstFrame = true;

    private boolean shouldStartRecovery(float speed) {
        float minTimeDist = Math.max(Client.WEAPON_STATUS.getFireInterval() + 0.005f, 0.1f);
        return Client.distFromLastShoot() > minTimeDist &&
                RecoilHandler.INSTANCE.getRecoilUpdater().getRecoilHeat() < 1f;
    }

    @Override
    public void update(float deltaTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            isFirstFrame = true;
            return;
        }

        if (isFirstFrame) {
            lastPlayerXRot = player.getXRot();
            lastPlayerYRot = player.getYRot();
            isFirstFrame = false;
            return;
        }

        float mouseDeltaX = player.getXRot() - lastPlayerXRot;
        float mouseDeltaY = player.getYRot() - lastPlayerYRot;

        IRecoilUpdater updater = RecoilHandler.INSTANCE.getRecoilUpdater();
        Vector2f recoilSpeed = (updater != null) ? updater.getCameraSpeed() : new Vector2f(0, 0);

        float recoilPitchDelta = recoilSpeed.x * scale;
        float recoilYawDelta = recoilSpeed.y * scale;

        pitchToRecovery += recoilPitchDelta;
        pitchTotal += recoilPitchDelta;
        yawToRecovery += recoilYawDelta;

        if (pitchToRecovery != 0 && Math.signum(mouseDeltaX) != Math.signum(pitchToRecovery)) {
            float oldPitchToRecovery = pitchToRecovery;
            pitchToRecovery += mouseDeltaX;
            if (Math.signum(pitchToRecovery) != Math.signum(oldPitchToRecovery)) {
                pitchToRecovery = 0f;
            }
        }

        if (yawToRecovery != 0 && Math.signum(mouseDeltaY) != Math.signum(yawToRecovery)) {
            float oldYawToRecovery = yawToRecovery;
            yawToRecovery += mouseDeltaY;
            if (Math.signum(yawToRecovery) != Math.signum(oldYawToRecovery)) {
                yawToRecovery = 0f;
            }
        }

        if ((pitchToRecovery != 0 || pitchTotal != 0) && shouldStartRecovery(recoilSpeed.x)) {
            float recoverySpeed = 0.2f * deltaTicks * 20f;
            recoverySpeed = Math.min(recoverySpeed, 1.0f);
            float pitchRecovery = pitchToRecovery * recoverySpeed;
            if (pitchToRecovery != 0) {
                pitchToRecovery -= pitchRecovery;
                recoilPitchDelta -= pitchRecovery;
            }
            if (pitchTotal != 0) {
                pitchTotal -= pitchTotal * recoverySpeed;
            }
        }

        if (yawToRecovery != 0 && shouldStartRecovery(recoilSpeed.y)) {
            float recoverySpeed = 0.2f * deltaTicks * 20f;
            float yawRecovery = yawToRecovery * Math.min(recoverySpeed, 1.0f);
            yawToRecovery -= yawRecovery;
            recoilYawDelta -= yawRecovery;
        }

        player.setXRot(player.getXRot() + recoilPitchDelta);
        player.setYRot(player.getYRot() + recoilYawDelta);

        lastPlayerXRot = player.getXRot();
        lastPlayerYRot = player.getYRot();
    }

    @Override
    public void clear() {
        pitchToRecovery = 0f;
        yawToRecovery = 0f;
        isFirstFrame = true;
    }

    @Override
    public void onBobbingView(PoseStack poseStack, float partialTicks, IGun gun) {
        IRecoilUpdater updater = RecoilHandler.INSTANCE.getRecoilUpdater();
        if (updater != null) {
            float timeDis = updater.distFromLastShoot();
            float currentShake = updater.getCamShakeZ();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.isSprinting()) {
                currentShake *= 1.25f;
            }
            float aimingProgress = Client.getAimingProgress(partialTicks);
            float rotX = (float) Utils.dampedOscillation(timeDis, currentShake * 0.4f * (1 - aimingProgress * 0.75f), 80, 0.35f, QUARTER_PI);
            float rotZ = (float) Utils.dampedOscillation(timeDis, currentShake, 40, 0.25f, QUARTER_PI);
            float transZ = (float) Utils.dampedOscillation(timeDis, Math.abs(currentShake) * (1.2f - aimingProgress), 50F, 0.35f, QUARTER_PI * 2);
            poseStack.mulPose(new Quaternionf().rotateXYZ(rotX, 0, rotZ));
            poseStack.translate(0, 0, transZ);
        }
    }

    public void applyGunModelTransform(PoseStack poseStack, boolean aiming, float particleTicks, float recoilHeatRes, IGunModel model) {
        float up = getUp();

    }

    @Override
    public float getUp() {
        return pitchToRecovery;
    }

    public static void _debugReloadInstance(IRecoilCameraHandler recoilCameraHandler) {
        if (!GCR.IS_DEVELOPMENT) return;
        INSTANCE = recoilCameraHandler;
    }

    public static IRecoilCameraHandler getInstance() { return INSTANCE; }

    static { INSTANCE = new RecoilCameraHandler(); }
}