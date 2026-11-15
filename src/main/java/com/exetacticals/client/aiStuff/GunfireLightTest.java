package com.exetacticals.client.aiStuff;

import com.exetacticals.GCR;
import com.exetacticals.client.render.fx.GunfireLights;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public final class GunfireLightTest {
    private static final boolean ENABLED = Boolean.getBoolean("exe_tacticals.flashTest");
    private static boolean worldRequested;
    private static int titleFrames;
    private static int ticks;
    private static String pendingShot;
    private static int framesAfterShot;

    private GunfireLightTest() {
    }

    public static void register() {
        if (ENABLED) {
            MinecraftForge.EVENT_BUS.addListener(GunfireLightTest::onTitle);
            MinecraftForge.EVENT_BUS.addListener(GunfireLightTest::onTick);
            MinecraftForge.EVENT_BUS.addListener(GunfireLightTest::onHud);
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
            GCR.LOGGER.error("[FlashTest] no world", e);
        }
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        if (ticks == 20) {
            mc.getSingleplayerServer().execute(() ->
                    mc.getSingleplayerServer().overworld().setDayTime(18000));
            mc.player.setXRot(25f);
            GunfireLights.debugHoldMs = 1500f;
        } else if (ticks == 60) {
            GunfireLights.onShot(mc.player, null, ItemStack.EMPTY);
        } else if (ticks == 100) {
            pendingShot = "flash_off";
            framesAfterShot = 0;
        } else if (ticks == 120) {
            pendingShot = "flash_on";
            framesAfterShot = 0;
        } else if (ticks == 160) {
            GCR.LOGGER.info("[FlashTest] done");
            mc.stop();
        }
    }

    private static void onHud(RenderGuiEvent.Post event) {
        if (pendingShot == null) {
            return;
        }
        if (pendingShot.equals("flash_on") && framesAfterShot == 0) {
            GunfireLights.onShot(Minecraft.getInstance().player, null, ItemStack.EMPTY);
        }
        if (++framesAfterShot < 4) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            File file = new File(new File(mc.gameDirectory, "screenshots"), pendingShot + ".png");
            image.writeToFile(file);
            GCR.LOGGER.info("[FlashTest] saved {}", file);
        } catch (Exception e) {
            GCR.LOGGER.error("[FlashTest] screenshot failed", e);
        }
        pendingShot = null;
    }
}
