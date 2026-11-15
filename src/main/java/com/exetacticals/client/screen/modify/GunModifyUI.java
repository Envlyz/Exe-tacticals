package com.exetacticals.client.screen.modify;

import com.exetacticals.GCR;
import com.exetacticals.client.screen.GuiPanelRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Раскладка и оформление экрана модификации.
 *
 * <pre>
 * ┌ [✥][⟲][+][-][Осмотр] ┐        НАЗВАНИЕ ОРУЖИЯ          ┌ ошибки сборки ┐
 *                            слот · модуль (акцентом)
 *                            подсказки управления
 *
 *                                   (модель)
 *
 *                       ┌ [=======●=========] [⟲] ┐   ← положение на планке
 *              ┌ ▌ПРИЦЕЛ  · 7           < 1/2 >  [Снять] ┐
 *              │ [■][■][■][■][■][■][■][■][■][■]          │   ← подходящие обвесы
 *              └─────────────────────────────────────────┘
 * </pre>
 * Полоса появляется, когда на модели выбран слот или модуль. Клик по карточке сразу ставит обвес.
 */
@OnlyIn(Dist.CLIENT)
public class GunModifyUI {
    public static final ResourceLocation DRAG = GCR.RL(GCR.MODID, "textures/gui/component/drag.png");
    public static final ResourceLocation STOP = GCR.RL(GCR.MODID, "textures/gui/component/stop.png");
    public static final ResourceLocation RESET = GCR.RL(GCR.MODID, "textures/gui/component/reset.png");

    private static final int MARGIN = 10;
    private static final int TOOLBAR_PADDING = 4;
    private static final int GAP = 4;

    public static final int MAX_STRIP_COLUMNS = 10;
    public static final int MAX_STRIP_ROWS = 2;
    private static final int STRIP_PADDING = 8;
    private static final int STRIP_HEADER = 18;
    private static final int SLOT_GAP = 4;
    private static final int SMALL_BTN = 14;

    private static final int SCROLLER_WIDTH = 180;
    private static final int ERROR_PANEL_WIDTH = 170;

    // видимость: всё (скрыто в режиме осмотра), полоса обвесов, ползунок планки
    private static boolean rootVisible = true;
    private static boolean stripVisible = false;
    private static boolean stripPaged = false;
    private static boolean scrollComponentsVisible = false;
    private static final List<Gate> gates = new ArrayList<>();

    private static int screenWidth;
    private static int screenHeight;
    private static int stripColumns = MAX_STRIP_COLUMNS;
    private static int stripX, stripY, stripW, stripH;
    private static int shownSlots;
    private static String pageText = "";

    // заголовок
    private static Component title = Component.empty();
    private static Component slotLabel = Component.empty();
    private static Component moduleLabel = Component.empty();
    private static int totalSuitable;

    public static UiButton btnRemoveModule;
    public static UiButton btnDragModel;
    public static UiButton btnResetModel;
    private static final List<UiButton> toolbar = new ArrayList<>();

    public static RailScroller scroller;
    private static UiButton btnResetScroller;
    private static UiButton btnPrevPage;
    private static UiButton btnNextPage;

    private static final List<DisplaySlot> itemSlots = new ArrayList<>();

    private record Gate(AbstractWidget widget, BooleanSupplier visible) {}

    /** Создаёт все виджеты и добавляет их на экран (вызывается из каждого {@code Screen.init()}). */
    public static void create(GunModifyScreen screen) {
        gates.clear();
        toolbar.clear();
        itemSlots.clear();
        rootVisible = true;
        stripVisible = false;
        stripPaged = false;
        scrollComponentsVisible = false;
        stripW = 0;
        screenWidth = screen.width;
        screenHeight = screen.height;

        createToolbar(screen);
        createScroller(screen);
        createStrip(screen);
        refreshVisibility();
    }

