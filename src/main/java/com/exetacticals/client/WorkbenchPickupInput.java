package com.exetacticals.client;

import com.exetacticals.network.GCRNetwork;
import com.exetacticals.network.c2s.TakeCraftedGunPacket;
import com.exetacticals.workbench.GunWorkbenchBlock;
import com.exetacticals.workbench.GunWorkbenchBlockEntity;
import com.exetacticals.client.screen.GuiPanelRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class WorkbenchPickupInput {
    private static GunWorkbenchBlockEntity hintTable;
    private static ItemStack hintStack = ItemStack.EMPTY;
    private static final float BG_ALPHA = 0.45f;
    private static final GuiPanelRender.Fade hintFade = new GuiPanelRender.Fade(14f);

    private WorkbenchPickupInput() {
    }

    public static boolean isTargeted(GunWorkbenchBlockEntity table) {
        return table != null && table == hintTable;
    }

    @SubscribeEvent
    public static void onHighlight(net.minecraftforge.client.event.RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            GunWorkbenchBlockEntity table = GunWorkbenchBlock.blockEntity(mc.level, event.getTarget().getBlockPos());
            if (table != null && table.isOccupied()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        hintTable = null;
        if (mc.player == null || mc.level == null || mc.screen != null || mc.player.isSpectator()
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        GunWorkbenchBlockEntity table = GunWorkbenchBlock.blockEntity(mc.level, hit.getBlockPos());
        if (table == null || !table.isOccupied()) {
            return;
        }
        hintTable = table;
        boolean pressed = false;
        while (mc.options.keySwapOffhand.consumeClick()) {
            pressed = true;
        }
        if (pressed) {
            GCRNetwork.sendToServer(new TakeCraftedGunPacket(table.getBlockPos()));
        }
    }

    @SubscribeEvent
    public static void onHud(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GunWorkbenchBlockEntity table = hintTable;
        boolean show = table != null && !table.isRemoved() && table.isOccupied() && mc.screen == null && !mc.options.hideGui;
        if (show) {
            hintStack = table.crafted();
        }
        float a = hintFade.update(show);
        if (a <= 0.01f || hintStack.isEmpty()) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        String key = mc.options.keySwapOffhand.getTranslatedKeyMessage().getString().toUpperCase();
        String label = Component.translatable("gui.exe_tacticals.workbench.take_hint", hintStack.getHoverName()).getString();
        float keySize = 6.5f, textSize = 6.5f;
        float keyW = Math.max(9.5f, width(key, keySize, true) + 5f);
        float textW = width(label, textSize, false);
        float h = 14f, pad = 2.25f, gap = 4f;
        float w = pad + keyW + gap + textW + 6f;
        float x = (g.guiWidth() - w) / 2f;
        float y = g.guiHeight() / 2f + 10f + (1f - a) * 3f;
        GuiPanelRender.shadow(g, x, y + 1, w, h, 4.5f, 3f, GuiPanelRender.alpha(GuiPanelRender.SHADOW, a * 0.4f));
        GuiPanelRender.fill(g, x, y, w, h, 4.5f, GuiPanelRender.alpha(GuiPanelRender.PANEL_TINT, a * BG_ALPHA));
        GuiPanelRender.stroke(g, x, y, w, h, 4.5f, 0.5f, GuiPanelRender.alpha(GuiPanelRender.PANEL_BORDER, a * BG_ALPHA));
        float kx = x + pad, ky = y + pad, kh = h - pad * 2;
        GuiPanelRender.fill(g, kx, ky, keyW, kh, 2.5f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, a * 0.8f));
        GuiPanelRender.stroke(g, kx, ky, keyW, kh, 2.5f, 0.5f, GuiPanelRender.alpha(0x40FFFFFF, a * 0.8f));
        text(g, key, kx + (keyW - width(key, keySize, true)) / 2f, ky + kh / 2f + 2.3f, keySize,
                GuiPanelRender.alpha(0xFFFFFFFF, a), true);
        text(g, label, kx + keyW + gap, y + h / 2f + 2.3f, textSize, GuiPanelRender.alpha(GuiPanelRender.TEXT, a * 0.9f), false);
    }

    private static float width(String s, float size, boolean medium) {
        com.exetacticals.client.tooltip.MsdfFont f = medium
                ? com.exetacticals.client.tooltip.MsdfFont.medium() : com.exetacticals.client.tooltip.MsdfFont.regular();
        return f == null ? Minecraft.getInstance().font.width(s) : f.width(s, size);
    }

    private static void text(GuiGraphics g, String s, float x, float baseline, float size, int color, boolean medium) {
        com.exetacticals.client.tooltip.MsdfFont f = medium
                ? com.exetacticals.client.tooltip.MsdfFont.medium() : com.exetacticals.client.tooltip.MsdfFont.regular();
        if (f == null) {
            g.drawString(Minecraft.getInstance().font, s, (int) x, (int) (baseline - 7), color, false);
        } else {
            f.draw(g, net.minecraft.util.FormattedCharSequence.forward(s, net.minecraft.network.chat.Style.EMPTY),
                    x, baseline, size, color, false);
        }
    }
}
