package com.exetacticals.client.screen;

import com.exetacticals.client.tooltip.ClientStatTooltip;
import com.exetacticals.client.tooltip.MsdfFont;
import com.exetacticals.items.ModuleItem;
import com.exetacticals.items.tooltip.StatRow;
import com.exetacticals.items.tooltip.StatTooltip;
import com.exetacticals.network.GCRNetwork;
import com.exetacticals.network.c2s.CraftGunPacket;
import com.exetacticals.workbench.GunCraftingRecipe;
import com.exetacticals.workbench.GunWorkbenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Меню оружейного верстака — почти на весь экран.
 * <pre>
 * ┌ ▌ОРУЖЕЙНЫЙ ВЕРСТАК ────────────────────────────────────────────────── Рецептов: 5 ┐
 * │ [M4A1    ][АК-74М  ][АКМН    ][AK24    ] │ ┌───────────────────────────────────┐ │
 * │ [MCX     ]                               │ │  [большая иконка]   M4A1          │ │
 * │                                          │ └───────────────────────────────────┘ │
 * │                                          │ Характеристики     │ Требуется        │
 * │                                          │ ▬▬▬▬▬▬             │ [■] 12 / 20      │
 * │                                          │                    │     [ Создать ]  │
 * └───────────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * Слева — сетка карточек 120×30, до 4 в ряд (на узком экране меньше), прокрутка колесом;
 * справа — выбранное оружие: характеристики полосками, ресурсы «есть / нужно», кнопка «Создать».
 * Крафт проверяет сервер ({@link CraftGunPacket}).
 */
@OnlyIn(Dist.CLIENT)
public class GunWorkbenchScreen extends AbstractContainerScreen<GunWorkbenchMenu> {
    private static final int MARGIN = 10;
    /** всё меню целиком (с текстом и карточками) уменьшено пропорционально, вокруг центра экрана */
    private static final float UI_SCALE = 0.8f;
    private static final int PAD = 10;
    private static final int HEADER_H = 26;
    private static final int CARD_W = 120;
    private static final int CARD_H = 30;
    private static final int CARD_GAP = 4;
    private static final int MAX_COLUMNS = 4;
    /** справа должно остаться хотя бы столько под выбранное оружие */
    private static final int DETAILS_MIN_W = 310;
    /** строка с названием над окном осмотра */
    private static final int TITLE_H = 30;
    /** под характеристиками должно остаться столько (8 строк полосок + заголовок) */
    private static final int STATS_H = 112;
    private static final int ING_W = 140;
    private static final int ING_ROW = 18;
    private static final int BTN_W = 120;
    private static final int BTN_H = 22;

    private static final int TEXT = 0xFFEDEDF0;
    private static final int TEXT_DIM = 0xFF9A9AA6;
    private static final int OK = 0xFF6FDB8F;
    private static final int BAD = 0xFFFF6464;

    /** все рецепты верстака */
    private List<GunCraftingRecipe> allRecipes = new ArrayList<>();
    /** рецепты выбранной категории (вкладки над карточками) — то, что видно в сетке */
    private List<GunCraftingRecipe> recipes = new ArrayList<>();
    /** категории рецептов по порядку; вкладки рисуются, если их больше одной ({@code null} — «Все») */
    private final List<String> categories = new ArrayList<>();
    private String category = null;
    private final GuiPanelRender.Fade[] tabHover = new GuiPanelRender.Fade[32];
    private static final int TAB_H = 15;
    private static final int TAB_GAP = 4;
    /** порядок вкладок; остальные категории — после, по алфавиту */
    private static final List<String> CATEGORY_ORDER = List.of("rifle", "magazine_ar", "magazine_ak",
            "sight", "rail", "suppressor", "muzzle", "stock", "grip", "foregrip", "handguard", "barrel",
            "underbarrel", "laser", "dustcover", "ammo");
    private int selected = 0;
    private float scroll = 0;
    private int columns = MAX_COLUMNS;
    private final GuiPanelRender.Fade buttonHover = new GuiPanelRender.Fade();
    private final GuiPanelRender.Fade[] cardHover = new GuiPanelRender.Fade[128];
    // осмотр: поворот модели мышью, без неё — медленное вращение
    private float viewYaw = -30f;
    private float viewPitch = 12f;
    private boolean dragging;
    private long lastInteraction;
    private long lastFrame;
    /** приближение модели в окне осмотра: кнопки «+»/«−» в углу и колесо над окном */
    private float viewZoom = 0.55f; // по умолчанию
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.15f;
    private static final int ZOOM_BTN = 14;
    private final GuiPanelRender.Fade zoomInHover = new GuiPanelRender.Fade();
    private final GuiPanelRender.Fade zoomOutHover = new GuiPanelRender.Fade();
    // ховер характеристик: плавное увеличение строки и тултип с пояснением
    private final GuiPanelRender.Fade[] statHover = new GuiPanelRender.Fade[16];
    private ClientStatTooltip statBlock;
    private int statX;
    private int statY;
    private int hoveredStat = -1;
    private final GuiPanelRender.Fade[] ingHover = new GuiPanelRender.Fade[16];
    private int hoveredIng = -1;
    private int hoveredCard = -1;
    private static final int CLOSE_BTN = 16;
    private final GuiPanelRender.Fade closeHover = new GuiPanelRender.Fade();

    {
        for (int i = 0; i < statHover.length; i++) {
            statHover[i] = new GuiPanelRender.Fade(16f);
        }
        for (int i = 0; i < ingHover.length; i++) {
            ingHover[i] = new GuiPanelRender.Fade(16f);
        }
    }

    public GunWorkbenchScreen(GunWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        for (int i = 0; i < cardHover.length; i++) {
            cardHover[i] = new GuiPanelRender.Fade(14f);
        }
        for (int i = 0; i < tabHover.length; i++) {
            tabHover[i] = new GuiPanelRender.Fade(16f);
        }
    }