    private static void createToolbar(GunModifyScreen screen) {
        btnDragModel = new UiButton(UiButton.SIZE, UiButton.SIZE, DRAG, Component.empty(), screen::onDragModel)
                .tooltip("tooltip.btn.drag");
        btnResetModel = UiButton.icon(RESET, screen::resetModelPose).tooltip("tooltip.btn.reset");
        UiButton zoomIn = UiButton.text(UiButton.SIZE, Component.literal("+"), screen::zoomInModel).tooltip("tooltip.btn.zoom_in");
        UiButton zoomOut = UiButton.text(UiButton.SIZE, Component.literal("-"), screen::zoomOutModel).tooltip("tooltip.btn.zoom_out");
        Component inspect = Component.translatable("tooltip.btn.inspect");
        UiButton btnInspect = UiButton.text(Minecraft.getInstance().font.width(inspect) + 12, inspect, screen::enterInspectionMode);

        int x = MARGIN + TOOLBAR_PADDING;
        int y = MARGIN + TOOLBAR_PADDING;
        for (UiButton button : List.of(btnDragModel, btnResetModel, zoomIn, zoomOut, btnInspect)) {
            button.setPosition(x, y);
            x += button.getWidth() + GAP;
            toolbar.add(button);
            add(screen, button, () -> rootVisible);
        }
    }

    private static void createScroller(GunModifyScreen screen) {
        int width = Math.min(SCROLLER_WIDTH, screenWidth - MARGIN * 2 - UiButton.SIZE - 20);
        scroller = new RailScroller(10, screen::trySetNodePos).setRange(0f, 1f);
        scroller.setWidth(Math.max(40, width));
        add(screen, scroller, () -> rootVisible && stripVisible && scrollComponentsVisible);

        btnResetScroller = UiButton.icon(RESET, screen::resetScroller).flat().tooltip("tooltip.btn.reset_scroll");
        add(screen, btnResetScroller, () -> rootVisible && stripVisible && scrollComponentsVisible);
    }

    private static void createStrip(GunModifyScreen screen) {
        int available = screenWidth - (MARGIN + STRIP_PADDING) * 2;
        stripColumns = Mth.clamp((available + SLOT_GAP) / (DisplaySlot.SIZE + SLOT_GAP), 1, MAX_STRIP_COLUMNS);
        for (int i = 0; i < stripColumns * MAX_STRIP_ROWS; i++) {
            DisplaySlot slot = new DisplaySlot(screen::onAttachmentSlotClick);
            itemSlots.add(slot);
            final int index = i;
            add(screen, slot, () -> rootVisible && stripVisible && index < shownSlots);
        }
        btnPrevPage = new UiButton(SMALL_BTN, SMALL_BTN, null, Component.literal("<"), (mx, my) -> screen.changePage(-1)).flat();
        btnNextPage = new UiButton(SMALL_BTN, SMALL_BTN, null, Component.literal(">"), (mx, my) -> screen.changePage(1)).flat();
        add(screen, btnPrevPage, () -> rootVisible && stripVisible && stripPaged);
        add(screen, btnNextPage, () -> rootVisible && stripVisible && stripPaged);

        Component remove = Component.translatable("tooltip.btn.uninstall");
        btnRemoveModule = new UiButton(Minecraft.getInstance().font.width(remove) + 22, SMALL_BTN, STOP, remove,
                (mx, my) -> screen.removeSelectedModule()).danger();
        add(screen, btnRemoveModule, () -> rootVisible && stripVisible && btnRemoveModule.selfVisible);
    }

    private static void add(GunModifyScreen screen, AbstractWidget widget, BooleanSupplier visible) {
        gates.add(new Gate(widget, visible));
        screen.addUiWidget(widget);
    }

    /** Пересчитывает видимость всех виджетов по текущему состоянию. */
    public static void refreshVisibility() {
        for (Gate gate : gates) {
            gate.widget().visible = gate.visible().getAsBoolean();
        }
    }

