package com.exetacticals.client.aiStuff;

import com.exetacticals.GCR;
import com.exetacticals.client.screen.modify.GunModifyScreen;
import com.exetacticals.client.screen.modify.GunModifyUI;
import com.exetacticals.items.GunItem;
import com.exetacticals.items.ModuleItem;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.RegistryObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public final class ModifyScreenShot {
    private static final boolean ENABLED = Boolean.getBoolean("exe_tacticals.shotModify");
    private static final boolean EXIT = Boolean.getBoolean("exe_tacticals.shotModifyExit");
    private static final String GUN = System.getProperty("exe_tacticals.shotModifyGun", "m4a1");
    private static final String[] SLOTS = System.getProperty("exe_tacticals.shotModifySlots", "SCOPE,MUZZLE").split(",");
    private static final int STEP_TICKS = 40;

    private static boolean worldRequested;
    private static int titleFrames;
    private static int ticks;
    private static int stage;
    private static int shot;
    private static boolean shotPending;

    private ModifyScreenShot() {
    }

    public static void register() {
        if (ENABLED) {
            MinecraftForge.EVENT_BUS.addListener(ModifyScreenShot::onTitle);
            MinecraftForge.EVENT_BUS.addListener(ModifyScreenShot::onTick);
            MinecraftForge.EVENT_BUS.addListener(ModifyScreenShot::onScreenRender);
        }
    }

    private static void onTitle(ScreenEvent.Render.Post event) {
        if (worldRequested || !(event.getScreen() instanceof TitleScreen screen) || ++titleFrames < 60) {
            return;
        }
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        Path saves = mc.gameDirectory.toPath().resolve("saves");
        try (Stream<Path> list = Files.list(saves)) {
            Path world = list.filter(Files::isDirectory).findFirst().orElse(null);
            if (world == null) {
                GCR.LOGGER.error("[ModifyScreenShot] no world in {}", saves);
                return;
            }
            GCR.LOGGER.info("[ModifyScreenShot] loading world {}", world.getFileName());
            mc.createWorldOpenFlows().loadLevel(screen, world.getFileName().toString());
        } catch (Exception e) {
            GCR.LOGGER.error("[ModifyScreenShot] failed to load a world", e);
        }
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        if (ticks == 40) {
            giveItems(mc);
        } else if (ticks == 100) {
            GunModifyUI.open();
        } else if (ticks > 100 && (ticks - 100) % STEP_TICKS == 0) {
            if (!(mc.screen instanceof GunModifyScreen screen)) {
                GCR.LOGGER.error("[ModifyScreenShot] modify screen is not open: {}", mc.screen);
                finish(mc);
                return;
            }
            if (stage <= SLOTS.length) {
                if (stage > 0) {
                    boolean ok = screen.debugSelectSlot(SLOTS[stage - 1].trim());
                    GCR.LOGGER.info("[ModifyScreenShot] select {} -> {}", SLOTS[stage - 1], ok);
                }
                shotPending = true;
                stage++;
            } else {
                finish(mc);
            }
        }
    }

    private static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!shotPending || !(event.getScreen() instanceof GunModifyScreen)) {
            return;
        }
        shotPending = false;
        event.getGuiGraphics().flush();
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            File dir = new File(mc.gameDirectory, "screenshots");
            dir.mkdirs();
            File file = new File(dir, "modify_" + shot++ + ".png");
            image.writeToFile(file);
            GCR.LOGGER.info("[ModifyScreenShot] saved {}", file.getAbsolutePath());
        } catch (Exception e) {
            GCR.LOGGER.error("[ModifyScreenShot] screenshot failed", e);
        }
    }

    private static void giveItems(Minecraft mc) {
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (player == null) {
            return;
        }
        mc.getSingleplayerServer().execute(() -> {
            player.getInventory().clearContent();
            for (RegistryObject<Item> entry : GCR.ITEMS.getEntries()) {
                Item item = entry.get();
                if (item instanceof GunItem && entry.getId().getPath().equals(GUN)) {
                    player.getInventory().setItem(player.getInventory().selected, new ItemStack(item));
                }
            }
            int given = 0;
            for (RegistryObject<Item> entry : GCR.ITEMS.getEntries()) {
                Item item = entry.get();
                if (item instanceof ModuleItem<?> && !(item instanceof GunItem) && given < 35) {
                    player.getInventory().add(new ItemStack(item));
                    given++;
                }
            }
            GCR.LOGGER.info("[ModifyScreenShot] gave {} and {} attachments", GUN, given);
        });
    }

    private static void finish(Minecraft mc) {
        stage = Integer.MAX_VALUE / 2;
        ticks = Integer.MIN_VALUE / 2;
        GCR.LOGGER.info("[ModifyScreenShot] done");
        if (EXIT) {
            mc.stop();
        }
    }
}