    @Override
    protected void init() {
        imageWidth = Math.max(200, width - MARGIN * 2);
        baseHeight = Math.max(150, height - MARGIN * 2);
        imageHeight = baseHeight;
        super.init();
        GunCraftingRecipe keep = recipes.isEmpty() ? null : recipes.get(Mth.clamp(selected, 0, recipes.size() - 1));
        allRecipes = new ArrayList<>(menu.recipes());
        allRecipes.sort(java.util.Comparator.comparingInt((GunCraftingRecipe r) -> categoryRank(r.category()))
                .thenComparing(GunCraftingRecipe::category));
        categories.clear();
        for (GunCraftingRecipe r : allRecipes) {
            if (!categories.contains(r.category())) {
                categories.add(r.category());
            }
        }
        if (category != null && !categories.contains(category)) {
            category = null;
        }
        applyCategory();
        selected = keep != null && recipes.contains(keep) ? recipes.indexOf(keep)
                : Mth.clamp(selected, 0, Math.max(0, recipes.size() - 1));
        int available = imageWidth - PAD * 3 - DETAILS_MIN_W;
        columns = Mth.clamp((available + CARD_GAP) / (CARD_W + CARD_GAP), 1, MAX_COLUMNS);
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private static int categoryRank(String c) {
        int i = CATEGORY_ORDER.indexOf(c);
        return i < 0 ? CATEGORY_ORDER.size() : i;
    }

    private void applyCategory() {
        recipes = category == null ? allRecipes : allRecipes.stream().filter(r -> r.category().equals(category)).toList();
    }

    /** Вкладки категорий — если категорий больше одной; у верстака магазинов их нет (там всего 10 рецептов). */
    private boolean hasTabs() {
        return categories.size() > 1 && menu.type() != com.exetacticals.workbench.WorkbenchType.MAGAZINE;
    }

    /** Название вкладки: множественное число ({@code workbench.group.<кат>}), если есть, иначе название категории. */
    private static String tabName(String c) {
        if (c == null) {
            return Component.translatable("gui.exe_tacticals.workbench.group.all").getString();
        }
        String key = "gui.exe_tacticals.workbench.group." + c;
        return net.minecraft.client.resources.language.I18n.exists(key)
                ? Component.translatable(key).getString()
                : Component.translatable("gui.exe_tacticals.workbench.category." + c).getString();
    }

    /** Вкладки над карточками: {x, y, w} каждой (первая — «Все»), с переносом по ширине сетки. */
    private List<float[]> tabRects() {
        List<float[]> out = new ArrayList<>();
        if (!hasTabs()) {
            return out;
        }
        float x = gridX(), y = gridY(), right = gridX() + gridW();
        for (int i = 0; i <= categories.size(); i++) {
            String c = i == 0 ? null : categories.get(i - 1);
            float w = width(tabName(c), 8.5f, false) + 14;
            if (x + w > right && x > gridX()) {
                x = gridX();
                y += TAB_H + TAB_GAP;
            }
            out.add(new float[]{x, y, w});
            x += w + TAB_GAP;
        }
        return out;
    }

    /** высота полосы вкладок (с отступом снизу) */
    private int tabsH() {
        List<float[]> rects = tabRects();
        if (rects.isEmpty()) {
            return 0;
        }
        return (int) (rects.get(rects.size() - 1)[1] - gridY()) + TAB_H + 8;
    }

    /** верх и высота области карточек (под вкладками) */
    private int cardsY() {
        return gridY() + tabsH();
    }

    private int cardsH() {
        return gridH() - tabsH();
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        List<float[]> rects = tabRects();
        for (int i = 0; i < rects.size(); i++) {
            float[] r = rects.get(i);
            String c = i == 0 ? null : categories.get(i - 1);
            boolean sel = java.util.Objects.equals(c, category);
            boolean hovered = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + TAB_H;
            float hover = tabHover[Math.min(i, tabHover.length - 1)].update(hovered);
            int fill = sel ? GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.3f) : GuiPanelRender.mix(0x14FFFFFF, 0x2AFFFFFF, hover);
            GuiPanelRender.fill(g, r[0], r[1], r[2], TAB_H, round(r[2], TAB_H), fill);
            GuiPanelRender.stroke(g, r[0], r[1], r[2], TAB_H, round(r[2], TAB_H), sel ? 1f : 0.5f,
                    sel ? GuiPanelRender.ACCENT : GuiPanelRender.mix(0x22FFFFFF, 0x50FFFFFF, hover));
            text(g, tabName(c), r[0] + 7, r[1] + 10.5f, 8.5f, sel ? TEXT : GuiPanelRender.mix(TEXT_DIM, TEXT, hover), false);
        }
    }

    /** Одно скругление углов для всего в меню верстака, не зависит от размера плашки (шейдер сам ограничит половиной стороны). */
    public static final float ROUND = 5f;

    private static float round(float w, float h) {
        return ROUND;
    }

    /** Копия результата для окна модели и характеристик — предмет рецепта не трогаем (рендер дописывает в него данные). */
    private GunCraftingRecipe previewRecipe;
    private ItemStack previewStack = ItemStack.EMPTY;

    private ItemStack preview(GunCraftingRecipe recipe) {
        if (recipe != previewRecipe) {
            previewRecipe = recipe;
            previewStack = recipe == null ? ItemStack.EMPTY : new ItemStack(recipe.result().getItem());
        }
        return previewStack;
    }

    private GunCraftingRecipe current() {
        return recipes.isEmpty() ? null : recipes.get(selected);
    }

    // ---------------------------------------------------------------- геометрия

    private int gridX() {
        return leftPos + PAD;
    }

    private int gridY() {
        return topPos + HEADER_H + 6;
    }

    private int gridW() {
        return columns * CARD_W + (columns - 1) * CARD_GAP;
    }

    /** высота панели без растяжки; окно модели считается от неё, чтобы не прыгало */
    private int baseHeight;

    /**
     * Высота панели постоянная — такая, чтобы поместилось всё у AK24 (если его рецепта нет — у первого);
     * не больше, чем влезает в экран с учётом общего уменьшения.
     */
    private void layout() {
        GunCraftingRecipe reference = null;
        for (GunCraftingRecipe r : allRecipes) {
            net.minecraft.resources.ResourceLocation id =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(r.result().getItem());
            if (id != null && id.getPath().equals("ak24")) {
                reference = r;
                break;
            }
        }
        if (reference == null) {
            // верстаки магазинов/обвесов/патронов: высота — по самому длинному рецепту, чтобы панель не прыгала
            int best = -1;
            for (GunCraftingRecipe r : allRecipes) {
                int extra = extraBelowStats(r);
                if (extra > best) {
                    best = extra;
                    reference = r;
                }
            }
        }
        int need = HEADER_H + 6 + detailsY() + 11 + extraBelowStats(reference) + PAD + 4;
        int max = (int) ((height - MARGIN * 2) / UI_SCALE);
        int h = Mth.clamp(need, baseHeight, Math.max(baseHeight, max));
        if (h != imageHeight) {
            imageHeight = h;
            topPos = (height - imageHeight) / 2;
        }
    }

    /** высота полосок + разделителя + патрона + столбика магазинов у выбранного оружия */
    private final java.util.Map<GunCraftingRecipe, Integer> extraCache = new java.util.IdentityHashMap<>();

    private int extraBelowStats(GunCraftingRecipe recipe) {
        return recipe == null ? 0 : extraCache.computeIfAbsent(recipe, this::computeExtraBelowStats);
    }

