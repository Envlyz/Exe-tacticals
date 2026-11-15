package com.exetacticals.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ClientTextLine implements ClientTooltipComponent {
    public static final float TITLE_SIZE = 10.5f;
    public static final float BODY_SIZE = 8.5f;
    private static final int TEXT = 0xFFC9CAD2;
    private static final int TITLE = 0xFFFFFFFF;

    private final FormattedCharSequence text;
    private final boolean title;

    public ClientTextLine(TextLine line) {
        this.text = Language.getInstance().getVisualOrder(line.text());
        this.title = line.title();
    }

    private MsdfFont font() {
        return title ? MsdfFont.medium() : MsdfFont.regular();
    }

    private float size() {
        return title ? TITLE_SIZE : BODY_SIZE;
    }

    @Override
    public int getHeight() {
        return title ? 12 : 10;
    }

    @Override
    public int getWidth(@NotNull Font vanilla) {
        MsdfFont font = font();
        return font == null ? vanilla.width(text) : (int) Math.ceil(font.width(text, size()));
    }

    @Override
    public void renderImage(@NotNull Font vanilla, int x, int y, @NotNull GuiGraphics g) {
        MsdfFont font = font();
        if (font == null) {
            g.drawString(vanilla, text, x, y, title ? TITLE : TEXT, false);
            return;
        }
        float baseline = y + (title ? 9.5f : 7.5f);
        font.draw(g, text, x, baseline, size(), title ? TITLE : TEXT, true);
    }
}
