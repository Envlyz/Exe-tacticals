package com.exetacticals.modularSys.fire;

import com.exetacticals.Client;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.stuck.ClientGunStuckCache;
import com.exetacticals.modularSys.modules.guns.SlottedGunMainPart;
import com.exetacticals.modularSys.task.GunTaskHandler;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.network.c2s.GunFirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.Map;

public abstract class AssaultRifeFireMode<T extends SlottedGunMainPart> extends FireMode<T> {
    public AssaultRifeFireMode(String name) {
        super(name);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public FireControl onClientIntentToFire(Player player, ItemStack stack, T gun) {
        ClientGunStuckCache.get().projectToNbt(stack, gun);
        boolean stuck = ClientGunStuckCache.get().isStuck(stack, gun);
        if (stuck) {
            return FireControl.EXIT_FIRE_STATE;
        }
        int ammoLeft = gun.getGunAmmoLeft(stack);
        return ammoLeft > 0 ? FireControl.ALLOW_FIRE : FireControl.EXIT_FIRE_STATE;
    }

    @Override
    public void triggerServerShoot(Player player, ItemStack stack, T gun, GunFirePacket packet) {
        boolean stuck = packet.stuck;
        if (useAmmo(player, stack, gun, stuck)) {
            gun.serverShoot(player, stack, packet.shootId, packet);
        } else if (player instanceof ServerPlayer serverPlayer) {
            gun.serverShootRejected(serverPlayer, stack, packet.shootId);
        }
    }

    protected float stuckRate(Player player, ItemStack stack, T gun) {
        float stuckRate = gun.getStuckRate(stack);
        float maxStuckRate = gun.getMaxStuckRate(stack);
        float currHeat = gun.getCurrHeat(stack, player.level().getGameTime());
        float heatStuckRatio = gun.getHeatStuckRatio(stack);
        currHeat *= heatStuckRatio;
        currHeat = Mth.clamp(currHeat, 0, 1);
        currHeat = (float) Math.pow(currHeat, 3);
        return Mth.clamp(Mth.lerp(currHeat, stuckRate, maxStuckRate), 0f, 1f);
    }

    @OnlyIn(Dist.CLIENT)
    protected enum ClientShot {
        NONE,
        FIRED,
        JAMMED
    }

    @OnlyIn(Dist.CLIENT)
    protected ClientShot clientShoot(Player player, ItemStack stack, T gun) {
        boolean rollStuck = rollStuckClient(player, stack, gun);
        if (!useAmmo(player, stack, gun, rollStuck)) {
            return ClientShot.NONE;
        }
        boolean stuck = gun.isStuck(stack);
        boolean lastRound = !stuck && gun.getGunAmmoLeft(stack) == 0 && gun.getMagAttachment(stack) != null;
        String gunId = gun.getIdentityID(stack);
        int shootId = nextShootId();
        if (stuck) {
            ClientGunStuckCache.get().onLocalJam(gunId, shootId);
            Minecraft.getInstance().gui.setOverlayMessage(
                    Component.translatable("exe_tacticals.overlay.stuck")
                            .setStyle(Style.EMPTY.withColor(Color.RED.getRGB())), false);
        }
        sendPacket(shootId, stuck, gunId);
        gun.clientShoot(player, stack);
        Client.WEAPON_STATUS.onShoot();
        Client.getGunRenderer().dispatchAnimationEvent(EventType.SHOOT, Map.of(
                EventType.PARAM_STUCK, Boolean.toString(stuck),
                EventType.PARAM_LAST_ROUND, Boolean.toString(lastRound)
        ));
        return stuck ? ClientShot.JAMMED : ClientShot.FIRED;
    }

    @OnlyIn(Dist.CLIENT)
    protected boolean rollStuckClient(Player player, ItemStack stack, T gun) {
        float rate = stuckRate(player, stack, gun);
        if (rate <= 0f) {
            return false;
        }
        if (rate >= 1f) {
            return true;
        }
        return Math.random() < rate;
    }

    protected boolean useAmmo(Player player, ItemStack stack, T gun, boolean handleStuck) {
        boolean stuck = gun.isStuck(stack);
        if (stuck) {
            return false;
        }
        int ammoLeft = gun.getGunAmmoLeft(stack);
        if (ammoLeft > 0) {
            gun.setGunAmmoLeft(stack, 0);
            useMagAmmo(stack, gun, player, handleStuck);
            gun.notifyDataChanged(stack);
            return true;
        }
        return false;
    }

    protected abstract void useMagAmmo(ItemStack itemStack, T gun, Player player, boolean handleStuck);

    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootSemi(Player player, ItemStack stack, T gun) {
        if (clientShoot(player, stack, gun) != ClientShot.NONE) {
            IFireMode.stopFire();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootFullAuto(Player player, ItemStack stack, T gun) {
        ClientShot shot = clientShoot(player, stack, gun);
        if (shot == ClientShot.JAMMED) {
            IFireMode.stopFire();
        } else if (shot == ClientShot.FIRED) {
            Client.WEAPON_STATUS.fireCount++;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void triggerClientShootBurst(Player player, ItemStack stack, T gun, int burstCount) {
        ClientShot shot = clientShoot(player, stack, gun);
        if (shot == ClientShot.JAMMED) {
            IFireMode.stopFire();
        } else if (shot == ClientShot.FIRED) {
            Client.WEAPON_STATUS.fireCount++;
            if (Client.WEAPON_STATUS.fireCount >= burstCount) {
                IFireMode.stopFire();
            }
        }
    }
}
