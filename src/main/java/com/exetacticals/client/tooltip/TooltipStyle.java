package com.exetacticals.client.tooltip;

import com.exetacticals.GCR;
import com.exetacticals.client.screen.GuiPanelRender;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class TooltipStyle {
    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 7;
    private static final int TITLE_GAP = 6;
    private static final float RADIUS = 6f;
    private static final int TINT = 0xD8111216;
    private static final int BORDER = 0x30FFFFFF;
    private static final int TITLE = 0xFFFFFFFF;
    private static final int TEXT = 0xFFC9CAD2;

    private TooltipStyle() {
    }

    private static boolean isOurs(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && GCR.MODID.equals(id.getNamespace());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onGather(RenderTooltipEvent.GatherComponents event) {
        if (!isOurs(event.getItemStack()) || MsdfFont.regular() == null || MsdfFont.medium() == null) {
            return;
        }
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        for (int i = 0; i < elements.size(); i++) {
            var left = elements.get(i).left();
            if (left.isPresent()) {
                elements.set(i, Either.right(new TextLine(left.get(), i == 0)));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPre(RenderTooltipEvent.Pre event) {
        if (event.isCanceled() || !isOurs(event.getItemStack())) {
            return;
        }
        List<ClientTooltipComponent> components = event.getComponents();
        if (components.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        render(event.getGraphics(), event.getFont(), components, event.getX(), event.getY(),
                event.getScreenWidth(), event.getScreenHeight());
    }

    private static void render(GuiGraphics g, Font font, List<ClientTooltipComponent> components,
                               int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int contentW = 0;
        int contentH = 0;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent c = components.get(i);
            contentW = Math.max(contentW, c.getWidth(font));
            contentH += c.getHeight() + (i == 0 && components.size() > 1 ? TITLE_GAP : 0);
        }
        contentH -= 1;
        int boxW = contentW + PADDING_X * 2;
        int boxH = contentH + PADDING_Y * 2;

        int boxX = mouseX + 12;
        if (boxX + boxW > screenWidth - 4) {
            boxX = Math.max(4, mouseX - 12 - boxW);
        }
        int boxY = mouseY - 14;
        boxY = Math.max(4, Math.min(boxY, screenHeight - boxH - 4));

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        GuiPanelRender.captureFrame();
        GuiPanelRender.shadow(g, boxX, boxY + 2, boxW, boxH, RADIUS, 8f, 0x90000000);
        GuiPanelRender.glass(g, boxX, boxY, boxW, boxH, RADIUS, GuiPanelRender.PANEL_BLUR, TINT);
        GuiPanelRender.gradient(g, boxX, boxY, boxW, Math.min(boxH, 24), RADIUS, 0x18FFFFFF, 0x00FFFFFF);
        GuiPanelRender.stroke(g, boxX, boxY, boxW, boxH, RADIUS, 0.5f, BORDER);

        int x = boxX + PADDING_X;
        int y = boxY + PADDING_Y;
        int[] rowY = new int[components.size()];
        for (int i = 0; i < components.size(); i++) {
            rowY[i] = y;
            y += components.get(i).getHeight();
            if (i == 0 && components.size() > 1) {
                float lineY = y + TITLE_GAP / 2f - 0.5f;
                GuiPanelRender.gradient(g, x, lineY, contentW, 1f, 0.5f, 0x22FFFFFF, 0x22FFFFFF);
                GuiPanelRender.fill(g, x, lineY, Math.min(components.get(0).getWidth(font), contentW), 1f, 0.5f, GuiPanelRender.ACCENT);
                y += TITLE_GAP;
            }
        }

        var bufferSource = g.bufferSource();
        for (int i = 0; i < components.size(); i++) {
            components.get(i).renderText(font, x, rowY[i], g.pose().last().pose(), bufferSource);
        }
        g.flush();
        for (int i = 0; i < components.size(); i++) {
            components.get(i).renderImage(font, x, rowY[i], g);
        }
        g.pose().popPose();
    }
}
