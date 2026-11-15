package com.exetacticals.modularSys.fire;

import com.exetacticals.Client;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.network.c2s.GunFirePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IFireMode<T extends IGun> {

    static long rpmToIntervalNanos(int rpm) {
        if (rpm <= 0) {
            return 0L;
        }
        return 60_000_000_000L / rpm;
    }

    @Deprecated
    static int rpmToDelay(int rpm) {
        return 60000 / rpm / 5;
    }

    enum FireControl {
        CANCEL_FIRE,
        EXIT_FIRE_STATE,
        ALLOW_FIRE
    }

    int modifyRpm(int baseRpm);

    @OnlyIn(Dist.CLIENT)
    FireControl clientIntentToFire(Player player, ItemStack stack, T gun);

    @OnlyIn(Dist.CLIENT)
    void triggerClientShoot(Player player, ItemStack stack, T gun);

    void triggerServerShoot(Player player, ItemStack stack, T gun, GunFirePacket packet);

    String getName();

    Class<T> getGunClass();

    static void stopFire() {
        Client.WEAPON_STATUS.fireCount = 0;
        Client.LEFT_BUTTON_PRESSED.set(false);
    }

}
