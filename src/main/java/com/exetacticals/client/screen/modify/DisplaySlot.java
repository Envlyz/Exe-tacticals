package com.exetacticals.client.screen.modify;

import com.exetacticals.client.screen.GuiPanelRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class DisplaySlot extends AbstractWidget {
    public static final int SIZE = 32;
    private static final float ITEM_SCALE = 1.625f;
    private static final float RADIUS = 6f;
    private static final int BACKGROUND_TOP = 0x5C9A9AA4;
    private static final int BACKGROUND = 0x40606068;
    private static final int BACKGROUND_HOVERED = 0x80B4B4BE;
    private static final int BORDER = 0x30FFFFFF;

    private final GuiPanelRender.Fade hoverFade = new GuiPanelRender.Fade(16f);
    private final Consumer<DisplaySlot> onClick;
    private ItemStack value = ItemStack.EMPTY;
    private int count = 0;
    private boolean installed = false;

    public DisplaySlot(Consumer<DisplaySlot> onClick) {
        super(0, 0, SIZE, SIZE, Component.empty());
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (value.isEmpty()) {
            return;
        }
        boolean available = count > 0 || installed;
        float hover = hoverFade.update(isHovered && available);
        int x = getX();
        int y = getY();

        if (installed) {
            GuiPanelRender.shadow(g, x, y, SIZE, SIZE, RADIUS, 5f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.35f));
        } else if (hover > 0.01f) {
            GuiPanelRender.shadow(g, x, y, SIZE, SIZE, RADIUS, 5f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.25f * hover));
        }
        GuiPanelRender.gradient(g, x, y, SIZE, SIZE, RADIUS,
                GuiPanelRender.mix(BACKGROUND_TOP, BACKGROUND_HOVERED, hover),
                GuiPanelRender.mix(BACKGROUND, BACKGROUND_HOVERED, hover));
        int border = installed ? GuiPanelRender.ACCENT : GuiPanelRender.mix(BORDER, GuiPanelRender.ACCENT, hover);
        GuiPanelRender.stroke(g, x, y, SIZE, SIZE, RADIUS, installed ? 1f : 0.5f, border);

        float scale = ITEM_SCALE * (1f + 0.08f * hover);
        float offset = (SIZE - 16 * scale) / 2f;
        g.pose().pushPose();
        g.pose().translate(x + offset, y + offset - hover, 0f);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(value, 0, 0);
        g.pose().popPose();

        g.pose().pushPose();
        g.pose().translate(0f, 0f, 300f);
        if (!available) {
            GuiPanelRender.fill(g, x, y, SIZE, SIZE, RADIUS, 0xA00C0C0E);
        }
        Font font = Minecraft.getInstance().font;
        if (installed) {
            GuiPanelRender.circle(g, x + 5f, y + 5f, 2.5f, GuiPanelRender.ACCENT);
        }
        if (!installed || count > 0) {
            String text = count > 99 ? "99+" : Integer.toString(count);
            int tw = font.width(text);
            float bw = tw + 5f;
            float bx = x + SIZE - bw - 1.5f;
            float by = y + SIZE - 9.5f;
            GuiPanelRender.fill(g, bx, by, bw, 8f, 4f, 0xC0000000);
            int color = count > 0 ? GuiPanelRender.OK : GuiPanelRender.DANGER;
            g.pose().pushPose();
            g.pose().translate(bx + 2.5f + tw * 0.125f, by + 1.5f, 0f);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.drawString(font, text, 0, 0, color, false);
            g.pose().popPose();
        }
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || value.isEmpty() || !clicked(mouseX, mouseY)) {
            return false;
        }
        if (count > 0 && !installed) {
            playDownSound(Minecraft.getInstance().getSoundManager());
        }
        onClick.accept(this);
        return true;
    }

    public boolean isMouseOverSlot() {
        return visible && isHovered && !value.isEmpty();
    }

    public void setItem(ItemStack stack) {
        this.value = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack getValue() {
        return value;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public boolean isInstalled() {
        return installed;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
