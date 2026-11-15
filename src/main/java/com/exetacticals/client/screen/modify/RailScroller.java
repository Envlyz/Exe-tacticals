package com.exetacticals.client.screen.modify;

import com.exetacticals.client.screen.GuiPanelRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 水平滑条，用来调整导轨上模块的位置。
 * 只能拖动滑块；点击轨道和鼠标滚轮都不改变数值（滚轮留给模型缩放）。
 */
@OnlyIn(Dist.CLIENT)
public class RailScroller extends AbstractWidget {
    private static final int THUMB_WIDTH = 12;

    private final GuiPanelRender.Fade activeFade = new GuiPanelRender.Fade();
    private final Consumer<Float> onValueChanged;
    private Predicate<Float> preValueChange = f -> true;
    private float minValue = 0f;
    private float maxValue = 1f;
    private float value = 0f;
    private boolean dragging = false;
    private double dragStartX;
    private float dragStartValue;

    public RailScroller(int height, Consumer<Float> onValueChanged) {
        super(0, 0, 0, height, Component.empty());
        this.onValueChanged = onValueChanged;
    }

    public RailScroller setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    public RailScroller setPreValueChange(Predicate<Float> preValueChange) {
        this.preValueChange = preValueChange;
        return this;
    }

    /** 设置数值；数值真的变化时通知监听者（与原 LDLib 滑条的行为一致）。 */
    public void setValue(float newValue) {
        newValue = Mth.clamp(newValue, minValue, maxValue);
        if (newValue != value) {
            value = newValue;
            onValueChanged.accept(value);
        }
    }

    public float getValue() {
        return value;
    }

    private int thumbX() {
        float t = maxValue == minValue ? 0 : (value - minValue) / (maxValue - minValue);
        return getX() + Math.round(t * (getWidth() - THUMB_WIDTH));
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        float trackH = 4f;
        float trackY = y + (h - trackH) / 2f;
        int tx = thumbX();
        boolean hot = dragging || (isHovered && mouseX >= tx && mouseX < tx + THUMB_WIDTH);
        float active = activeFade.update(hot);
        // дорожка: пройденная часть подсвечена акцентом
        GuiPanelRender.fill(g, x, trackY, w, trackH, trackH / 2f, 0xA0101013);
        GuiPanelRender.fill(g, x, trackY, tx - x + THUMB_WIDTH / 2f, trackH, trackH / 2f, GuiPanelRender.ACCENT_SOFT);
        GuiPanelRender.stroke(g, x, trackY, w, trackH, trackH / 2f, 0.5f, 0x26FFFFFF);
        // деления
        for (int i = 1; i < 10; i++) {
            float dx = x + THUMB_WIDTH / 2f + (w - THUMB_WIDTH) * i / 10f;
            GuiPanelRender.fill(g, dx - 0.25f, trackY + trackH + 1.5f, 0.5f, i == 5 ? 3f : 2f, 0f, 0x40FFFFFF);
        }
        if (active > 0.01f) {
            GuiPanelRender.shadow(g, tx, y, THUMB_WIDTH, h, h / 2f, 4f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.35f * active));
        }
        GuiPanelRender.fill(g, tx, y, THUMB_WIDTH, h, h / 2f, GuiPanelRender.mix(0xFFD6D6DC, GuiPanelRender.ACCENT, active));
        GuiPanelRender.fill(g, tx + THUMB_WIDTH / 2f - 0.5f, y + 2.5f, 1f, h - 5f, 0.5f, 0x55000000);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int tx = thumbX();
        dragging = mouseX >= tx && mouseX < tx + THUMB_WIDTH;
        dragStartX = mouseX;
        dragStartValue = value;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (!dragging) {
            return;
        }
        float remaining = getWidth() - THUMB_WIDTH;
        if (remaining <= 0) {
            return;
        }
        float newValue = dragStartValue + (float) ((mouseX - dragStartX) / remaining) * (maxValue - minValue);
        if (!preValueChange.test(newValue)) {
            return;
        }
        setValue(newValue);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