    public static void setVisible(boolean visible) {
        rootVisible = visible;
        refreshVisibility();
    }

    // ---------------------------------------------------------------- полоса обвесов

    public static void setStripVisible(boolean visible) {
        stripVisible = visible;
        if (!visible) {
            shownSlots = 0;
            stripW = 0;
        }
        refreshVisibility();
    }

    /** Подписи над полосой и в заголовке экрана. */
    public static void setLabels(Component gunName, Component slot, Component module, int suitableCount) {
        title = gunName == null ? Component.empty() : gunName;
        slotLabel = slot == null ? Component.empty() : slot;
        moduleLabel = module == null ? Component.empty() : module;
        totalSuitable = suitableCount;
    }

    /**
     * Подгоняет полосу под число карточек на странице и ставит её по центру внизу экрана,
     * ползунок планки — над ней. Вызывать после того, как предметы страницы разложены по слотам.
     */
    public static void layoutStrip(boolean paged) {
        if (btnPrevPage == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int filled = 0;
        for (DisplaySlot slot : itemSlots) {
            if (!slot.getValue().isEmpty()) {
                filled++;
            }
        }
        stripPaged = paged && stripVisible;
        shownSlots = stripVisible ? filled : 0;
        if (!stripVisible) {
            stripW = 0;
            refreshVisibility();
            return;
        }
        // постраничная полоса держит размер полной страницы, чтобы кнопки не прыгали
        int cells = stripPaged ? itemSlots.size() : Math.max(filled, 1);
        int columns = Math.min(cells, stripColumns);
        int rows = Math.max(1, (cells + columns - 1) / columns);
        int slotsW = columns * DisplaySlot.SIZE + (columns - 1) * SLOT_GAP;
        int slotsH = rows * DisplaySlot.SIZE + (rows - 1) * SLOT_GAP;

        int headerRight = (btnRemoveModule.selfVisible ? btnRemoveModule.getWidth() + GAP : 0)
                + (stripPaged ? SMALL_BTN * 2 + font.width(pageText) + 8 : 0);
        int headerW = 10 + font.width(slotLabel) + 6 + font.width(" · " + totalSuitable) + 12 + headerRight;
        int emptyW = filled == 0 ? font.width(Component.translatable("gui.exe_tacticals.modify.no_attachments")) + 8 : 0;
        stripW = Math.max(Math.max(slotsW, headerW), Math.max(emptyW, 120)) + STRIP_PADDING * 2;
        stripH = STRIP_HEADER + (filled == 0 ? 20 : slotsH) + STRIP_PADDING + 2;
        stripX = (screenWidth - stripW) / 2;
        stripY = screenHeight - MARGIN - stripH;

        int slotsX = stripX + (stripW - slotsW) / 2;
        int slotsY = stripY + STRIP_HEADER + 2;
        for (int i = 0; i < itemSlots.size(); i++) {
            DisplaySlot slot = itemSlots.get(i);
            slot.setPosition(slotsX + (i % columns) * (DisplaySlot.SIZE + SLOT_GAP),
                    slotsY + (i / columns) * (DisplaySlot.SIZE + SLOT_GAP));
        }

        int headerY = stripY + (STRIP_HEADER - SMALL_BTN) / 2 + 2;
        int right = stripX + stripW - STRIP_PADDING;
        if (btnRemoveModule.selfVisible) {
            right -= btnRemoveModule.getWidth();
            btnRemoveModule.setPosition(right, headerY);
            right -= GAP;
        }
        if (stripPaged) {
            right -= SMALL_BTN;
            btnNextPage.setPosition(right, headerY);
            right -= font.width(pageText) + 8;
            pageTextX = right + 4;
            right -= SMALL_BTN;
            btnPrevPage.setPosition(right, headerY);
        }

        int scrollerW = scroller.getWidth();
        int rowW = scrollerW + 4 + UiButton.SIZE;
        int rowX = (screenWidth - rowW) / 2;
        int rowY = stripY - 10 - UiButton.SIZE;
        scroller.setPosition(rowX, rowY + (UiButton.SIZE - scroller.getHeight()) / 2);
        btnResetScroller.setPosition(rowX + scrollerW + 4, rowY);
        refreshVisibility();
    }

    private static int pageTextX;

    public static void setScrollComponentsVisible(boolean visible) {
        scrollComponentsVisible = visible;
        refreshVisibility();
    }

    public static void setScrollPos(float pos) {
        if (scroller != null) {
            scroller.setValue(pos);
        }
    }

    public static void clearItemSlots() {
        for (DisplaySlot slot : itemSlots) {
            slot.setItem(ItemStack.EMPTY);
            slot.setInstalled(false);
        }
    }

    public static List<DisplaySlot> getItemSlots() {
        return itemSlots;
    }

    public static int getPageSize() {
        return itemSlots.size();
    }

    public static void syncPageInfo(GunModifyScreen screen) {
        int maxPage = screen.getMaxPage();
        pageText = maxPage == 0 ? "" : screen.getPage() + " / " + maxPage;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GunModifyScreen(Component.empty()));
    }

