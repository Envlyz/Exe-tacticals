package com.exetacticals.modularSys.modules.guns;

import com.exetacticals.client.recoil.RecoilData;
import com.exetacticals.items.DisplayData;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.builder.IReadOnlyTree;
import com.exetacticals.modularSys.fire.IFireMode;
import com.exetacticals.modularSys.modules.IAmmoSource;
import com.exetacticals.modularSys.modules.IArmHandlerModular;
import com.exetacticals.modularSys.modules.IStateModular;
import com.exetacticals.modularSys.modules.gunProperties.IProperties;
import com.exetacticals.modularSys.modules.gunProperties.impl.BaseProperties;
import com.exetacticals.modularSys.modules.states.*;
import com.exetacticals.modularSys.modules.views.IGunView;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.network.c2s.GunFirePacket;
import com.exetacticals.network.s2c.GunFireAckPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface IGun extends IModular, IAmmoSource, IGunView, IStateModular {
    String NONE = "__none__";
    String MODULE_DATE_KEY = "gcr_module_date";
    String IDENTITY_ID_KEY = "gcr_identity_id";
    String MODIFY_ID_KEY = "gcr_modify_id";
    String ROOT_NODE_ID_KEY = "gcr_root_node_id";

    String PROPERTIES_KEY = "gcr_properties";
    String MODULES_KEY = "gcr_modules";
    String STATES_KEY = "gcr_states";

    String DATA_CHANGED_KEY = "gcr_data_changed";

    int FIRE_SOUND_NORMAL = 0;
    int FIRE_SOUND_SUPPRESSED = 1;

    Str USING_SIGHT = new Str("using_sight");
    Str USING_AMMO_SOURCE = new Str("using_ammo_source");
    Str LEFT_ARM_HOLD = new Str("left_arm_hold");
    Str RIGHT_ARM_HOLD = new Str("right_arm_hold");
    Str FIRE_MODEL_ID = new Str("fire_mode_id", "none");
    Int FIRE_SOUND_TYPE = new Int("fire_sound_type", 0);
    Bool STUCK = new Bool("stuck");
    Num HEAT = new Num("heat", 0);
    LInt HEAT_LAST_UPDATE = new LInt("heat_last_update");
    LInt LAST_SHOOT_TIME = new LInt("last_shoot");
    Num HEAT_STUCK_RATIO = new Num("heat_stuck_ratio", 1);

    void serverShoot(LivingEntity entity, ItemStack itemStack, int shootID, GunFirePacket packet);

    void serverShootRejected(ServerPlayer player, ItemStack itemStack, int shootID);

    void syncStuck(ServerPlayer player, ItemStack itemStack);

    void fullSyncFromServer(ItemStack itemStack, CompoundTag data);

    @OnlyIn(Dist.CLIENT)
    void clientShoot(Player player, ItemStack itemStack);

    @OnlyIn(Dist.CLIENT)
    void serverShootAck(GunFireAckPacket packet, ItemStack itemStack);

    DisplayData getDisplayData();

    long getDataDate(ItemStack itemStack);

    void setDataDate(ItemStack itemStack, long dataDate);

    void reload(ItemStack itemStack, Player player);

    void serverInitData(ItemStack itemStack);

    CompoundTag checkAndGet(ItemStack itemStack);

    CompoundTag checkAndGetRaw(ItemStack itemStack);

    ListTag getModulesTag(ItemStack itemStack);

    CompoundTag getPropertiesTag(ItemStack itemStack);

    String rootNodeId(ItemStack itemStack);

    CompoundTag rootNodeTag(ItemStack itemStack);

    @Nullable
    CompoundTag getNodeStatesTag(ItemStack itemStack, String nodeId);
    CompoundTag getAmmoSourceTag(ItemStack itemStack);
    CompoundTag getStatesTag(ItemStack itemStack);

    void setStatesTag(ItemStack itemStack, CompoundTag states);

    void setModulesTag(ItemStack itemStack, ListTag nodes);

    void setPropertiesTag(ItemStack itemStack, CompoundTag properties);

    String getIdentityID(ItemStack itemStack);

    int getModifyID(ItemStack itemStack);

    void setModifyID(ItemStack itemStack, int modifyID, boolean copy);

    int getRpm(ItemStack itemStack);

    boolean isStuck(ItemStack itemStack);

    void removeStuck(ItemStack itemStack);

    void setStuck(boolean stuck, CompoundTag states);

    IFireMode<?> getFireMode(ItemStack itemStack);
    List<IFireMode<?>> getAllFireModes();
    void setFireMode(ItemStack itemStack, IFireMode<?> fireMode);
    void toNextFireMode(ItemStack itemStack);

    String getUsingSightID(ItemStack itemStack);

    String getLeftArmHoldID(ItemStack itemStack);
    String getRightArmHoldID(ItemStack itemStack);

    void setUsingSightID(ItemStack itemStack, String sightID);

    int getAmmoLeft(ItemStack itemStack);

    void setAmmoLeft(ItemStack itemStack, int ammoLeft);

    @Nullable
    IGunTask<?> getTask(ItemStack itemStack, IGunTask.TaskType type, Map<String, Object> args);

    RecoilData getRecoilData();

    Map<String, IProperties> getProperties();

    void notifyDataChanged(ItemStack itemStack);

    boolean dataChanged(ItemStack itemStack);

    void onReceiveStatesDataFormClient(CompoundTag payLoad, ItemStack itemStack, String nodeId, Player player);

    CompoundTag reCalculateProperties(IReadOnlyTree warehouse);

    BaseProperties getBaseProperties();

    @Nullable
    IArmHandlerModular getLeftArmHolding(ItemStack itemStack);
    @Nullable
    IArmHandlerModular getRightArmHolding(ItemStack itemStack);

    @Nullable
    IArmHandlerModular.AdditionalPropModifier getLeftArmModifier(ItemStack itemStack);
    @Nullable
    IArmHandlerModular.AdditionalPropModifier getRightArmModifier(ItemStack itemStack);

    float getImpulseRatio(ItemStack itemStack);

    float getCurrentWeight(ItemStack itemStack);

    float getAimingSpeed(ItemStack itemStack);

    float getStuckRate(ItemStack itemStack);

    float getMaxStuckRate(ItemStack itemStack);

    float getRecoilControl(ItemStack itemStack);

    float getStability(ItemStack itemStack);

    float getAgility(ItemStack itemStack);

    float getFireSoundRange(ItemStack itemStack);

    boolean isPistol();

    float getSpread(ItemStack itemStack);

    float getShootHeat(ItemStack itemStack);

    float getHeatDecSpeed(ItemStack itemStack);

    float getCurrHeat(ItemStack itemStack, long now);

    float getCurrBaseDamage(ItemStack itemStack);

    void setCurrHeat(ItemStack itemStack, float heat, long heatLastUpdate, long lastShootTime);

    void updateHeat(ItemStack itemStack, float heatInc, long time, boolean setLastShootTime);

    float getHeatStuckRatio(ItemStack itemStack);
}