    private int computeExtraBelowStats(GunCraftingRecipe recipe) {
        if (recipe == null) {
            return 0;
        }
        if (!(recipe.result().getItem() instanceof ModuleItem<?> item)) {
            int guns = compatibleGuns(recipe).size();
            return guns == 0 ? 0 : AMMO_GAP + 1 + AMMO_GAP + 14 + guns * AMMO_ROW;
        }
        List<StatRow> rows = new ArrayList<>();
        try {
            item.getModule().appendStatRows(new ItemStack(recipe.result().getItem()), rows);
        } catch (RuntimeException ignored) {
            // пусто
        }
        int stats = rows.isEmpty() ? 0 : 6 + rows.size() * 11;
        int mags = compatibleMagazines(recipe).size();
        int guns = compatibleGuns(recipe).size();
        return stats + (mags == 0 ? 0 : AMMO_GAP + 1 + AMMO_GAP + 14 + AMMO_ROW + 4 + 14 + mags * AMMO_ROW)
                + (guns == 0 ? 0 : AMMO_GAP + 1 + AMMO_GAP + 14 + guns * AMMO_ROW);
    }

    private int gridH() {
        return topPos + imageHeight - PAD - gridY();
    }

    private int rightX() {
        return gridX() + gridW() + PAD;
    }

    private int rightW() {
        return leftPos + imageWidth - PAD - rightX();
    }

    private int ingX() {
        return leftPos + imageWidth - PAD - ING_W;
    }

    private int btnX() {
        return leftPos + imageWidth - PAD - BTN_W;
    }

    private int btnY() {
        return topPos + imageHeight - PAD - BTN_H;
    }

    /** Окно осмотра 3D-модели: свободное место над характеристиками минус 100 px (80…200). */
    private int viewH() {
        // на 100 px ниже, чем всё свободное место: характеристики поднимаются вслед за окном
        int baseGridH = baseHeight - PAD - (HEADER_H + 6);
        return Mth.clamp(baseGridH - TITLE_H - 14 - STATS_H - 100, 80, 200);
    }

    /** где начинаются характеристики и ресурсы (от верха сетки) */
    private int detailsY() {
        return TITLE_H + 2 + viewH() + 12;
    }

    /** кнопка закрытия — правый верхний угол панели, над окном с моделью */
    private int closeX() {
        return leftPos + imageWidth - PAD - CLOSE_BTN;
    }

    private int closeY() {
        return topPos + (HEADER_H - CLOSE_BTN) / 2 + 1;
    }

