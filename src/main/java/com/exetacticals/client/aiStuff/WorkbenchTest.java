package com.exetacticals.client.aiStuff;

import com.exetacticals.GCR;
import com.exetacticals.client.screen.GunWorkbenchScreen;
import com.exetacticals.workbench.GunWorkbenchBlock;
import com.exetacticals.workbench.GunWorkbenchMenu;
import com.exetacticals.workbench.ModWorkbench;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkHooks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public final class WorkbenchTest {
    private static final boolean ENABLED = Boolean.getBoolean("exe_tacticals.workbenchTest");
    private static final boolean CRAFT = Boolean.getBoolean("exe_tacticals.workbenchCraftTest");
    private static volatile long lastRender = System.nanoTime();
    private static boolean worldRequested;
    private static int titleFrames;
    private static int ticks;
    private static String pendingShot;
    private static int frames;
    private static BlockPos mainPos = BlockPos.ZERO;

    private WorkbenchTest() {
    }

    public static void register() {
        if (ENABLED) {
            MinecraftForge.EVENT_BUS.addListener(WorkbenchTest::onTitle);
            MinecraftForge.EVENT_BUS.addListener(WorkbenchTest::onTick);
            MinecraftForge.EVENT_BUS.addListener(WorkbenchTest::onHud);
            MinecraftForge.EVENT_BUS.addListener(WorkbenchTest::onScreen);
            if (CRAFT) {
                MinecraftForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent e) -> lastRender = System.nanoTime());
                Thread watchdog = new Thread(WorkbenchTest::watchdog, "exe-watchdog");
                watchdog.setDaemon(true);
                watchdog.start();
            }
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
            GCR.LOGGER.error("[WorkbenchTest] no world", e);
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
        switch (ticks) {
            case 30 -> mc.getSingleplayerServer().execute(() -> {
                ServerLevel level = sp.serverLevel();
                level.setDayTime(6000);
                int px = sp.getBlockX();
                int pz = sp.getBlockZ();
                int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                BlockPos main = new BlockPos(px - 1, ground, pz + 3);
                mainPos = main;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -2; dz <= 6; dz++) {
                        for (int dy = 0; dy <= 4; dy++) {
                            level.setBlock(new BlockPos(px + dx, ground + dy, pz + dz), Blocks.AIR.defaultBlockState(), 3);
                        }
                        level.setBlock(new BlockPos(px + dx, ground - 1, pz + dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                }
                sp.teleportTo(level, px + 0.5, ground, pz - 0.5, 0f, 38f);
                BlockState state = ModWorkbench.BLOCK.get().defaultBlockState()
                        .setValue(GunWorkbenchBlock.FACING, Direction.NORTH);
                level.setBlock(main, state, 3);
                state.getBlock().setPlacedBy(level, main, state, sp, ItemStack.EMPTY);
                sp.getInventory().clearContent();
                sp.getInventory().add(new ItemStack(Items.IRON_INGOT, 64));
                sp.getInventory().add(new ItemStack(Items.REDSTONE, 3));
                sp.getInventory().add(new ItemStack(Items.BLACK_DYE, 8));
                sp.getInventory().add(new ItemStack(Items.COPPER_INGOT, 5));
                sp.setGameMode(CRAFT ? net.minecraft.world.level.GameType.CREATIVE : net.minecraft.world.level.GameType.SURVIVAL);
            });
            case 90 -> pendingShot = "workbench_world";
            case 110 -> mc.getSingleplayerServer().execute(() -> {
                BlockPos main = mainPos;
                NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                        (id, inv, p) -> new GunWorkbenchMenu(id, inv, main),
                        Component.translatable("block.exe_tacticals.gun_workbench")), buf -> buf.writeBlockPos(main));
            });
            case 150 -> pendingShot = "workbench_menu";
            case 165 -> {
                if (CRAFT) {
                    mc.getSingleplayerServer().execute(() -> {
                        if (sp.containerMenu instanceof GunWorkbenchMenu menu) {
                            menu.craft(sp, GCR.RL(GCR.MODID, "workbench/m4a1"));
                            GCR.LOGGER.info("[WorkbenchTest] crafted, inventory: {}", sp.getInventory().items);
                        }
                        sp.closeContainer();
                        var table = GunWorkbenchBlock.blockEntity(sp.level(), mainPos);
                        GCR.LOGGER.info("[WorkbenchTest] on table: {}", table == null ? "no block entity" : table.crafted());
                    });
                }
            }
            case 200 -> {
                if (CRAFT) {
                    pendingShot = "workbench_table";
                }
            }
            case 230 -> {
                if (CRAFT) {
                    com.exetacticals.network.GCRNetwork.sendToServer(new com.exetacticals.network.c2s.TakeCraftedGunPacket(mainPos));
                }
            }
            case 250 -> {
                if (CRAFT) {
                    mc.getSingleplayerServer().execute(() -> {
                        var table = GunWorkbenchBlock.blockEntity(sp.level(), mainPos);
                        GCR.LOGGER.info("[WorkbenchTest] after take: table={}, inventory={}",
                                table == null ? "?" : table.crafted(), sp.getInventory().items);
                        for (int i = 0; i < 9; i++) {
                            if (sp.getInventory().getItem(i).getItem() instanceof com.exetacticals.items.GunItem) {
                                sp.getInventory().selected = i;
                                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(i));
                                GCR.LOGGER.info("[WorkbenchTest] holding crafted gun in slot {}", i);
                                break;
                            }
                        }
                    });
                }
            }
            case 300 -> {
                if (CRAFT) {
                    GCR.LOGGER.info("[WorkbenchTest] alive after holding crafted gun");
                }
            }
            case 320 -> {
                GCR.LOGGER.info("[WorkbenchTest] done, screen={}", mc.screen);
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void watchdog() {
        boolean dumped = false;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            if (!dumped && System.nanoTime() - lastRender > 6_000_000_000L && ticks > 100) {
                dumped = true;
                StringBuilder sb = new StringBuilder("[WorkbenchTest] FREEZE detected, thread dump:\n");
                for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                    String name = e.getKey().getName();
                    if (!name.contains("Render") && !name.contains("Server thread") && !name.contains("GCR") && !name.toLowerCase().contains("weapon")
                            && !name.toLowerCase().contains("loop")) {
                        continue;
                    }
                    sb.append("== ").append(name).append(" (").append(e.getKey().getState()).append(")\n");
                    for (StackTraceElement el : e.getValue()) {
                        sb.append("    at ").append(el).append('\n');
                    }
                }
                GCR.LOGGER.error(sb.toString());
            }
        }
    }

    private static void onHud(RenderGuiEvent.Post event) {
        if (("workbench_world".equals(pendingShot) || "workbench_table".equals(pendingShot)) && ++frames > 2) {
            shot();
        }
    }

    private static void onScreen(ScreenEvent.Render.Post event) {
        if ("workbench_menu".equals(pendingShot) && event.getScreen() instanceof GunWorkbenchScreen) {
            event.getGuiGraphics().flush();
            shot();
        }
    }

    private static void shot() {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            File file = new File(new File(mc.gameDirectory, "screenshots"), pendingShot + ".png");
            image.writeToFile(file);
            GCR.LOGGER.info("[WorkbenchTest] saved {}", file);
        } catch (Exception e) {
            GCR.LOGGER.error("[WorkbenchTest] screenshot failed", e);
        }
        pendingShot = null;
        frames = 0;
    }
}
