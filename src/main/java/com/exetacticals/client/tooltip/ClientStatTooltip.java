package com.exetacticals.client.tooltip;

import com.exetacticals.client.screen.GuiPanelRender;
import com.exetacticals.items.tooltip.StatRow;
import com.exetacticals.items.tooltip.StatTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientStatTooltip implements ClientTooltipComponent {
    private static final float SIZE = 8.5f;
    private static final int ROW_HEIGHT = 11;
    private static final int BAR_WIDTH = 64;
    private static final float BAR_HEIGHT = 4f;
    private static final int GAP = 7;
    private static final int MARGIN_TOP = 3;
    private static final int MARGIN_BOTTOM = 3;
    private static final float DELTA_FULL = 30f;

    private static final int LABEL = 0xFFB9BAC4;
    private static final int VALUE = 0xFFF2F2F5;
    private static final int GOOD = 0xFF6FDB8F;
    private static final int BAD = 0xFFFF6464;
    private static final int TRACK = 0x2EFFFFFF;

    public static float barRadius = -1f;

    private static float barR() {
        return barRadius < 0 ? BAR_HEIGHT / 2f : barRadius;
    }

    private final List<StatRow> rows;
    private final List<String> labels;

    public ClientStatTooltip(StatTooltip data) {
        this.rows = data.rows();
        this.labels = rows.stream().map(r -> r.name().getString()).toList();
    }

    @Override
    public int getHeight() {
        return MARGIN_TOP + rows.size() * ROW_HEIGHT + MARGIN_BOTTOM;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return labelWidth(font) + GAP + BAR_WIDTH + GAP + valueWidth(font);
    }

    private static MsdfFont msdf(boolean medium) {
        return medium ? MsdfFont.medium() : MsdfFont.regular();
    }

    private float textWidth(Font vanilla, String s, boolean medium) {
        MsdfFont font = msdf(medium);
        return font == null ? vanilla.width(s) : font.width(s, SIZE);
    }

    private int labelWidth(Font font) {
        float w = 0;
        for (String label : labels) {
            w = Math.max(w, textWidth(font, label, false));
        }
        return (int) Math.ceil(w);
    }

    private int valueWidth(Font font) {
        float w = 0;
        for (StatRow row : rows) {
            w = Math.max(w, textWidth(font, row.text(), true));
        }
        return Math.max((int) Math.ceil(w), 22);
    }

    private void text(GuiGraphics g, Font vanilla, String s, float x, int rowY, int color, boolean medium) {
        MsdfFont font = msdf(medium);
        if (font == null) {
            g.drawString(vanilla, s, (int) x, rowY, color, false);
        } else {
            font.draw(g, FormattedCharSequence.forward(s, Style.EMPTY), x, rowY + 7.5f, SIZE, color, true);
        }
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics g) {
        renderRows(font, x, y, g, null);
    }

    public List<StatRow> rows() {
        return rows;
    }

    public int rowAt(Font font, int x, int y, double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + getWidth(font)) {
            return -1;
        }
        int i = (int) Math.floor((mouseY - y - MARGIN_TOP) / ROW_HEIGHT);
        return i >= 0 && i < rows.size() ? i : -1;
    }

    public void renderRows(Font font, int x, int y, GuiGraphics g, float[] hover) {
        int labelW = labelWidth(font);
        int valueW = valueWidth(font);
        int barX = x + labelW + GAP;
        int valueRight = barX + BAR_WIDTH + GAP + valueW;
        for (int i = 0; i < rows.size(); i++) {
            StatRow row = rows.get(i);
            int rowY = y + MARGIN_TOP + i * ROW_HEIGHT;
            float barY = rowY + (9 - BAR_HEIGHT) / 2f;
            float h = hover != null && i < hover.length ? hover[i] : 0f;
            g.pose().pushPose();
            if (h > 0.001f) {
                GuiPanelRender.fill(g, x - 3, rowY - 1.5f, valueRight - x + 6, ROW_HEIGHT, barR() < 0 ? 3f : Math.max(barR(), 1f),
                        GuiPanelRender.alpha(0x22FFFFFF, h));
                float scale = 1f + 0.06f * h;
                g.pose().translate(x, rowY + 4.5f, 0);
                g.pose().scale(scale, scale, 1f);
                g.pose().translate(-x, -(rowY + 4.5f), 0);
            }

            text(g, font, labels.get(i), x, rowY, LABEL, false);
            GuiPanelRender.fill(g, barX, barY, BAR_WIDTH, BAR_HEIGHT, barR(), TRACK);

            int valueColor = VALUE;
            switch (row.kind()) {
                case DELTA -> {
                    boolean good = row.isImprovement();
                    int color = good ? GOOD : BAD;
                    float half = BAR_WIDTH / 2f;
                    float len = Mth.clamp(Math.abs(row.value()) / DELTA_FULL, 0.06f, 1f) * half;
                    float center = barX + half;
                    float from = good ? center : center - len;
                    GuiPanelRender.gradient(g, from, barY, len, BAR_HEIGHT, barR(), color,
                            GuiPanelRender.mix(color, 0xFF000000, 0.25f));
                    GuiPanelRender.fill(g, center - 0.5f, barY - 1.5f, 1f, BAR_HEIGHT + 3f, 0f, 0x80FFFFFF);
                    valueColor = color;
                }
                case FILL -> {
                    float t = Mth.clamp(row.value(), 0f, 1f);
                    int color = 0xFF000000 | Mth.hsvToRgb(t / 3f, 0.75f, 1f);
                    if (t > 0) {
                        GuiPanelRender.gradient(g, barX, barY, Math.max(BAR_HEIGHT, BAR_WIDTH * t), BAR_HEIGHT,
                                barR(), color, GuiPanelRender.mix(color, 0xFF000000, 0.25f));
                    }
                    valueColor = color;
                }
                default -> {
                    float max = StatReference.max(row.group(), row.key());
                    float t = max <= 0 ? 0f : Mth.clamp(row.value() / max, 0.04f, 1f);
                    int color = GuiPanelRender.ACCENT;
                    GuiPanelRender.gradient(g, barX, barY, BAR_WIDTH * t, BAR_HEIGHT, barR(),
                            GuiPanelRender.mix(color, 0xFFFFFFFF, 0.15f), color);
                    for (int q = 1; q < 4; q++) {
                        GuiPanelRender.fill(g, barX + BAR_WIDTH * q / 4f - 0.25f, barY, 0.5f, BAR_HEIGHT, 0f, 0x40000000);
                    }
                }
            }
            text(g, font, row.text(), valueRight - textWidth(font, row.text(), true), rowY, valueColor, true);
            g.pose().popPose();
        }
    }
}
