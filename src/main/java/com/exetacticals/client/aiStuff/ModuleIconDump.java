package com.exetacticals.client.aiStuff;

import com.exetacticals.GCR;
import com.exetacticals.items.GunItem;
import com.exetacticals.items.MagazineItem;
import com.exetacticals.items.ModuleItem;
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
import net.minecraftforge.registries.RegistryObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Дев-инструмент: снимки 3D-иконок всех обвесов/магазинов (для проверки {@code ModuleItemRenderer}).
 *
 * <p>Включается JVM-флагом {@code -Dexe_tacticals.dumpIcons=true}: на главном экране рисует сетку предметов
 * (увеличенно) и сохраняет страницы в {@code run/screenshots/module_icons_N.png}.
 * С {@code -Dexe_tacticals.dumpIconsExit=true} после этого закрывает игру.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ModuleIconDump {
    private static final boolean ENABLED = Boolean.getBoolean("exe_tacticals.dumpIcons");
    private static final boolean EXIT = Boolean.getBoolean("exe_tacticals.dumpIconsExit");
    private static final float ITEM_SCALE = 2f;
    private static final int WARMUP_FRAMES = 60;

    private static int frames;
    private static int page;
    private static boolean done;

    private ModuleIconDump() {
    }

    public static void register() {
        if (ENABLED) {
            MinecraftForge.EVENT_BUS.addListener(ModuleIconDump::onRender);
        }
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        if (done || !(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        if (++frames < WARMUP_FRAMES) {
            return;
        }
        List<ItemStack> stacks = collect();
        int cell = (int) (16 * ITEM_SCALE) + 6;
        int cols = Math.max(1, screen.width / cell);
        int rows = Math.max(1, screen.height / cell);
        int perPage = cols * rows;
        int from = page * perPage;
        if (from >= stacks.size()) {
            done = true;
            GCR.LOGGER.info("[ModuleIconDump] done, {} items", stacks.size());
            if (EXIT) {
                Minecraft.getInstance().stop();
            }
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        g.fill(0, 0, screen.width, screen.height, 0xFF8B8B8B);
        for (int i = from; i < Math.min(stacks.size(), from + perPage); i++) {
            int local = i - from;
            int x = (local % cols) * cell + 3;
            int y = (local / cols) * cell + 3;
            g.fill(x, y, x + cell - 6, y + cell - 6, 0xFF6F6F6F);
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(ITEM_SCALE, ITEM_SCALE, 1);
            g.renderItem(stacks.get(i), 0, 0);
            g.pose().popPose();
        }
        g.flush();
        try (NativeImage image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
            dir.mkdirs();
            File file = new File(dir, "module_icons_" + page + ".png");
            image.writeToFile(file);
            GCR.LOGGER.info("[ModuleIconDump] saved {} (items {}..{})", file.getAbsolutePath(), from, Math.min(stacks.size(), from + perPage) - 1);
        } catch (Exception e) {
            GCR.LOGGER.error("[ModuleIconDump] failed to save page {}", page, e);
        }
        page++;
    }

    private static List<ItemStack> collect() {
        List<ItemStack> stacks = new ArrayList<>();
        for (RegistryObject<Item> entry : GCR.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof ModuleItem<?> && !(item instanceof GunItem)) {
                ItemStack stack = new ItemStack(item);
                if (item instanceof MagazineItem mag) {
                    mag.setRounds(stack, mag.capacity());
                }
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
