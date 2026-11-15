package com.exetacticals.network.c2s;

import com.exetacticals.network.IPacket;
import com.exetacticals.network.PacketContext;
import com.exetacticals.workbench.GunWorkbenchMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Клиент нажал «Создать» в меню оружейного верстака; сервер сам проверяет меню, расстояние и ресурсы. */
public class CraftGunPacket implements IPacket<CraftGunPacket> {
    public final ResourceLocation recipeId;

    public CraftGunPacket(ResourceLocation recipeId) {
        this.recipeId = recipeId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipeId);
    }

    public static CraftGunPacket decode(FriendlyByteBuf buf) {
        return new CraftGunPacket(buf.readResourceLocation());
    }

    @Override
    public void onClient(CraftGunPacket packet, PacketContext context) {
    }

    @Override
    public void onServer(CraftGunPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof GunWorkbenchMenu menu) {
                menu.craft(player, packet.recipeId);
            }
        });
    }
}
