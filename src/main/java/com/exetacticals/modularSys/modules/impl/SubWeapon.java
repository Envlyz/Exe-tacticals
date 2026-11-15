package com.exetacticals.modularSys.modules.impl;

import com.exetacticals.modularSys.Direction;
import com.exetacticals.modularSys.modules.IInteractiveModular;
import com.exetacticals.modularSys.modules.UniqueModule;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.network.c2s.SubWeaponFirePacket;
import com.exetacticals.network.c2s.SubWeaponReloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class SubWeapon extends UniqueModule implements IInteractiveModular {
    public SubWeapon(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
    }

    public void serverShoot(SubWeaponFirePacket packet, ItemStack itemStack, ServerPlayer player, IGun gun, SubWeapon subWeapon) {

    }

    public void serverReload(SubWeaponReloadPacket packet, ItemStack itemStack, ServerPlayer player, IGun gun, SubWeapon subWeapon) {

    }
}
