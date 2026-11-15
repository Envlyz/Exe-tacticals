package com.exetacticals.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.exetacticals.client.PhysicalMagazineInput;
import com.exetacticals.client.KeyBinds;
import com.exetacticals.items.MagazineItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MagazineWheelScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double CANCEL_RADIUS = 43.0;
    private static final double PETAL_INNER_RADIUS = 50.0;
    private static final int SECTOR = 0xEB000000;
    private static final int SELECT_INNER = 0xEB000101;
    private static final int SELECT_OUTER = 0xFF02D3AC;
    private static final int LABEL_SELECTED = 0xFFFFFF00;
    private int soundHovered = -2;
    private static final float ITEM_SCALE = 2.0f;
    private static final float NAME_SIZE = 8.5f;
    private static final float SMALL_SIZE = 7.5f;
    private static final int NAME_LINE = 10;
    private static final int SMALL_LINE = 9;

    private static com.exetacticals.client.tooltip.MsdfFont msdf(boolean medium) {
        return medium ? com.exetacticals.client.tooltip.MsdfFont.medium() : com.exetacticals.client.tooltip.MsdfFont.regular();
    }

    private float textWidth(String s, float size, boolean medium) {
        var f = msdf(medium);
        return f == null ? font.width(s) * size / 9f : f.width(s, size);
    }

    private String ellipsize(String s, float max, float size, boolean medium) {
        if (textWidth(s, size, medium) <= max) return s;
        String cut = s;
        while (!cut.isEmpty() && textWidth(cut + "…", size, medium) > max) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + "…";
    }

    private static final int AMMO_BALL_COLOR = 0xFFF0C94A;
    private static final int AMMO_TRACER_COLOR = 0xFFFF5A46;

    private void ammoLine(GuiGraphics gfx, int ball, int tracers, float cx, float top, float max) {
        String ballName = Component.translatable("exe_tacticals.wheel.ammo_ball").getString();
        String tracerName = Component.translatable("exe_tacticals.wheel.ammo_tracer").getString();
        List<String> parts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            parts.clear();
            colors.clear();
            boolean full = pass == 0;
            if (ball > 0) {
                parts.add(full ? ballName + " " + ball : String.valueOf(ball));
                colors.add(AMMO_BALL_COLOR);
            }
            if (tracers > 0) {
                if (!parts.isEmpty()) {
                    parts.add(" · ");
                    colors.add(0xFF8A9396);
                }
                parts.add(full ? tracerName + " " + tracers : String.valueOf(tracers));
                colors.add(AMMO_TRACER_COLOR);
            }
            if (textWidth(String.join("", parts), SMALL_SIZE, false) <= max) break;
        }
        float total = textWidth(String.join("", parts), SMALL_SIZE, false);
        float x = cx - total / 2f;
        for (int p = 0; p < parts.size(); p++) {
            float w = textWidth(parts.get(p), SMALL_SIZE, false);
            label(gfx, parts.get(p), x + w / 2f, top, SMALL_SIZE, colors.get(p), false);
            x += w;
        }
    }

    private void label(GuiGraphics gfx, String text, float cx, float top, float size, int color, boolean medium) {
        var f = msdf(medium);
        if (f == null) {
            float k = size / 9f;
            gfx.pose().pushPose();
            gfx.pose().translate(cx, top, 0);
            gfx.pose().scale(k, k, 1f);
            gfx.drawCenteredString(font, text, 0, 0, color);
            gfx.pose().popPose();
            return;
        }
        float w = f.width(text, size);
        f.draw(gfx, net.minecraft.util.FormattedCharSequence.forward(text, net.minecraft.network.chat.Style.EMPTY),
                cx - w / 2f, top + f.ascent(size), size, color, true);
    }
    private static final float[] ACCENT_RGB = {0.008f, 0.827f, 0.675f};
    private final GuiPanelRender.Fade[] petalFade = new GuiPanelRender.Fade[PAGE_SIZE];
    private final GuiPanelRender.Fade centerFade = new GuiPanelRender.Fade(14f);
    private long openedAt = System.nanoTime();
    {
        for (int i = 0; i < petalFade.length; i++) petalFade[i] = new GuiPanelRender.Fade(14f);
    }

    private record GroupKey(String moduleType, String itemId, int rounds, int capacity,
                            String cartridges, String displayName) {}

    private record MagazineGroup(PhysicalMagazineInput.MagazineChoice selected, int quantity) {}

    private final List<MagazineGroup> groups;
    private final String gunIdentity;
    private final String preferredType;
    private int page;
    private int hovered = -1;
    private boolean cancelHover = true;
    private boolean completed;

    public MagazineWheelScreen(List<PhysicalMagazineInput.MagazineChoice> choices,
                                         String gunIdentity, String preferredType) {
        super(Component.translatable("exe_tacticals.wheel.title"));
        this.groups = groupChoices(choices, preferredType);
        this.gunIdentity = gunIdentity;
        this.preferredType = preferredType;
    }

    private static List<MagazineGroup> groupChoices(List<PhysicalMagazineInput.MagazineChoice> choices,
                                                     String preferredType) {
        Map<GroupKey, List<PhysicalMagazineInput.MagazineChoice>> groups = new LinkedHashMap<>();
        for (var choice : choices) {
            ItemStack stack = choice.snapshot();
            if (!(stack.getItem() instanceof MagazineItem item)) continue;
            var roundTypes = item.getRoundTypes(stack);
            ArrayList<String> cartridgeKeys = new ArrayList<>(roundTypes.getAllKeys());
            cartridgeKeys.sort(String::compareTo);
            StringBuilder composition = new StringBuilder();
            for (String key : cartridgeKeys) {
                composition.append(key).append('=').append(roundTypes.getInt(key)).append(';');
            }
            GroupKey key = new GroupKey(choice.magazineType(),
                    String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())),
                    choice.rounds(), choice.capacity(), composition.toString(),
                    stack.getHoverName().getString());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(choice);
        }

        List<Map.Entry<GroupKey, List<PhysicalMagazineInput.MagazineChoice>>> ordered =
                new ArrayList<>(groups.entrySet());
        ordered.sort(Comparator
                .comparing((Map.Entry<GroupKey, List<PhysicalMagazineInput.MagazineChoice>> e)
                        -> !e.getKey().moduleType().equals(preferredType))
                .thenComparing(e -> e.getKey().displayName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.getKey().itemId())
                .thenComparing(Comparator.comparingInt(
                        (Map.Entry<GroupKey, List<PhysicalMagazineInput.MagazineChoice>> e)
                                -> e.getKey().rounds()).reversed())
                .thenComparing(e -> e.getKey().cartridges()));

        ArrayList<MagazineGroup> result = new ArrayList<>();
        for (var entry : ordered) {
            int amount = 0;
            for (var choice : entry.getValue()) amount += Math.max(1, choice.snapshot().getCount());
            result.add(new MagazineGroup(entry.getValue().get(0), amount));
        }
        return List.copyOf(result);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void mirrorMovementKey(KeyMapping mapping, long window) {
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() >= 0) {
            mapping.setDown(InputConstants.isKeyDown(window, key.getValue()));
        }
    }

    private void clearMovementKeys() {
        if (minecraft == null) return;
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || !minecraft.isWindowActive()) {
            clearMovementKeys();
            return;
        }
        long window = minecraft.getWindow().getWindow();
        mirrorMovementKey(minecraft.options.keyUp, window);
        mirrorMovementKey(minecraft.options.keyDown, window);
        mirrorMovementKey(minecraft.options.keyLeft, window);
        mirrorMovementKey(minecraft.options.keyRight, window);
        mirrorMovementKey(minecraft.options.keyJump, window);
        mirrorMovementKey(minecraft.options.keyShift, window);
        mirrorMovementKey(minecraft.options.keySprint, window);
    }

    private int displayed() {
        return Math.min(PAGE_SIZE, Math.max(0, groups.size() - page * PAGE_SIZE));
    }

    private double outerRadius() {
        return Math.max(90.0, Math.min(166.0, Math.min(width * 0.35, (height - 90.0) * 0.46)));
    }

    private void updateHovered(double x, double y) {
        double dx = x - width / 2.0;
        double dy = y - height / 2.0;
        cancelHover = Math.hypot(dx, dy) <= CANCEL_RADIUS;
        if (cancelHover || displayed() == 0) {
            hovered = -1;
            return;
        }
        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += TWO_PI;
        int n = displayed();
        int index = (int) Math.floor((angle + Math.PI / n) * n / TWO_PI) % n;
        hovered = page * PAGE_SIZE + index;
    }

    private void playHoverSound() {
        int now = cancelHover ? -1 : hovered;
        if (now != soundHovered) {
            if (soundHovered != -2 && minecraft != null) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        com.exetacticals.sound.ModSounds.WHEEL_SELECT.get(), 1.0f, 0.5f));
            }
            soundHovered = now;
        }
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft != null) {
            org.lwjgl.glfw.GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),
                    org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN);
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        updateHovered(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        int pages = Math.max(1, (groups.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (pages <= 1) return true;
        page = Math.floorMod(page + (delta < 0 ? 1 : -1), pages);
        updateHovered(x, y);
        return true;
    }

    private void finish(boolean choose) {
        if (completed) return;
        completed = true;
        PhysicalMagazineInput.MagazineChoice selected = choose && hovered >= 0 && hovered < groups.size()
                ? groups.get(hovered).selected() : null;
        PhysicalMagazineInput.onWheelClosed(gunIdentity, selected);
        if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (KeyBinds.RELOAD.getKey().getType() == InputConstants.Type.KEYSYM
                && keyCode == KeyBinds.RELOAD.getKey().getValue()) {
            finish(true);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (KeyBinds.RELOAD.getKey().getType() == InputConstants.Type.MOUSE
                && button == KeyBinds.RELOAD.getKey().getValue()) {
            updateHovered(x, y);
            finish(true);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        finish(false);
    }

    @Override
    public void removed() {
        if (minecraft != null) {
            org.lwjgl.glfw.GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),
                    org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
        }
        clearMovementKeys();
        if (!completed) finish(false);
    }

    private static void vertex(BufferBuilder b, double x, double y, int color) {
        b.vertex(x, y, 0).color((color >>> 16) & 255, (color >>> 8) & 255,
                color & 255, (color >>> 24) & 255).endVertex();
    }

    private static void triangle(BufferBuilder b, double x1, double y1,
                                 double x2, double y2, double x3, double y3, int color) {
        vertex(b, x1, y1, color);
        vertex(b, x2, y2, color);
        vertex(b, x3, y3, color);
    }

    private static int lerpColor(int a, int b, double t) {
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 255, cb = (b >>> shift) & 255;
            out |= ((int) Math.round(ca + (cb - ca) * t) & 255) << shift;
        }
        return out;
    }

    private static void ringGradient(BufferBuilder b, double cx, double cy, double inner, double outer,
                                     double start, double stop, int innerColor, int outerColor) {
        int rings = 10;
        for (int k = 0; k < rings; k++) {
            double r0 = inner + (outer - inner) * k / rings;
            double r1 = inner + (outer - inner) * (k + 1) / rings;
            int c0 = lerpColor(innerColor, outerColor, Math.pow((double) k / rings, 2.0));
            int c1 = lerpColor(innerColor, outerColor, Math.pow((double) (k + 1) / rings, 2.0));
            int steps = Math.max(8, (int) Math.ceil((stop - start) / Math.toRadians(4)));
            for (int i = 0; i < steps; i++) {
                double a = start + (stop - start) * i / steps;
                double z = start + (stop - start) * (i + 1) / steps;
                double aiX = cx + Math.sin(a) * r0, aiY = cy - Math.cos(a) * r0;
                double biX = cx + Math.sin(z) * r0, biY = cy - Math.cos(z) * r0;
                double aoX = cx + Math.sin(a) * r1, aoY = cy - Math.cos(a) * r1;
                double boX = cx + Math.sin(z) * r1, boY = cy - Math.cos(z) * r1;
                vertex(b, aiX, aiY, c0); vertex(b, aoX, aoY, c1); vertex(b, biX, biY, c0);
                vertex(b, biX, biY, c0); vertex(b, aoX, aoY, c1); vertex(b, boX, boY, c1);
            }
        }
    }

    private static void ringSection(BufferBuilder b, double cx, double cy,
                                    double inner, double outer, double start, double stop, int color) {
        int steps = Math.max(8, (int) Math.ceil((stop - start) / Math.toRadians(4)));
        for (int i = 0; i < steps; i++) {
            double a = start + (stop - start) * i / steps;
            double z = start + (stop - start) * (i + 1) / steps;
            double aiX = cx + Math.sin(a) * inner, aiY = cy - Math.cos(a) * inner;
            double biX = cx + Math.sin(z) * inner, biY = cy - Math.cos(z) * inner;
            double aoX = cx + Math.sin(a) * outer, aoY = cy - Math.cos(a) * outer;
            double boX = cx + Math.sin(z) * outer, boY = cy - Math.cos(z) * outer;
            triangle(b, aiX, aiY, aoX, aoY, biX, biY, color);
            triangle(b, biX, biY, aoX, aoY, boX, boY, color);
        }
    }

    private void renderSectors(int centerX, int centerY, double outer) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int n = displayed();
        for (int i = 0; i < n; i++) {
            boolean selected = !cancelHover && hovered == page * PAGE_SIZE + i;
            double center = TWO_PI * i / n;
            double halfAngle = Math.PI / n;
            double gap = Math.min(Math.toRadians(1.5), halfAngle * 0.1);
            if (selected) {
                ringGradient(b, centerX, centerY, PETAL_INNER_RADIUS, outer,
                        center - halfAngle + gap, center + halfAngle - gap, SELECT_INNER, SELECT_OUTER);
            } else {
                ringSection(b, centerX, centerY, PETAL_INNER_RADIUS, outer,
                        center - halfAngle + gap, center + halfAngle - gap, SECTOR);
            }
        }
        if (cancelHover) {
            ringGradient(b, centerX, centerY, 0, CANCEL_RADIUS - 1, -Math.PI, Math.PI, SELECT_INNER, SELECT_OUTER);
        } else {
            ringSection(b, centerX, centerY, 0, CANCEL_RADIUS - 1, -Math.PI, Math.PI, SECTOR);
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.disableBlend();
    }

    private boolean renderWheelShader(GuiGraphics gfx, int cx, int cy, double outer) {
        ShaderInstance shader = com.exetacticals.client.render.Shaders.radialWheel;
        if (shader == null) {
            return false;
        }
        gfx.flush();
        GuiPanelRender.captureFrame();
        int n = displayed();
        float[] highlight = new float[PAGE_SIZE];
        float[] favorite = new float[PAGE_SIZE];
        for (int i = 0; i < PAGE_SIZE; i++) {
            boolean on = i < n && !cancelHover && hovered == page * PAGE_SIZE + i;
            highlight[i] = petalFade[i].update(on);
            favorite[i] = i < n && groups.get(page * PAGE_SIZE + i).selected().magazineType().equals(preferredType) ? 1f : 0f;
        }
        float center = centerFade.update(cancelHover);
        float open = Math.min(1f, (System.nanoTime() - openedAt) / 140_000_000f);
        float pad = 18f;
        float size = (float) (outer + pad) * 2f;
        float x = cx - size / 2f, y = cy - size / 2f;
        double guiScale = minecraft == null ? 2.0 : minecraft.getWindow().getGuiScale();

        shader.safeGetUniform("Size").set(size);
        shader.safeGetUniform("Radii").set((float) CANCEL_RADIUS - 1f, (float) PETAL_INNER_RADIUS, (float) outer, 7f);
        shader.safeGetUniform("Gap").set(4f);
        shader.safeGetUniform("Count").set((float) n);
        shader.safeGetUniform("Highlight").set(highlight);
        shader.safeGetUniform("Favorite").set(favorite);
        shader.safeGetUniform("CenterHighlight").set(center);
        shader.safeGetUniform("Accent").set(ACCENT_RGB[0], ACCENT_RGB[1], ACCENT_RGB[2]);
        shader.safeGetUniform("BlurRadius").set((float) (7.0 * guiScale));
        shader.safeGetUniform("Opacity").set(open);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, GuiPanelRender.frameCopyTexture());
        RenderSystem.setShader(() -> shader);
        Matrix4f m = gfx.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(m, x, y, 0).uv(0, 0).color(255, 255, 255, 255).endVertex();
        b.vertex(m, x, y + size, 0).uv(0, 1).color(255, 255, 255, 255).endVertex();
        b.vertex(m, x + size, y + size, 0).uv(1, 1).color(255, 255, 255, 255).endVertex();
        b.vertex(m, x + size, y, 0).uv(1, 0).color(255, 255, 255, 255).endVertex();
        BufferUploader.drawWithShader(b.end());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        return true;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        updateHovered(mouseX, mouseY);
        playHoverSound();

        int cx = width / 2, cy = height / 2;
        double r = outerRadius();
        if (!renderWheelShader(gfx, cx, cy, r)) {
            renderSectors(cx, cy, r);
        }
        label(gfx, title.getString().toUpperCase(), cx, (float) (cy - r - 26), 10f, 0xFFFFFFFF, true);

        int n = displayed();
        for (int i = 0; i < n; i++) {
            MagazineGroup group = groups.get(page * PAGE_SIZE + i);
            var choice = group.selected();
            double a = TWO_PI * i / n;
            double mid = (PETAL_INNER_RADIUS + r) / 2.0;
            int tx = cx + (int) Math.round(Math.sin(a) * mid);
            int ty = cy - (int) Math.round(Math.cos(a) * mid);

            ItemStack display = choice.snapshot();
            boolean selected = !cancelHover && hovered == page * PAGE_SIZE + i;
            int tracers = 0;
            if (display.getItem() instanceof MagazineItem magItem) {
                var types = magItem.getRoundTypes(display);
                for (String k : types.getAllKeys()) if (MagazineItem.isTracer(k)) tracers += Math.max(0, types.getInt(k));
            }
            tracers = Math.min(tracers, choice.rounds());
            int ball = choice.rounds() - tracers;
            boolean showAmmo = choice.rounds() > 0;
            int lines = (group.quantity() > 1 ? 3 : 2) + (showAmmo ? 1 : 0);

            double depth = r - PETAL_INNER_RADIUS - 8;
            double chord = n == 1 ? 2 * mid : 2 * (mid * 0.82 * Math.sin(Math.PI / n)) - 8;
            double sinA = Math.abs(Math.sin(a)), cosA = Math.abs(Math.cos(a));
            float itemScale = selected ? ITEM_SCALE * 1.12f : ITEM_SCALE;
            double textW = 0;
            double blockH = 0;
            for (int attempt = 0; attempt < 6; attempt++) {
                blockH = 16 * itemScale + 2 + NAME_LINE + (lines - 1) * SMALL_LINE;
                double byDepth = sinA < 1e-3 ? 1e4 : (depth - blockH * cosA) / sinA;
                double byChord = cosA < 1e-3 ? 1e4 : (chord - blockH * sinA) / cosA;
                textW = Math.min(Math.min(byDepth, byChord), 130);
                if (textW >= 40 || itemScale <= 1.01f) break;
                itemScale = Math.max(1f, itemScale - 0.25f);
            }
            float maxText = (float) Math.max(20, textW);

            int top = (int) Math.round(ty - blockH / 2.0);
            float itemSize = 16 * itemScale;
            gfx.pose().pushPose();
            gfx.pose().translate(tx, top + itemSize / 2f, 0);
            gfx.pose().scale(itemScale, itemScale, itemScale);
            gfx.renderItem(display, -8, -8);
            gfx.pose().popPose();
            int lineY = (int) (top + itemSize + 2);
            label(gfx, ellipsize(display.getHoverName().getString(), maxText, NAME_SIZE, true), tx, lineY,
                    NAME_SIZE, selected ? LABEL_SELECTED : 0xFFFFFFFF, true);
            String count = choice.rounds() + "/" + choice.capacity();
            if (choice.magazineType().equals(preferredType)) count = "★ " + count;
            label(gfx, ellipsize(count, maxText, SMALL_SIZE, false), tx, lineY + NAME_LINE,
                    SMALL_SIZE, 0xFFB4BDC0, false);
            int nextY = lineY + NAME_LINE + SMALL_LINE;
            if (showAmmo) {
                ammoLine(gfx, ball, tracers, tx, nextY, maxText);
                nextY += SMALL_LINE;
            }
            if (group.quantity() > 1) {
                label(gfx, "×" + group.quantity(), tx, nextY, SMALL_SIZE, GcrUITheme.AMBER, false);
            }
        }

        label(gfx, "R", cx, cy - 13, 11f, GcrUITheme.AMBER, true);
        label(gfx, ellipsize(Component.translatable("exe_tacticals.wheel.cancel").getString(),
                        (float) (CANCEL_RADIUS * 1.6), SMALL_SIZE, false), cx, cy + 2, SMALL_SIZE,
                cancelHover ? LABEL_SELECTED : 0xFFFFFFFF, false);
        if (groups.size() > PAGE_SIZE) {
            label(gfx, (page + 1) + "/" + ((groups.size() + PAGE_SIZE - 1) / PAGE_SIZE),
                    cx, cy + (int) r + 9, NAME_SIZE, GcrUITheme.AMBER, true);
        }
        label(gfx, Component.translatable("exe_tacticals.wheel.hint").getString(),
                cx, Math.min(height - 12, cy + (int) r + 28), SMALL_SIZE, 0xFFB4BDC0, false);
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
