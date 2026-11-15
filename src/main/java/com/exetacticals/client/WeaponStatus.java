package com.exetacticals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.client.model.modular.IScopeModel;
import com.exetacticals.client.model.modular.ModuleModelRegister;
import com.exetacticals.client.recoil.RecoilData;
import com.exetacticals.client.recoil.RecoilHandler;
import com.exetacticals.client.render.GunPoseHandler;
import com.exetacticals.client.render.ModuleRenderNode;
import com.exetacticals.client.screen.modify.GunModifyScreen;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ModuleHandler;
import com.exetacticals.modularSys.builder.Node;
import com.exetacticals.modularSys.fire.IFireMode;
import com.exetacticals.modularSys.modules.IArmHandlerModular;
import com.exetacticals.modularSys.modules.IInteractiveModular;
import com.exetacticals.modularSys.modules.ISight;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.modules.impl.Muzzle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class WeaponStatus {
    public static final java.util.UUID SPEED_ID = java.util.UUID.nameUUIDFromBytes("exe_tacticals:ads_speed_modifier".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private long lastJump;
    private boolean isHoldingGun;
    private ItemStack itemStack;
    private IGun gun;
    private float aimingProgress;
    private float aimingProgressLast;
    public boolean isAiming;
    private Node root;
    private ModuleRenderNode renderRoot;

    private final Object2ObjectOpenHashMap<String, ModuleRenderNode> flatRenderNodeMap = new Object2ObjectOpenHashMap<>();
    private String identityID;
    private int modifyID;
    private IFireMode<?> fireMode;
    private volatile long fireIntervalNanos;
    private volatile boolean fireBlocked;
    public volatile int fireCount;
    public volatile long lastShoot;
    public float aimingSpeed;

    private Node activeSight = null;
    private final Object2ObjectOpenHashMap<String, Node> IDToNodes = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, IModularModel> IDToModels = new Object2ObjectOpenHashMap<>();
    private final List<Pair<IInteractiveModular, Node>> interactiveModules = new ArrayList<>();

    private Vector3f muzzleFlashPos = null;
    private float muzzleFlashRadius = 5f;
    private float muzzleFlashIntensity =  2.5f;
    private float adsZCompensation = 1;
    private float lastAdsZCompensation = 1;

    private float recoilControl;
    private float impulse;
    private float agility;
    private float stability;
    private float length;
    private float weight;
    private float heat;
    private float lastHeat;

    public boolean isSuppressed = false;

    private float cantedAdsIncSpeedFactor = 1;
    private float cantedAdsDecSpeedFactor = 1;

    private IArmHandlerModular leftArmHold;
    private IArmHandlerModular rightArmHold;

    public float shootRandomSeed;

    public WeaponStatus() {

    }

    public ModuleRenderNode getRenderRoot() {
        return renderRoot;
    }

    public Object2ObjectOpenHashMap<String, ModuleRenderNode> getFlatRenderNodeMap() {
        return flatRenderNodeMap;
    }

    public Object2ObjectOpenHashMap<String, IModularModel> getIDToModels() {
        return IDToModels;
    }

    public float getAimingProgress() {
        return aimingProgress;
    }

    public float getAimingProgress(float partialTicks) {
        return Mth.lerp(partialTicks, aimingProgressLast, aimingProgress);
    }

    public boolean isAiming() {
        return isAiming;
    }

    public void onTickEnd(@NotNull Player localPlayer) {
    }

    public void onTickStart(@NotNull Player localPlayer) {
        itemStack = localPlayer.getMainHandItem();
        gun = itemStack.getItem() instanceof GunItem gunItem ? gunItem.getGun() : null;
        isHoldingGun = gun != null;
        if (gun == null) {
            clearGunState();
        } else {
            String id = gun.getIdentityID(itemStack);
            boolean reInitModules = false;
            if (!Objects.equals(id, identityID)) {
                identityID = id;
                reInitModules = true;
            }
            this.fireMode = gun.getFireMode(itemStack);
            if (fireMode != null) {
                int rpm = fireMode.modifyRpm(gun.getRpm(itemStack));
                fireIntervalNanos = IFireMode.rpmToIntervalNanos(rpm);
            } else {
                fireIntervalNanos = 0L;
            }
            int lastModifyID = gun.getModifyID(itemStack);
            if (!reInitModules && !Objects.equals(lastModifyID, modifyID)) {
                reInitModules = true;
            }
            modifyID = lastModifyID;
            if (reInitModules) {
                muzzleFlashRadius = 0;
                muzzleFlashIntensity = 0;
                onModuleTreeChange();
                leftArmHold = gun.getLeftArmHolding(itemStack);
                rightArmHold = gun.getRightArmHolding(itemStack);
                handlePropertiesUpdate(gun, itemStack);
                RecoilData recoilData = getGun().getRecoilData();
                RecoilHandler.INSTANCE.getRecoilUpdater().setRecoilData(recoilData);
            }
            lastHeat = heat;
            heat = gun.getCurrHeat(itemStack, localPlayer.level().getGameTime());
            checkSight();
            handleInteractiveModules();
        }
        handleAds(localPlayer);
    }

    private void checkSight() {
        String usingSightID = gun.getUsingSightID(itemStack);
        activeSight = IDToNodes.get(usingSightID);
        if (activeSight != null && activeSight.getModule() instanceof ISight sight) {
            lastAdsZCompensation = adsZCompensation;
            adsZCompensation = sight.getZCompensation();
            int isSide = activeSight.getUnit().getCustomParam(ISight.ON_SIDE_POSITION);
            if (isSide == 1) {
                cantedAdsIncSpeedFactor = 0.75f;
                cantedAdsDecSpeedFactor = 0.6f;
            } else {
                cantedAdsIncSpeedFactor = 1f;
                cantedAdsDecSpeedFactor = 1f;
            }
        }
    }

    private void handleAds(Player localPlayer) {
        if (Minecraft.getInstance().screen instanceof GunModifyScreen) {
            Client.RIGHT_BUTTON_PRESSED.set(false);
        }
        aimingProgressLast = aimingProgress;
        if (Client.RIGHT_BUTTON_PRESSED.get()) {
            if (aimingProgress < 1) {
                aimingProgress = Math.min(1, aimingProgress + aimingSpeed * cantedAdsIncSpeedFactor);
            }
            float sprintingProgress = SprintingHandler.INSTANCE.getSprintingProgress();
            float sprinting = 1 - sprintingProgress * sprintingProgress;
            float r1 = 1.001f - aimingProgress;
            float r2 = 1.001f - sprinting;
            aimingProgress = (aimingProgress * r1 + sprinting * r2) / (r1 + r2);
            isAiming = true;
        } else {
            float exitSpeed = Math.max(0.3f, aimingSpeed);
            aimingProgress = Math.max(0, aimingProgress - exitSpeed * cantedAdsDecSpeedFactor);
            isAiming = false;
        }
        if (localPlayer == null) {
            return;
        }
        AttributeInstance attr = localPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            if (isAiming) {
                if (attr.getModifier(SPEED_ID) == null) {
                    double speedBonus = calcAdsSpeedModifier();
                    if (speedBonus != 0) {
                        attr.addTransientModifier(
                                new AttributeModifier(
                                        SPEED_ID, "exe_tacticals:ads_speed_modifier",
                                        speedBonus,
                                        AttributeModifier.Operation.MULTIPLY_TOTAL
                                )
                        );
                    }
                }
            } else if (aimingProgress < 0.5f) {
                attr.removeModifier(SPEED_ID);
            }
        }
    }

    public float calcAdsSpeedModifier() {
        float score = agility * 2.2f / weight * 0.486f;
        score = (float) - Math.exp(-Math.pow(score, 0.66f)) * 0.7f;
        return Mth.clamp(score, -0.4f, 0);
    }

    protected void handlePropertiesUpdate(IGun gun, ItemStack itemStack) {
        impulse = gun.getImpulseRatio(itemStack);
        recoilControl = gun.getRecoilControl(itemStack);
        stability = gun.getStability(itemStack);
        agility = gun.getAgility(itemStack);
        weight = gun.getCurrentWeight(itemStack);
        aimingSpeed = calculateFinalAimingSpeed(gun.getAimingSpeed(itemStack), agility, weight);
    }

    public float calculateFinalAimingSpeed(float baseSpeed, float agility, float weight) {
        float k = agility / weight;
        k = (float) Math.pow(k, 0.7f);
        float factor = (float) (- Math.exp(-k) + 1);
        baseSpeed *= (0.5f + factor) * 0.13f;
        return Mth.clamp(baseSpeed, 0.1f, 0.4f);
    }

    private static final long FALLBACK_FIRE_INTERVAL_NANOS = 100_000_000L;

    public long getFireIntervalNanos() {
        long interval = fireIntervalNanos;
        return interval > 0L ? interval : FALLBACK_FIRE_INTERVAL_NANOS;
    }

    public float getFireInterval() {
        return getFireIntervalNanos() * 1e-9f;
    }

    public boolean isFireBlocked() {
        return fireBlocked;
    }

    public void updateFireBlocked() {
        boolean blocked = !isHoldingGun || DrawHolsterHandler.get().getEquipProgress() < 1f;
        if (fireBlocked == blocked) {
            return;
        }
        fireBlocked = blocked;
        if (!blocked && Client.LEFT_BUTTON_PRESSED.get()) {
            ClientWeaponLooper.wake();
        }
    }

    public float getStability() {
        return stability;
    }

    public float getRecoilControl() {
        return recoilControl;
    }

    public float getPlayerDynamicFactor() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return 1;
        }
        float factor = 1f;
        if (player.isCrouching()) {
            factor += 0.15f;
        } else if (player.xxa != 0 || player.yya != 0 || player.zza != 0) {
            factor -= 0.15f;

        } else if (player.isSprinting()) {
            factor -= 0.20f;
        }
        float jumpDist = (System.currentTimeMillis() - lastJump) * 0.001f;
        if (jumpDist < 0.7f) {
            factor = Math.max((factor - (0.7f - jumpDist) * 0.5f), 0.35f);
        }
        return factor;
    }

    public float getImpulse() {
        return impulse;
    }

    public void onShoot() {
        this.shootRandomSeed = (float) Math.random();
    }

    private void handleInteractiveModules() {
        for (Pair<IInteractiveModular, Node> pair : interactiveModules) {
            IInteractiveModular interactive = pair.getLeft();
            Node node = pair.getRight();
            interactive.onClientTick(node.getID(), node.getUnit(), gun, itemStack);
        }
    }

    public Map<String, Node> getIDToNodes() {
        return IDToNodes;
    }

    private void onModuleTreeChange() {
        IDToNodes.clear();
        IDToModels.clear();
        interactiveModules.clear();
        aimingSpeed = gun.getAimingSpeed(itemStack);
        ListTag modulesTag = gun.getModulesTag(itemStack);
        root = Node.read(modulesTag);
        final boolean[] Suppressed = {true};
        if (root != null) {
            updateRenderNodes();
            root.dfs(node -> {
                IModular module = node.getModule();
                IModularModel model = ModuleModelRegister.get(module);
                String id = node.getID();
                int depth = node.getDepth();
                IDToNodes.put(id, node);
                IDToModels.put(id, model);
                if (node.getModule() instanceof IInteractiveModular modular) {
                    interactiveModules.add(Pair.of(modular, node));
                }
                if (Suppressed[0] && node.getModule() instanceof Muzzle muzzle && !muzzle.isSuppressor()) {
                    Suppressed[0] = false;
                }
            });
        }
        isSuppressed = Suppressed[0];
    }

    private void updateRenderNodes() {
        if (root != null) {
            renderRoot = null;
            flatRenderNodeMap.clear();
            renderRoot = ModuleHandler.buildRenderTreeByNode(root);
            if (renderRoot != null) {
                renderRoot.dfsTravel(renderNode ->
                        flatRenderNodeMap.put(renderNode.id, renderNode));
            }
        }
    }

    public void invalidateModelCache() {
        identityID = null;
        modifyID = 0;
        root = null;
        renderRoot = null;
        activeSight = null;
        IDToNodes.clear();
        IDToModels.clear();
        interactiveModules.clear();
        flatRenderNodeMap.clear();
    }

    public IGun getGun() {
        return gun;
    }

    public boolean isSightActivated(String sightID) {
        return activeSight != null && activeSight.getID().equals(sightID);
    }

    public float getLerpAdsZCompensation(float partialTicks) {
        float switchProgress = GunPoseHandler.INSTANCE.getSwitchProgress(partialTicks);
        return Mth.lerp(switchProgress, lastAdsZCompensation, adsZCompensation);
    }

    private void clearGunState() {
        isAiming = false;
        activeSight = null;
        identityID = null;
        muzzleFlashRadius = 0;
        muzzleFlashIntensity = 0;
        heat = 0;
        lastHeat = 0;
        fireIntervalNanos = 0L;
        fireBlocked = true;
    }

    public IFireMode<?> getPrevFireMode() {
        return fireMode;
    }

    public boolean isHoldingGun() {
        return isHoldingGun;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setMuzzleFlashConfig(PoseStack.Pose pose, float muzzleFlashIntensity) {
        if (muzzleFlashIntensity > this.muzzleFlashIntensity) {
            this.muzzleFlashIntensity = muzzleFlashIntensity;
            this.muzzleFlashRadius = this.muzzleFlashIntensity * 1.5f;
            setMuzzleFlashPos(pose);
        }
    }

    private void setMuzzleFlashPos(PoseStack.Pose pose) {
        PoseStack.Pose copy = com.exetacticals.Utils.copyPose(pose);
        copy.pose().translate(0,0.0625f,-0.0625f);
        Vector3f translation = copy.pose().getTranslation(new Vector3f());
        translation.x = 0;
        muzzleFlashPos = translation;
    }

    public Node getActiveSight() {
        return activeSight;
    }

    public Vector3f getMuzzleFlashPos() {
        return muzzleFlashPos;
    }

    public boolean isUsingScope() {
        return activeSight != null && ModuleModelRegister.get(activeSight.getModule()) instanceof IScopeModel;
    }

    public void clearMuzzleFlashModelEffect() {
        muzzleFlashRadius = 0;
        muzzleFlashIntensity = 0;
    }

    public float getMuzzleFlashIntensity() {
        return muzzleFlashIntensity;
    }

    public float getMuzzleFlashRadius() {
        return muzzleFlashRadius;
    }

    public float getWeight() {
        return weight;
    }

    public float getAgility() {
        return agility;
    }

    public IArmHandlerModular getRightArmHold() {
        return rightArmHold;
    }

    public IArmHandlerModular getLeftArmHold() {
        return leftArmHold;
    }

    public void onPlayerJump() {
        lastJump = System.currentTimeMillis();
    }

    public long getLastJump() {
        return lastJump;
    }

    public float getHeat(long nowTick) {
        return gun == null ? 0 : gun.getCurrHeat(itemStack, nowTick);
    }

    public float getHeat(float particleTicks) {
        return Mth.lerp(particleTicks, lastHeat, heat);
    }
}
