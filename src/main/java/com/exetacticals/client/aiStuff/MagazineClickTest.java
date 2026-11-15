package com.exetacticals.client.aiStuff;

import com.exetacticals.GCR;
import com.exetacticals.items.MagazineItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public final class MagazineClickTest {
    private static final boolean ENABLED = Boolean.getBoolean("exe_tacticals.magClickTest");
    private static boolean worldRequested;
    private static int titleFrames;
    private static int ticks;

    private MagazineClickTest() {
    }

    public static void register() {
        if (ENABLED) {
            MinecraftForge.EVENT_BUS.addListener(MagazineClickTest::onTitle);
            MinecraftForge.EVENT_BUS.addListener(MagazineClickTest::onTick);
        }
    }

    private static void onTitle(ScreenEvent.Render.Post event) {
        if (worldRequested || !(event.getScreen() instanceof TitleScreen screen) || ++titleFrames < 60) {
            return;
        }
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        try (Stream<Path> list = Files.list(mc.gameDirectory.toPath().resolve("saves"))) {
            list.filter(Files::isDirectory).findFirst()
                    .ifPresent(w -> mc.createWorldOpenFlows().loadLevel(screen, w.getFileName().toString()));
        } catch (Exception e) {
            GCR.LOGGER.error("[MagClickTest] no world", e);
        }
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) {
            return;
        }
        int menu = mc.player.inventoryMenu.containerId;
        switch (ticks) {
            case 40 -> mc.getSingleplayerServer().execute(() -> {
                sp.getInventory().clearContent();
                sp.getInventory().setItem(9, new ItemStack(ForgeRegistries.ITEMS.getValue(GCR.RL(GCR.MODID, "akm_steel_mag"))));
                sp.getInventory().setItem(10, new ItemStack(GCR.AMMO_762X39_ITEM.get(), 64));
                sp.inventoryMenu.broadcastChanges();
            });
            case 60 -> mc.gameMode.handleInventoryMouseClick(menu, 9, 0, ClickType.PICKUP, mc.player);
            case 70 -> mc.gameMode.handleInventoryMouseClick(menu, 10, 1, ClickType.PICKUP, mc.player);
            case 80 -> log("after load", sp);
            case 90 -> mc.gameMode.handleInventoryMouseClick(menu, 11, 1, ClickType.PICKUP, mc.player);
            case 100 -> log("after unload", sp);
            case 110 -> {
                GCR.LOGGER.info("[MagClickTest] done");
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void log(String step, ServerPlayer sp) {
        ItemStack carried = sp.inventoryMenu.getCarried();
        int rounds = carried.getItem() instanceof MagazineItem m ? m.getRounds(carried) : -1;
        GCR.LOGGER.info("[MagClickTest] {}: carried={} rounds={} slot10={} slot11={}", step,
                carried, rounds, sp.getInventory().getItem(10), sp.getInventory().getItem(11));
    }
}
