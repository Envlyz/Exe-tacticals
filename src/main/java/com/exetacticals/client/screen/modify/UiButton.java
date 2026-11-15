package com.exetacticals.client.screen.modify;

import com.exetacticals.client.screen.GuiPanelRender;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class UiButton extends AbstractButton {
    public static final int SIZE = 18;
    private static final int BACKGROUND = 0x8C1A1A1F;
    private static final int BACKGROUND_HOVERED = 0xD02C2C33;
    private static final int BACKGROUND_DISABLED = 0x50141417;
    private static final int BORDER = 0x26FFFFFF;
    private static final float RADIUS = 5f;

    public interface PressHandler {
        void onPress(double mouseX, double mouseY);
    }

    private final GuiPanelRender.Fade hoverFade = new GuiPanelRender.Fade();
    @Nullable
    private final ResourceLocation icon;
    private final PressHandler handler;
    private boolean enabled = true;
    private boolean danger = false;
    private boolean flat = false;
    private String tooltipKey;
    boolean selfVisible = true;

    public UiButton(int width, int height, @Nullable ResourceLocation icon, Component text, PressHandler handler) {
        super(0, 0, width, height, text);
        this.icon = icon;
        this.handler = handler;
    }

    public static UiButton icon(ResourceLocation icon, Runnable action) {
        return new UiButton(SIZE, SIZE, icon, Component.empty(), (x, y) -> action.run());
    }

    public static UiButton text(int width, Component text, Runnable action) {
        return new UiButton(width, SIZE, null, text, (x, y) -> action.run());
    }

    public UiButton danger() {
        this.danger = true;
        return this;
    }

    public UiButton flat() {
        this.flat = true;
        return this;
    }

    public UiButton tooltip(String key) {
        if (!Objects.equals(tooltipKey, key)) {
            tooltipKey = key;
            setTooltip(key == null ? null : Tooltip.create(Component.translatable(key)));
        }
        return this;
    }

    public void enable(String tooltipKey) {
        enabled = true;
        setVisible(true);
        tooltip(tooltipKey);
    }

    public void disable(String tooltipKey) {
        enabled = false;
        setVisible(true);
        tooltip(tooltipKey);
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVisible(boolean visible) {
        this.selfVisible = visible;
        GunModifyUI.refreshVisibility();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (enabled) {
            handler.onPress(mouseX, mouseY);
        }
    }

    @Override
    public void onPress() {
        if (enabled) {
            handler.onPress(getX(), getY());
        }
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float hover = hoverFade.update(enabled && isHoveredOrFocused());
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        float radius = Math.min(RADIUS, Math.min(w, h) / 2f);
        int accent = danger ? GuiPanelRender.DANGER : GuiPanelRender.ACCENT;

        int background = !enabled ? BACKGROUND_DISABLED : GuiPanelRender.mix(BACKGROUND, BACKGROUND_HOVERED, hover);
        int border = !enabled ? 0x12FFFFFF : GuiPanelRender.mix(BORDER, accent, hover);
        if (flat) {
            background = GuiPanelRender.alpha(background, hover);
            border = GuiPanelRender.alpha(border, hover);
        }
        if (hover > 0.01f) {
            GuiPanelRender.shadow(g, x, y, w, h, radius, 4f, GuiPanelRender.alpha(accent, 0.22f * hover));
        }
        GuiPanelRender.fill(g, x, y, w, h, radius, background);
        GuiPanelRender.stroke(g, x, y, w, h, radius, 0.5f, border);

        int textColor = !enabled ? 0xFF5E5E66 : GuiPanelRender.mix(GuiPanelRender.TEXT, danger ? GuiPanelRender.DANGER : 0xFFFFFFFF, hover);
        boolean hasText = !getMessage().getString().isEmpty();
        var font = Minecraft.getInstance().font;
        int iconSize = Math.min(w, h) - 6;
        int contentWidth = (icon != null ? iconSize : 0) + (hasText ? font.width(getMessage()) : 0)
                + (icon != null && hasText ? 3 : 0);
        int cx = x + (w - contentWidth) / 2;
        if (icon != null) {
            RenderSystem.enableBlend();
            float shade = enabled ? 0.82f + 0.18f * hover : 0.35f;
            g.setColor(shade, shade, shade, enabled ? 1f : 0.6f);
            g.blit(icon, cx, y + (h - iconSize) / 2, iconSize, iconSize, 0, 0, 32, 32, 32, 32);
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
            cx += iconSize + 3;
        }
        if (hasText) {
            g.drawString(font, getMessage(), cx, y + (h - 8) / 2 + 1, textColor, false);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