    /** Клики по панелям не должны уходить на модель за ними. */
    public static boolean isOverPanel(double mouseX, double mouseY) {
        if (!rootVisible) {
            return false;
        }
        if (stripW > 0 && mouseX >= stripX && mouseX <= stripX + stripW && mouseY >= stripY && mouseY <= stripY + stripH) {
            return true;
        }
        return stripW > 0 && scrollComponentsVisible
                && mouseX >= scroller.getX() - 8 && mouseX <= btnResetScroller.getX() + UiButton.SIZE + 6
                && mouseY >= btnResetScroller.getY() - 5 && mouseY <= btnResetScroller.getY() + UiButton.SIZE + 5;
    }

    // ---------------------------------------------------------------- отрисовка

    /** До виджетов: стеклянные подложки и надписи, которые должны быть под ними. */
    public static void renderBackground(GuiGraphics g) {
        if (!rootVisible) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        renderToolbarPanel(g);
        renderTitle(g, font);

        if (stripW <= 0) {
            return;
        }
        if (scrollComponentsVisible) {
            float left = scroller.getX() - 8;
            float right = btnResetScroller.getX() + UiButton.SIZE + 6;
            GuiPanelRender.panel(g, left, btnResetScroller.getY() - 5, right - left, UiButton.SIZE + 10, 9f);
        }
        GuiPanelRender.panel(g, stripX, stripY, stripW, stripH);
        // заголовок полосы: акцентная черта, название слота, число вариантов
        int hx = stripX + STRIP_PADDING;
        int hy = stripY + 7;
        GuiPanelRender.fill(g, hx, hy - 1, 2, 10, 1, GuiPanelRender.ACCENT);
        g.drawString(font, slotLabel, hx + 6, hy, GuiPanelRender.TEXT, false);
        g.drawString(font, " · " + totalSuitable, hx + 6 + font.width(slotLabel), hy, GuiPanelRender.TEXT_DIM, false);
        if (stripPaged) {
            g.drawString(font, pageText, pageTextX, hy, GuiPanelRender.TEXT_DIM, false);
        }
        GuiPanelRender.fill(g, stripX + STRIP_PADDING, stripY + STRIP_HEADER - 1, stripW - STRIP_PADDING * 2, 0.5f, 0, 0x1EFFFFFF);
        if (shownSlots == 0) {
            g.drawCenteredString(font, Component.translatable("gui.exe_tacticals.modify.no_attachments"),
                    stripX + stripW / 2, stripY + STRIP_HEADER + 8, GuiPanelRender.TEXT_DIM);
        }
    }