    private void renderCloseButton(GuiGraphics g, int mouseX, int mouseY) {
        int x = closeX();
        int y = closeY();
        boolean hovered = mouseX >= x && mouseX < x + CLOSE_BTN && mouseY >= y && mouseY < y + CLOSE_BTN;
        float h = closeHover.update(hovered);
        GuiPanelRender.fill(g, x, y, CLOSE_BTN, CLOSE_BTN, ROUND, GuiPanelRender.mix(0x1EFFFFFF, 0xCCE5484D, h));
        GuiPanelRender.stroke(g, x, y, CLOSE_BTN, CLOSE_BTN, ROUND, 0.5f, GuiPanelRender.mix(0x30FFFFFF, 0xFFFF6B6B, h));
        // крестик — две повёрнутые полоски
        float cx = x + CLOSE_BTN / 2f;
        float cy = y + CLOSE_BTN / 2f;
        int color = GuiPanelRender.mix(0xFFD0D0D6, 0xFFFFFFFF, h);
        for (float angle : new float[]{45f, -45f}) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
            GuiPanelRender.fill(g, -5f, -0.6f, 10f, 1.2f, 0.6f, color);
            g.pose().popPose();
        }
    }

    private int rows() {
        return (recipes.size() + columns - 1) / columns;
    }

    private float maxScroll() {
        return Math.max(0, rows() * (CARD_H + CARD_GAP) - CARD_GAP - cardsH());
    }

    private float cardX(int i) {
        return gridX() + (i % columns) * (CARD_W + CARD_GAP);
    }

    private float cardY(int i) {
        return cardsY() + (i / columns) * (CARD_H + CARD_GAP) - scroll;
    }

    // ---------------------------------------------------------------- отрисовка

    @Override
    public void render(@NotNull GuiGraphics g, int rawMouseX, int rawMouseY, float partialTick) {
        layout();
        renderBackground(g);
        GuiPanelRender.captureFrame();
        int mouseX = (int) toUiX(rawMouseX);
        int mouseY = (int) toUiY(rawMouseY);
        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0);
        g.pose().scale(UI_SCALE, UI_SCALE, 1f);
        g.pose().translate(-width / 2f, -height / 2f, 0);
        try {
            renderContent(g, mouseX, mouseY);
        } finally {
            g.pose().popPose();
        }
        // тултипы — без масштаба, у настоящего курсора
        GunCraftingRecipe recipe = current();
        if (recipe != null) {
            renderHoverTooltips(g, recipe, mouseX, mouseY, rawMouseX, rawMouseY);
            if (!hoveredAmmoStack.isEmpty()) {
                g.renderTooltip(font, hoveredAmmoStack, rawMouseX, rawMouseY);
            }
            if (statBlock != null && hoveredStat >= 0 && hoveredStat < statBlock.rows().size()) {
                renderStatInfo(g, statBlock.rows().get(hoveredStat), rawMouseX, rawMouseY);
            }
        }
    }

    private double toUiX(double mouseX) {
        return (mouseX - width / 2.0) / UI_SCALE + width / 2.0;
    }

    private double toUiY(double mouseY) {
        return (mouseY - height / 2.0) / UI_SCALE + height / 2.0;
    }

    /** Ножницы задаются в координатах экрана, а рисуем мы в уменьшенных — пересчёт. */
    private void scissor(GuiGraphics g, float x0, float y0, float x1, float y1) {
        float cx = width / 2f;
        float cy = height / 2f;
        g.enableScissor((int) Math.floor((x0 - cx) * UI_SCALE + cx), (int) Math.floor((y0 - cy) * UI_SCALE + cy),
                (int) Math.ceil((x1 - cx) * UI_SCALE + cx), (int) Math.ceil((y1 - cy) * UI_SCALE + cy));
    }

    private void renderContent(GuiGraphics g, int mouseX, int mouseY) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        boolean creative = Minecraft.getInstance().player.isCreative();

        GuiPanelRender.panel(g, leftPos, topPos, imageWidth, imageHeight, round(imageWidth, imageHeight));
        GuiPanelRender.fill(g, leftPos + PAD, topPos + 9, 2.5f, 12, 1, GuiPanelRender.ACCENT);
        text(g, title.getString().toUpperCase(),
                leftPos + PAD + 8, topPos + 19, 11f, TEXT, true);
        String count = Component.translatable("gui.exe_tacticals.workbench.count", recipes.size()).getString();
        text(g, count, closeX() - 8 - width(count, 8.5f, false), topPos + 18, 8.5f, TEXT_DIM, false);
        renderCloseButton(g, mouseX, mouseY);
        GuiPanelRender.fill(g, leftPos + PAD, topPos + HEADER_H, imageWidth - PAD * 2, 0.5f, 0, 0x22FFFFFF);
        // разделитель сетки и правой части
        GuiPanelRender.fill(g, rightX() - PAD / 2f, gridY(), 0.5f, gridH(), 0, 0x22FFFFFF);

        renderGrid(g, inventory, mouseX, mouseY, creative);
        GunCraftingRecipe recipe = current();
        if (recipe == null) {
            text(g, Component.translatable("gui.exe_tacticals.workbench.empty").getString(),
                    rightX() + 8, gridY() + 20, 9f, TEXT_DIM, false);
            return;
        }
        renderPreview(g, recipe, mouseX, mouseY);
        renderStats(g, recipe, mouseX, mouseY);
        renderCompatibleGuns(g, recipe, rightX() + 2, statsBottom, mouseX, mouseY);
        renderIngredients(g, recipe, inventory, creative, mouseX, mouseY);
        boolean occupied = menu.tableOccupied();
        renderButton(g, !occupied && (creative || recipe.canCraft(inventory)),
                occupied ? "gui.exe_tacticals.workbench.occupied" : null, mouseX, mouseY);
    }

    private void renderGrid(GuiGraphics g, Inventory inventory, int mouseX, int mouseY, boolean creative) {
        hoveredCard = -1;
        renderTabs(g, mouseX, mouseY);
        int x = gridX();
        int y = cardsY();
        scissor(g, x - 2, y - 2, x + gridW() + 2, y + cardsH());
        for (int i = 0; i < recipes.size(); i++) {
            float cx = cardX(i);
            float cy = cardY(i);
            if (cy + CARD_H < y || cy > y + cardsH()) {
                continue;
            }
            GunCraftingRecipe r = recipes.get(i);
            boolean hovered = mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H
                    && mouseY >= y && mouseY < y + cardsH();
            if (hovered) {
                hoveredCard = i;
            }
            boolean isSelected = i == selected;
            float hover = cardHover[Math.min(i, cardHover.length - 1)].update(hovered);
            // ховер: карточка чуть увеличивается от центра
            g.pose().pushPose();
            if (hover > 0.001f) {
                float sc = 1f + 0.05f * hover;
                g.pose().translate(cx + CARD_W / 2f, cy + CARD_H / 2f, 0);
                g.pose().scale(sc, sc, 1f);
                g.pose().translate(-(cx + CARD_W / 2f), -(cy + CARD_H / 2f), 0);
            }
            boolean craftable = creative || r.canCraft(inventory);

            if (isSelected) {
                GuiPanelRender.shadow(g, cx, cy, CARD_W, CARD_H, round(CARD_W, CARD_H), 5f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.3f));
            }
            int top = isSelected ? GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.26f) : GuiPanelRender.mix(0x1CFFFFFF, 0x30FFFFFF, hover);
            int bottom = isSelected ? GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.13f) : GuiPanelRender.mix(0x0CFFFFFF, 0x1CFFFFFF, hover);
            GuiPanelRender.gradient(g, cx, cy, CARD_W, CARD_H, round(CARD_W, CARD_H), top, bottom);
            GuiPanelRender.stroke(g, cx, cy, CARD_W, CARD_H, round(CARD_W, CARD_H), isSelected ? 1f : 0.5f,
                    isSelected ? GuiPanelRender.ACCENT : GuiPanelRender.mix(0x22FFFFFF, 0x55FFFFFF, hover));

            // иконка чуть крупнее обычной
            g.pose().pushPose();
            g.pose().translate(cx + 5, cy + (CARD_H - 20) / 2f, 0);
            g.pose().scale(1.25f, 1.25f, 1f);
            g.renderItem(r.result(), 0, 0);
            g.pose().popPose();

            String name = ellipsize(r.result().getHoverName().getString(), CARD_W - 40, 9f, true);
            text(g, name, cx + 30, cy + 13, 9f, TEXT, true);
            // подпись категории не выходит за карточку (длинные названия — с «…»)
            String category = ellipsize(Component.translatable("gui.exe_tacticals.workbench.category." + r.category()).getString(),
                    CARD_W - 40, 7.5f, false);
            text(g, category, cx + 30, cy + 24, 7.5f, isSelected ? GuiPanelRender.ACCENT : TEXT_DIM, false);
            GuiPanelRender.circle(g, cx + CARD_W - 8, cy + 8, 2.5f, craftable ? OK : 0x50FFFFFF);
            g.pose().popPose();
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            float trackH = cardsH();
            float barH = Math.max(16, trackH * cardsH() / (cardsH() + maxScroll()));
            float barY = y + (trackH - barH) * (scroll / maxScroll());
            GuiPanelRender.fill(g, x + gridW() + 2, barY, 2, barH, 1, 0x60FFFFFF);
        }
    }

    private void renderPreview(GuiGraphics g, GunCraftingRecipe recipe, int mouseX, int mouseY) {
        int x = rightX();
        int y = gridY();
        int w = rightW();
        // название и категория
        text(g, recipe.result().getHoverName().getString(), x + 2, y + 15, 15f, TEXT, true);
        String category = Component.translatable("gui.exe_tacticals.workbench.category." + recipe.category()).getString();
        text(g, category, x + 2, y + 27, 9f, GuiPanelRender.ACCENT, false);

        // окно осмотра
        int vy = y + TITLE_H + 2;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= vy && mouseY < vy + viewH();
        GuiPanelRender.gradient(g, x, vy, w, viewH(), round(w, viewH()), 0x30FFFFFF, 0x08FFFFFF);
        GuiPanelRender.stroke(g, x, vy, w, viewH(), round(w, viewH()), 0.5f, hovered || dragging ? GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.31f) : 0x22FFFFFF);
        // «пол» под моделью
        GuiPanelRender.shadow(g, x + w * 0.2f, vy + viewH() - 22, w * 0.6f, 6, 3f, 10f, 0x40000000);

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (!dragging && now - lastInteraction > 2500) {
            viewYaw += dt * 20f; // медленно крутится, если не трогать
        }
        renderGunModel(g, preview(recipe), x, vy, w, viewH());

        renderZoomButtons(g, mouseX, mouseY);
        String hint = Component.translatable("gui.exe_tacticals.workbench.inspect_hint").getString();
        text(g, hint, x + 8, vy + viewH() - 7, 7.5f, 0x90A0A0AA, false);
    }

    /** левая верхняя точка кнопки «+» (под ней «−») — правый верхний угол окна осмотра */
    private int zoomBtnX() {
        return rightX() + rightW() - ZOOM_BTN - 6;
    }

    private int zoomInY() {
        return gridY() + TITLE_H + 2 + 6;
    }

    private int zoomOutY() {
        return zoomInY() + ZOOM_BTN + 3;
    }

    private static boolean inside(double mx, double my, int x, int y, int size) {
        return mx >= x && mx < x + size && my >= y && my < y + size;
    }

    private void renderZoomButtons(GuiGraphics g, int mouseX, int mouseY) {
        int x = zoomBtnX();
        zoomButton(g, x, zoomInY(), "+", zoomInHover.update(inside(mouseX, mouseY, x, zoomInY(), ZOOM_BTN)), viewZoom < ZOOM_MAX);
        zoomButton(g, x, zoomOutY(), "−", zoomOutHover.update(inside(mouseX, mouseY, x, zoomOutY(), ZOOM_BTN)), viewZoom > ZOOM_MIN);
    }

    /** прозрачный квадратик со значком */
    private void zoomButton(GuiGraphics g, int x, int y, String label, float hover, boolean enabled) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400); // поверх модели
        GuiPanelRender.fill(g, x, y, ZOOM_BTN, ZOOM_BTN, round(ZOOM_BTN, ZOOM_BTN), GuiPanelRender.mix(0x26FFFFFF, 0x50FFFFFF, hover));
        GuiPanelRender.stroke(g, x, y, ZOOM_BTN, ZOOM_BTN, round(ZOOM_BTN, ZOOM_BTN), 0.5f,
                GuiPanelRender.mix(0x30FFFFFF, GuiPanelRender.ACCENT, hover));
        float lw = width(label, 11f, true);
        text(g, label, x + (ZOOM_BTN - lw) / 2f, y + 11f, 11f, enabled ? TEXT : 0x60FFFFFF, true);
        g.pose().popPose();
    }

    private void zoom(int direction) {
        float next = Mth.clamp(viewZoom + direction * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
        if (next != viewZoom) {
            viewZoom = next;
            lastInteraction = System.currentTimeMillis();
            click(direction > 0 ? 1.1f : 0.9f);
        }
    }

    /** 3D-модель оружия (как в рамке, {@code FIXED}) в прямоугольнике окна осмотра, с поворотом мышью. */
    private void renderGunModel(GuiGraphics g, ItemStack stack, int x, int y, int w, int h) {
        if (!(stack.getItem() instanceof com.exetacticals.items.GunItem gunItem)) {
            renderItemModel(g, stack, x, y, w, h);
            return;
        }
        g.flush();
        scissor(g, x + 1, y + 1, x + w - 1, y + h - 1);
        com.mojang.blaze3d.vertex.PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + w / 2f, y + h / 2f + 4 - 30, 220); // модель выше центра окна на 30 px
        float size = Math.min(w * 0.7f, h * 1.35f) * viewZoom;
        pose.scale(size, -size, size);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(viewPitch));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(viewYaw));
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        try {
            com.exetacticals.Client.getGunRenderer().renderOther(null, stack, gunItem.getGun(),
                    net.minecraft.world.item.ItemDisplayContext.FIXED, pose, g.bufferSource(),
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            g.flush();
        } catch (RuntimeException e) {
            com.exetacticals.GCR.LOGGER.debug("workbench preview failed", e);
        } finally {
            pose.popPose();
            g.disableScissor();
        }
    }

    /**
     * Не оружие (магазин, обвес, патрон): обычная модель предмета ({@code FIXED}; у обвесов и магазинов — 3D через
     * {@code ModuleItemRenderer}), с тем же поворотом мышью и зумом.
     */
    private void renderItemModel(GuiGraphics g, ItemStack stack, int x, int y, int w, int h) {
        if (stack.isEmpty()) {
            return;
        }
        g.flush();
        scissor(g, x + 1, y + 1, x + w - 1, y + h - 1);
        com.mojang.blaze3d.vertex.PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + w / 2f, y + h / 2f - 4, 220);
        boolean module = stack.getItem() instanceof ModuleItem<?>;
        float size = Math.min(w * 0.55f, h * 0.75f) * viewZoom * (module ? 1.6f : 1f);
        pose.scale(size, -size, size);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(viewPitch));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(viewYaw));
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        try {
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.FIXED,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, pose, g.bufferSource(),
                    Minecraft.getInstance().level, 0);
            g.flush();
        } catch (RuntimeException e) {
            com.exetacticals.GCR.LOGGER.debug("workbench item preview failed", e);
        } finally {
            pose.popPose();
            g.disableScissor();
        }
    }

    /** где кончаются характеристики (или где они были бы) — под ними блок «Подходит для» */
    private int statsBottom;

    private void renderStats(GuiGraphics g, GunCraftingRecipe recipe, int mouseX, int mouseY) {
        statsBottom = gridY() + detailsY();
        statBlock = null;
        hoveredStat = -1;
        hoveredAmmoStack = ItemStack.EMPTY;
        if (!(recipe.result().getItem() instanceof ModuleItem<?> item)) {
            return;
        }
        List<StatRow> rows = new ArrayList<>();
        try {
            item.getModule().appendStatRows(preview(recipe), rows);
        } catch (RuntimeException ignored) {
            return;
        }
        if (rows.isEmpty()) {
            return;
        }
        int y = gridY() + detailsY();
        text(g, Component.translatable("gui.exe_tacticals.workbench.stats").getString(), rightX() + 2, y + 7, 9f, TEXT_DIM, false);
        statBlock = new ClientStatTooltip(new StatTooltip(rows));
        statX = rightX() + 2;
        statY = y + 11;
        hoveredStat = statBlock.rowAt(font, statX, statY, mouseX, mouseY);
        float[] hover = new float[rows.size()];
        for (int i = 0; i < hover.length && i < statHover.length; i++) {
            hover[i] = statHover[i].update(i == hoveredStat);
        }
        ClientStatTooltip.barRadius = ROUND;
        try {
            statBlock.renderRows(font, statX, statY, g, hover);
        } finally {
            ClientStatTooltip.barRadius = -1f;
        }
        statsBottom = statY + statBlock.getHeight() - 2;
        renderAmmoInfo(g, recipe, statX, statsBottom, mouseX, mouseY);
    }

    private static final int AMMO_ROW = 16;
    private static final int AMMO_GAP = 6;
    private final GuiPanelRender.Fade[] ammoHover = new GuiPanelRender.Fade[24];
    /** что под курсором в блоке патрона и магазинов — для тултипа */
    private ItemStack hoveredAmmoStack = ItemStack.EMPTY;

    {
        for (int i = 0; i < ammoHover.length; i++) {
            ammoHover[i] = new GuiPanelRender.Fade(16f);
        }
    }

    private final java.util.Map<GunCraftingRecipe, List<ItemStack>> compatibleGunsCache = new java.util.IdentityHashMap<>();

    /**
     * Не оружие (магазин, обвес, патрон): к какому оружию подходит. Магазин — {@code MagazineItem.compatibleGuns},
     * обвес — слоты в дереве оружия ({@code AttachmentCompatibilityTooltip}), патрон — оружие магазинов под этот патрон.
     */
    private List<ItemStack> compatibleGuns(GunCraftingRecipe recipe) {
        return compatibleGunsCache.computeIfAbsent(recipe, r -> {
            net.minecraft.world.item.Item item = r.result().getItem();
            java.util.Collection<String> ids;
            if (item instanceof com.exetacticals.items.GunItem) {
                return List.of();
            } else if (item instanceof com.exetacticals.items.MagazineItem mag) {
                ids = mag.compatibleGuns();
            } else if (item instanceof ModuleItem<?> module) {
                try {
                    ids = com.exetacticals.items.AttachmentCompatibilityTooltip.compatibleGuns(module.getModule());
                } catch (RuntimeException e) {
                    ids = List.of();
                }
            } else {
                java.util.Set<String> set = new java.util.LinkedHashSet<>();
                for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> entry : com.exetacticals.GCR.ITEMS.getEntries()) {
                    if (entry.get() instanceof com.exetacticals.items.MagazineItem mag && mag.cartridges().contains(item)) {
                        set.addAll(mag.compatibleGuns());
                    }
                }
                ids = set;
            }
            List<ItemStack> guns = new ArrayList<>();
            java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
            for (String id : ids) {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
                net.minecraft.world.item.Item gun = rl == null ? null : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
                if (gun != null && gun != net.minecraft.world.item.Items.AIR && seen.add(gun)) {
                    guns.add(new ItemStack(gun));
                }
            }
            return List.copyOf(guns);
        });
    }

    /** Под характеристиками: разделитель, «Подходит для:» и столбик оружия (иконка + название, тултип при наведении). */
    private void renderCompatibleGuns(GuiGraphics g, GunCraftingRecipe recipe, int x, int y, int mouseX, int mouseY) {
        List<ItemStack> guns = compatibleGuns(recipe);
        if (guns.isEmpty()) {
            return;
        }
        int right = ingX() - 14;
        GuiPanelRender.fill(g, x, y + AMMO_GAP, right - x, 0.5f, 0, 0x22FFFFFF);
        int rowY = y + AMMO_GAP * 2 + 1;
        text(g, Component.translatable("gui.exe_tacticals.workbench.fits").getString(), x, rowY + 10, 8.5f, TEXT_DIM, false);
        rowY += 14;
        int index = 0;
        for (ItemStack gun : guns) {
            ammoRow(g, gun, x, rowY, right, index++, mouseX, mouseY, TEXT);
            rowY += AMMO_ROW;
        }
    }

    private List<ItemStack> compatibleMagazines(GunCraftingRecipe recipe) {
        List<ItemStack> mags = new ArrayList<>();
        net.minecraft.resources.ResourceLocation gunId =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(recipe.result().getItem());
        if (gunId == null) {
            return mags;
        }
        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> entry : com.exetacticals.GCR.ITEMS.getEntries()) {
            if (entry.get() instanceof com.exetacticals.items.MagazineItem mag && mag.compatibleGuns().contains(gunId.toString())) {
                mags.add(new ItemStack(mag));
            }
        }
        return mags;
    }

    /**
     * Под полосками: разделитель, затем «Патрон» и «Магазины» — столбиком, каждая строка с иконкой и названием,
     * при наведении увеличивается и показывает тултип предмета.
     */
    private void renderAmmoInfo(GuiGraphics g, GunCraftingRecipe recipe, int x, int y, int mouseX, int mouseY) {
        hoveredAmmoStack = ItemStack.EMPTY;
        List<ItemStack> mags = compatibleMagazines(recipe);
        if (mags.isEmpty()) {
            return;
        }
        java.util.Set<net.minecraft.world.item.Item> cartridges = new java.util.LinkedHashSet<>();
        for (ItemStack mag : mags) {
            cartridges.addAll(((com.exetacticals.items.MagazineItem) mag.getItem()).cartridges());
        }
        int right = ingX() - 14;
        int w = right - x;
        // разделитель между характеристиками и патроном/магазинами
        GuiPanelRender.fill(g, x, y + AMMO_GAP, w, 0.5f, 0, 0x22FFFFFF); // как вертикальный разделитель рядом
        int rowY = y + AMMO_GAP * 2 + 1;
        int index = 0;
        String ammoLabel = Component.translatable("gui.exe_tacticals.workbench.ammo").getString();
        float labelW = Math.max(width(ammoLabel, 8.5f, false),
                width(Component.translatable("gui.exe_tacticals.workbench.magazines").getString(), 8.5f, false));
        // «Патрон:» отдельной строкой, под ней — столбик патронов
        text(g, ammoLabel, x, rowY + 10, 8.5f, TEXT_DIM, false);
        rowY += 14;
        for (net.minecraft.world.item.Item cartridge : cartridges) {
            ammoRow(g, new ItemStack(cartridge), x, rowY, right, index++, mouseX, mouseY, GuiPanelRender.ACCENT);
            rowY += AMMO_ROW;
        }
        rowY += 4;
        // «Магазины:» отдельной строкой, под ней — столбик магазинов
        text(g, Component.translatable("gui.exe_tacticals.workbench.magazines").getString(), x, rowY + 10, 8.5f, TEXT_DIM, false);
        rowY += 14;
        for (ItemStack mag : mags) {
            ammoRow(g, mag, x, rowY, right, index++, mouseX, mouseY, TEXT);
            rowY += AMMO_ROW;
        }
    }

    /** строка-плашка «иконка + название» во всю ширину колонки, с ховером */
    private void ammoRow(GuiGraphics g, ItemStack stack, float x, int y, int right, int index,
                         int mouseX, int mouseY, int textColor) {
        float w = right - x;
        int h = AMMO_ROW - 2;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        if (hovered) {
            hoveredAmmoStack = stack;
        }
        float t = index < ammoHover.length ? ammoHover[index].update(hovered) : 0f;
        g.pose().pushPose();
        if (t > 0.001f) {
            float sc = 1f + 0.05f * t;
            g.pose().translate(x + w / 2f, y + h / 2f, 0);
            g.pose().scale(sc, sc, 1f);
            g.pose().translate(-(x + w / 2f), -(y + h / 2f), 0);
        }
        GuiPanelRender.fill(g, x, y, w, h, ROUND, GuiPanelRender.mix(0x16FFFFFF, 0x34FFFFFF, t));
        if (t > 0.001f) {
            GuiPanelRender.stroke(g, x, y, w, h, ROUND, 0.5f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, t));
        }
        g.pose().pushPose();
        g.pose().translate(x + 2, y + 1, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        text(g, ellipsize(stack.getHoverName().getString(), (int) w - 22, 8.5f, false), x + 17, y + 10, 8.5f, textColor, false);
        g.pose().popPose();
    }

    private float chipWidth(ItemStack stack) {
        return 12 + 3 + width(stack.getHoverName().getString(), 8f, false) + 6;
    }

    /** «Плашка» с маленькой иконкой и названием; возвращает правый край. */
    private float chip(GuiGraphics g, ItemStack stack, float x, int y, int textColor) {
        float w = chipWidth(stack);
        GuiPanelRender.fill(g, x, y, w, 13, ROUND, 0x1CFFFFFF);
        g.pose().pushPose();
        g.pose().translate(x + 2, y + 0.5f, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        text(g, stack.getHoverName().getString(), x + 15, y + 9.5f, 8f, textColor, false);
        return x + w;
    }

    /** Тултип-пояснение к характеристике: название акцентом и описание, перенос по ширине. */
    private void renderStatInfo(GuiGraphics g, StatRow row, int rawX, int rawY) {
        String title = row.name().getString();
        String desc = Component.translatableWithFallback("stat.exe_tacticals.desc." + row.key(),
                Component.translatable("stat.exe_tacticals.desc.unknown").getString()).getString();
        renderInfoBox(g, title, desc, null, 0, rawX, rawY);
    }

    /** Тултип в стиле меню: заголовок, описание с переносом и (необязательно) цветная строка статуса снизу. */
    private void renderInfoBox(GuiGraphics g, String title, String desc, String status, int statusColor, int rawX, int rawY) {
        float size = 8.5f;
        int maxW = 170;
        List<String> lines = wrap(desc, maxW, size);
        float w = width(title, 10f, true);
        for (String line : lines) {
            w = Math.max(w, width(line, size, false));
        }
        if (status != null) {
            w = Math.max(w, width(status, size, true));
        }
        int boxW = (int) Math.ceil(w) + 16;
        int boxH = 22 + lines.size() * 11 + 4 + (status != null ? 12 : 0);
        int x = rawX + 12;
        int y = rawY - 6;
        if (x + boxW > width - 4) {
            x = rawX - 12 - boxW;
        }
        y = Math.max(4, Math.min(y, height - boxH - 4));
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        GuiPanelRender.shadow(g, x, y + 2, boxW, boxH, ROUND, 8f, 0x90000000);
        GuiPanelRender.glass(g, x, y, boxW, boxH, ROUND, GuiPanelRender.PANEL_BLUR, 0xD8111216);
        GuiPanelRender.stroke(g, x, y, boxW, boxH, ROUND, 0.5f, 0x30FFFFFF);
        GuiPanelRender.fill(g, x + 8, y + 17, width(title, 10f, true), 1f, 0.5f, GuiPanelRender.ACCENT);
        text(g, title, x + 8, y + 13, 10f, TEXT, true);
        for (int i = 0; i < lines.size(); i++) {
            text(g, lines.get(i), x + 8, y + 30 + i * 11, size, 0xFFC9CAD2, false);
        }
        if (status != null) {
            text(g, status, x + 8, y + 30 + lines.size() * 11 + 2, size, statusColor, true);
        }
        g.pose().popPose();
    }

    private List<String> wrap(String text, int maxWidth, float size) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && width(candidate, size, false) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private void renderIngredients(GuiGraphics g, GunCraftingRecipe recipe, Inventory inventory, boolean creative,
                                   int mouseX, int mouseY) {
        int x = ingX();
        int y = gridY() + detailsY();
        GuiPanelRender.fill(g, x - 8, y - 2, 0.5f, btnY() - y - 6, 0, 0x22FFFFFF);
        text(g, Component.translatable("gui.exe_tacticals.workbench.required").getString(), x, y + 7, 9f, TEXT_DIM, false);
        int rowY = y + 13;
        hoveredIng = -1;
        int index = 0;
        for (GunCraftingRecipe.Entry entry : recipe.entries()) {
            ItemStack shown = displayStack(entry);
            int have = GunCraftingRecipe.count(inventory, entry.ingredient());
            boolean enough = creative || have >= entry.count();
            boolean hovered = mouseX >= x && mouseX < x + ING_W && mouseY >= rowY && mouseY < rowY + ING_ROW - 2;
            if (hovered) {
                hoveredIng = index;
            }
            float h = index < ingHover.length ? ingHover[index].update(hovered) : 0f;
            g.pose().pushPose();
            if (h > 0.001f) {
                float sc = 1f + 0.06f * h;
                g.pose().translate(x + ING_W / 2f, rowY + (ING_ROW - 2) / 2f, 0);
                g.pose().scale(sc, sc, 1f);
                g.pose().translate(-(x + ING_W / 2f), -(rowY + (ING_ROW - 2) / 2f), 0);
            }
            int bg = enough ? 0x1A6FDB8F : 0x1AFF6464;
            GuiPanelRender.fill(g, x, rowY, ING_W, ING_ROW - 2, round(ING_W, ING_ROW - 2),
                    GuiPanelRender.mix(bg, enough ? 0x406FDB8F : 0x40FF6464, h));
            g.renderItem(shown, x + 1, rowY);
            String amount = have + " / " + entry.count();
            float aw = width(amount, 8.5f, true);
            text(g, ellipsize(shown.getHoverName().getString(), ING_W - 26 - (int) aw - 6, 8f, false),
                    x + 20, rowY + 11, 8f, TEXT_DIM, false);
            text(g, amount, x + ING_W - 4 - aw, rowY + 11, 8.5f, enough ? OK : BAD, true);
            g.pose().popPose();
            rowY += ING_ROW;
            index++;
        }
    }

    /** {@code disabledKey} — надпись неактивной кнопки вместо «Не хватает» ({@code null} — по умолчанию). */
    private void renderButton(GuiGraphics g, boolean enabled, String disabledKey, int mouseX, int mouseY) {
        int x = btnX();
        int y = btnY();
        boolean hovered = mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H;
        float hover = buttonHover.update(enabled && hovered);
        if (enabled) {
            GuiPanelRender.shadow(g, x, y, BTN_W, BTN_H, round(BTN_W, BTN_H), 5f, GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.25f + 0.3f * hover));
            GuiPanelRender.gradient(g, x, y, BTN_W, BTN_H, round(BTN_W, BTN_H),
                    GuiPanelRender.mix(GuiPanelRender.mix(GuiPanelRender.ACCENT, 0xFFFFFFFF, 0.18f),
                            GuiPanelRender.mix(GuiPanelRender.ACCENT, 0xFFFFFFFF, 0.35f), hover),
                    GuiPanelRender.mix(GuiPanelRender.mix(GuiPanelRender.ACCENT, 0xFF000000, 0.15f), GuiPanelRender.ACCENT, hover));
        } else {
            GuiPanelRender.fill(g, x, y, BTN_W, BTN_H, round(BTN_W, BTN_H), 0x40FFFFFF);
        }
        String label = Component.translatable(enabled ? "gui.exe_tacticals.workbench.craft"
                : disabledKey != null ? disabledKey : "gui.exe_tacticals.workbench.missing").getString();
        float lw = width(label, 9.5f, true);
        text(g, label, x + (BTN_W - lw) / 2f, y + 14.5f, 9.5f, enabled ? 0xFFFFFFFF : 0xFF8A8A94, true);
    }

    private void renderHoverTooltips(GuiGraphics g, GunCraftingRecipe recipe, int mouseX, int mouseY, int rawX, int rawY) {
        if (hoveredIng >= 0 && hoveredIng < recipe.entries().size()) {
            GunCraftingRecipe.Entry entry = recipe.entries().get(hoveredIng);
            Inventory inventory = Minecraft.getInstance().player.getInventory();
            boolean creative = Minecraft.getInstance().player.isCreative();
            int have = GunCraftingRecipe.count(inventory, entry.ingredient());
            int need = entry.count();
            String desc = Component.translatable("gui.exe_tacticals.workbench.ing_info", have, need).getString();
            String status = creative ? Component.translatable("gui.exe_tacticals.workbench.ing_creative").getString()
                    : have >= need ? Component.translatable("gui.exe_tacticals.workbench.ing_enough").getString()
                    : Component.translatable("gui.exe_tacticals.workbench.ing_missing", need - have).getString();
            renderInfoBox(g, displayStack(entry).getHoverName().getString(), desc, status,
                    creative || have >= need ? OK : BAD, rawX, rawY);
        }

    }

    private ItemStack displayStack(GunCraftingRecipe.Entry entry) {
        ItemStack[] items = entry.ingredient().getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        return items[(int) ((System.currentTimeMillis() / 1000L) % items.length)];
    }

    // ---------------------------------------------------------------- текст

    private float width(String s, float size, boolean medium) {
        MsdfFont f = medium ? MsdfFont.medium() : MsdfFont.regular();
        return f == null ? font.width(s) : f.width(s, size);
    }

    private void text(GuiGraphics g, String s, float x, float baseline, float size, int color, boolean medium) {
        MsdfFont f = medium ? MsdfFont.medium() : MsdfFont.regular();
        if (f == null) {
            g.drawString(font, s, (int) x, (int) (baseline - 7), color, false);
        } else {
            f.draw(g, FormattedCharSequence.forward(s, Style.EMPTY), x, baseline, size, color, true);
        }
    }

    private String ellipsize(String s, int maxWidth, float size, boolean medium) {
        if (width(s, size, medium) <= maxWidth) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && width(cut + "…", size, medium) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
    }

    // ---------------------------------------------------------------- ввод

    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        layout();
        double mouseX = toUiX(rawX);
        double mouseY = toUiY(rawY);
        if (button == 0) {
            if (mouseX >= closeX() && mouseX < closeX() + CLOSE_BTN && mouseY >= closeY() && mouseY < closeY() + CLOSE_BTN) {
                click(0.9f);
                onClose();
                return true;
            }
            List<float[]> tabs = tabRects();
            for (int i = 0; i < tabs.size(); i++) {
                float[] r = tabs.get(i);
                if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + TAB_H) {
                    String c = i == 0 ? null : categories.get(i - 1);
                    if (!java.util.Objects.equals(c, category)) {
                        category = c;
                        applyCategory();
                        selected = 0;
                        scroll = 0;
                        viewYaw = -30f;
                        viewPitch = 12f;
                        click(1.0f);
                    }
                    return true;
                }
            }
            int y = cardsY();
            if (mouseX >= gridX() && mouseX < gridX() + gridW() && mouseY >= y && mouseY < y + cardsH()) {
                for (int i = 0; i < recipes.size(); i++) {
                    float cx = cardX(i);
                    float cy = cardY(i);
                    if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                        if (i != selected) {
                            selected = i;
                            viewYaw = -30f;
                            viewPitch = 12f;
                            click(1.0f);
                        }
                        break;
                    }
                }
                return true;
            }
            if (inside(mouseX, mouseY, zoomBtnX(), zoomInY(), ZOOM_BTN)) {
                zoom(1);
                return true;
            }
            if (inside(mouseX, mouseY, zoomBtnX(), zoomOutY(), ZOOM_BTN)) {
                zoom(-1);
                return true;
            }
            int vy = gridY() + TITLE_H + 2;
            if (mouseX >= rightX() && mouseX < rightX() + rightW() && mouseY >= vy && mouseY < vy + viewH()) {
                dragging = true;
                lastInteraction = System.currentTimeMillis();
                return true;
            }
            GunCraftingRecipe recipe = current();
            if (recipe != null && mouseX >= btnX() && mouseX < btnX() + BTN_W && mouseY >= btnY() && mouseY < btnY() + BTN_H) {
                boolean creative = Minecraft.getInstance().player.isCreative();
                if (!menu.tableOccupied() && (creative || recipe.canCraft(Minecraft.getInstance().player.getInventory()))) {
                    GCRNetwork.sendToServer(new CraftGunPacket(recipe.getId()));
                    click(1.2f);
                }
                return true;
            }
        }
        return super.mouseClicked(rawX, rawY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            viewYaw += (float) dragX / UI_SCALE * 1.2f;
            viewPitch = Mth.clamp(viewPitch + (float) dragY / UI_SCALE, -80f, 80f);
            lastInteraction = System.currentTimeMillis();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            lastInteraction = System.currentTimeMillis();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double delta) {
        double mouseX = toUiX(rawX);
        double mouseY = toUiY(rawY);
        int vy = gridY() + TITLE_H + 2;
        if (mouseX >= rightX() && mouseX < rightX() + rightW() && mouseY >= vy && mouseY < vy + viewH()) {
            if (delta != 0) {
                viewZoom = Mth.clamp(viewZoom + (delta > 0 ? ZOOM_STEP : -ZOOM_STEP), ZOOM_MIN, ZOOM_MAX);
                lastInteraction = System.currentTimeMillis();
            }
            return true;
        }
        scroll = Mth.clamp(scroll - (float) delta * (CARD_H + CARD_GAP), 0, maxScroll());
        return true;
    }

    private static void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }
}
