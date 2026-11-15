package com.exetacticals.common;

import com.exetacticals.GCR;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ModuleRegister;
import com.exetacticals.modularSys.builder.*;
import com.exetacticals.modularSys.modules.StatesUpdateContext;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.network.s2c.InitClientGunDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.TickEvent;
import com.exetacticals.network.GCRNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonEvents {

    private static final String MESSAGE_RESET_KEY = "exe_tacticals.message.gun_data_reset";

    private static final String MESSAGE_PRUNED_KEY = "exe_tacticals.message.gun_data_pruned";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player entity = event.player;
        if (!entity.level().isClientSide) {
            handleDataChanged(entity.getMainHandItem());
        }
    }

    private static void handleDataChanged(ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem && gunItem.getGun().dataChanged(itemStack)) {
            CompoundTag original = itemStack.getTag();
            if (original != null && !original.isEmpty()) {
                CompoundTag newData = original.copy();
                newData.putBoolean(IGun.DATA_CHANGED_KEY, false);
                itemStack.setTag(newData);
            }
        }
    }

    private static void handleGunDataUpdate(LivingEquipmentChangeEvent event) {
        ItemStack eventTo = event.getTo();

        if (eventTo.getItem() instanceof GunItem gunItem) {
            IGun gun = gunItem.getGun();
            long dataDate = gun.getDataDate(eventTo);
            String identityID = gun.getIdentityID(eventTo);
            long serverStartTime = Commons.getServerStartTime();
            if (IGun.NONE.equals(identityID)) {
                gun.serverInitData(eventTo);
                gun.setDataDate(eventTo, serverStartTime);
                String initID = gun.getIdentityID(eventTo);
                if (!IGun.NONE.equals(initID) && event.getEntity() instanceof ServerPlayer player) {
                    GCRNetwork.sendToPlayer(
                            player,
                            new InitClientGunDataPacket(
                                    gun.getID(),
                                    Item.getId(gunItem),
                                    gun.checkAndGetRaw(eventTo)
                            )
                    );
                }
            } else if (dataDate != serverStartTime) {
                ListTag modulesTag = gun.getModulesTag(eventTo);
                List<String> oldIdList = collectModuleIds(gun, eventTo);
                IBuilder builder = new Builder();
                builder.initAndClear(modulesTag);
                try {
                    List<ValidateResult> commit = builder.commit();
                    boolean hasError = false;
                    for (ValidateResult result : commit) {
                        if (!result.isCommitAllowed()) {
                            hasError = true;
                            break;
                        }
                    }

                    IReadOnlyTree warehouse = builder.getWarehouse();
                    ListTag finalModules = warehouse.write().copy();

                    gun.setModulesTag(eventTo, finalModules);

                    CompoundTag lastStates = gun.getStatesTag(eventTo);
                    ShadowNode shadowTree = warehouse.getShadowTree();
                    StatesUpdateContext statesUpdateContext = new StatesUpdateContext(gun, shadowTree, lastStates);
                    statesUpdateContext.autoExec();
                    gun.setStatesTag(eventTo, lastStates);

                    CompoundTag properties = gun.reCalculateProperties(warehouse);
                    gun.setPropertiesTag(eventTo, properties);

                    gun.setDataDate(eventTo, serverStartTime);
                    gun.setModifyID(eventTo, gun.getModifyID(eventTo) + 1, false);
                    gun.notifyDataChanged(eventTo);
                    if (hasError) {
                        handlePrunedModulesRefund(gun, eventTo, oldIdList, finalModules, event.getEntity());
                    }
                } catch (Exception e) {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        handleGunDataRecovery(gunItem, player, oldIdList, e);
                    } else {
                        GCR.LOGGER.error("Failed to migrate gun {} data for non-player entity {}, "
                                + "no recovery is possible on this path", gun.getID(), event.getEntity(), e);
                    }
                }
            }
        }
    }

    private static void handleGunDataRecovery(GunItem gunItem, ServerPlayer player, List<String> brokenModuleIds, Throwable cause) {
        IGun gun = gunItem.getGun();
        long serverStartTime = Commons.getServerStartTime();

        ItemStack freshGun = new ItemStack(gunItem);
        gun.serverInitData(freshGun);
        CompoundTag freshTag = gun.checkAndGetRaw(freshGun);
        gun.setDataDate(freshGun, serverStartTime);
        List<ItemStack> refunds = collectModuleRefunds(brokenModuleIds, collectModuleIds(gun, freshGun));
        int refunded = giveModuleItems(player, refunds);

        player.setItemInHand(InteractionHand.MAIN_HAND, freshGun);

        GCR.LOGGER.error("Gun {} data migration failed for {}, the item was reset to a fresh one "
                        + "and {} extra module(s) were refunded (fresh data: {} bytes)",
                gun.getID(), player.getGameProfile().getName(), refunded, freshTag.sizeInBytes(), cause);

        player.sendSystemMessage(Component.literal(
                Component.translatable(MESSAGE_RESET_KEY).getString().replace("$count", Integer.toString(refunded))));
    }

    private static void handlePrunedModulesRefund(IGun gun, ItemStack itemStack, List<String> oldIdList,
                                                  ListTag finalModules, LivingEntity entity) {
        List<String> newIdList = collectModuleIds(finalModules);
        List<ItemStack> refunds = collectModuleRefunds(oldIdList, newIdList);
        int refunded = 0;
        if (entity instanceof Player player) {
            refunded = giveModuleItems(player, refunds);
            if (refunded > 0) {
                player.sendSystemMessage(Component.literal(Component.translatable(MESSAGE_PRUNED_KEY)
                        .getString().replace("$count", Integer.toString(refunded))));
            }
        }
        int pruned = oldIdList.size() - newIdList.size();
        GCR.LOGGER.warn("Gun {} data version migration pruned {} module(s) ({} -> {}), refunded {}",
                gun.getID(), pruned, oldIdList.size(), newIdList.size(), refunded);
    }

    private static int giveModuleItems(Player player, List<ItemStack> items) {
        int given = 0;
        for (ItemStack item : items) {
            if (!player.getInventory().add(item)) {
                ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), item);
                player.level().addFreshEntity(entity);
            }
            given++;
        }
        return given;
    }

    private static List<String> collectModuleIds(IGun gun, ItemStack itemStack) {
        return collectModuleIds(gun.getModulesTag(itemStack));
    }

    private static List<String> collectModuleIds(ListTag modules) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < modules.size(); i++) {
            CompoundTag moduleTag = modules.getCompound(i);
            String moduleId = moduleTag.getString(Unit.MODULE_ID);
            if (!moduleId.isEmpty()) {
                ids.add(moduleId);
            }
        }
        return ids;
    }

    private static List<ItemStack> collectModuleRefunds(List<String> brokenModuleIds, List<String> freshModuleIds) {
        Map<String, Integer> remaining = new HashMap<>();
        freshModuleIds.forEach(id -> remaining.merge(id, 1, Integer::sum));

        List<ItemStack> refunds = new ArrayList<>();
        for (String moduleId : brokenModuleIds) {
            Integer left = remaining.get(moduleId);
            if (left != null && left > 0) {
                remaining.put(moduleId, left - 1);
                continue;
            }
            IModular module = ModuleRegister.get(moduleId);
            if (module == null) {
                GCR.LOGGER.warn("Dropping unknown module '{}' while refunding broken gun data", moduleId);
                continue;
            }
            if (module.getBindItem() == null) {
                GCR.LOGGER.warn("Dropping module '{}' while refunding broken gun data: it has no bound item", moduleId);
                continue;
            }
            refunds.add(new ItemStack(module.getBindItem()));
        }
        return refunds;
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        handleGunDataUpdate(event);

        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || !(living instanceof Player)) {
            return;
        }
        handleDataChanged(event.getFrom());
        handleDataChanged(event.getTo());
    }

}