    private static void renderToolbarPanel(GuiGraphics g) {
        int left = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int top = 0;
        for (UiButton button : toolbar) {
            left = Math.min(left, button.getX());
            right = Math.max(right, button.getX() + button.getWidth());
            top = button.getY();
        }
        if (left > right) {
            return;
        }
        GuiPanelRender.panel(g, left - TOOLBAR_PADDING, top - TOOLBAR_PADDING,
                right - left + TOOLBAR_PADDING * 2, UiButton.SIZE + TOOLBAR_PADDING * 2, 8f);
    }

    private static void renderTitle(GuiGraphics g, Font font) {
        int cx = screenWidth / 2;
        int y = MARGIN + 2;
        // название оружия — крупнее обычного текста
        float scale = 1.5f;
        int tw = font.width(title);
        g.pose().pushPose();
        g.pose().translate(cx - tw * scale / 2f, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, title, 0, 0, GuiPanelRender.TEXT, true);
        g.pose().popPose();
        y += 16;

        Component sub;
        if (!slotLabel.getString().isEmpty()) {
            sub = moduleLabel.getString().isEmpty()
                    ? Component.translatable("gui.exe_tacticals.modify.empty_slot", slotLabel)
                    : Component.empty().append(slotLabel).append("  ·  ").append(moduleLabel);
            g.drawCenteredString(font, sub, cx, y, GuiPanelRender.ACCENT);
        } else {
            g.drawCenteredString(font, Component.translatable("gui.exe_tacticals.modify.pick_slot"), cx, y, GuiPanelRender.TEXT_DIM);
        }
        y += 12;
        // линия-разделитель под заголовком, гаснущая к краям
        float lineW = Math.max(80f, tw * scale + 40f);
        for (int i = 0; i < 4; i++) {
            float w = lineW * (1f - i / 4f);
            GuiPanelRender.fill(g, cx - w / 2f, y, w, 0.5f, 0f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.18f));
        }
    }

    /** После виджетов: подсказки, ошибки сборки, тултип наведённого обвеса. */
    public static void renderForeground(GuiGraphics g, int mouseX, int mouseY, List<Component> issues) {
        if (!rootVisible) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        renderHints(g, font);
        renderIssues(g, font, issues);
        g.pose().popPose();
        for (DisplaySlot slot : itemSlots) {
            if (slot.visible && slot.isMouseOverSlot()) {
                g.renderTooltip(font, slot.getValue(), mouseX, mouseY);
                break;
            }
        }
    }

    private static void renderHints(GuiGraphics g, Font font) {
        Component hint = Component.translatable("gui.exe_tacticals.modify.hint");
        int y = MARGIN + 2 + 16 + 12 + 5;
        g.pose().pushPose();
        g.pose().translate(screenWidth / 2f, y, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawCenteredString(font, hint, 0, 0, 0xB0A0A0AA);
        g.pose().popPose();
    }

    private static void renderIssues(GuiGraphics g, Font font, List<Component> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }
        int innerW = ERROR_PANEL_WIDTH - 16;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component issue : issues) {
            lines.addAll(font.split(issue, innerW));
        }
        int maxLines = Math.max(1, (screenHeight / 2) / 10);
        int count = Math.min(lines.size(), maxLines);
        int h = 22 + count * 10 + 4;
        int x = screenWidth - MARGIN - ERROR_PANEL_WIDTH;
        int y = MARGIN;
        GuiPanelRender.panel(g, x, y, ERROR_PANEL_WIDTH, h);
        GuiPanelRender.fill(g, x, y + 6, 2, h - 12, 1, GuiPanelRender.DANGER);
        g.drawString(font, Component.translatable("gui.exe_tacticals.modify.issues"), x + 9, y + 7, GuiPanelRender.DANGER, false);
        for (int i = 0; i < count; i++) {
            g.drawString(font, lines.get(i), x + 9, y + 21 + i * 10, 0xFFE6C8C8, false);
        }
    }
}
