package com.exetacticals.client.aiStuff;

import com.exetacticals.GCR;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;

/**
 * Дев-инструмент: снимок тултипов нескольких предметов на главном экране.
 * {@code -Dexe_tacticals.dumpTooltips=m4a1,ar_ext_handguard,...} (id предметов мода) →
 * {@code run/screenshots/tooltips.png}; с {@code -Dexe_tacticals.dumpTooltipsExit=true} игра закрывается.
 */
@OnlyIn(Dist.CLIENT)
public final class TooltipDump {
    private static final String ITEMS = System.getProperty("exe_tacticals.dumpTooltips", "");
    private static final boolean EXIT = Boolean.getBoolean("exe_tacticals.dumpTooltipsExit");
    private static int frames;
    private static boolean done;

    private TooltipDump() {
    }

    public static void register() {
        if (!ITEMS.isEmpty()) {
            MinecraftForge.EVENT_BUS.addListener(TooltipDump::onRender);
        }
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        if (done || !(event.getScreen() instanceof TitleScreen screen) || ++frames < 60) {
            return;
        }
        done = true;
        GuiGraphics g = event.getGuiGraphics();
        String[] ids = ITEMS.split(",");
        int column = screen.width / ids.length;
        for (int i = 0; i < ids.length; i++) {
            Item item = ForgeRegistries.ITEMS.getValue(GCR.RL(GCR.MODID, ids[i].trim()));
            if (item == null) {
                GCR.LOGGER.warn("[TooltipDump] no item {}", ids[i]);
                continue;
            }
            g.renderTooltip(Minecraft.getInstance().font, new ItemStack(item), 4 + i * column - 12, 20);
        }
        g.flush();
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            File file = new File(new File(mc.gameDirectory, "screenshots"), "tooltips.png");
            file.getParentFile().mkdirs();
            image.writeToFile(file);
            GCR.LOGGER.info("[TooltipDump] saved {}", file.getAbsolutePath());
        } catch (Exception e) {
            GCR.LOGGER.error("[TooltipDump] failed", e);
        }
        if (EXIT) {
            mc.stop();
        }
    }

}
