package com.exetacticals.client.screen.modify;

import com.exetacticals.Client;
import com.exetacticals.client.render.ModifyScreenRenderContext;
import com.exetacticals.client.screen.GuiPanelRender;
import com.exetacticals.items.GunItem;
import com.exetacticals.items.ModuleItem;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.ModuleRegister;
import com.exetacticals.modularSys.builder.*;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.slot.IRail;
import com.exetacticals.modularSys.slot.OperationType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class GunModifyScreen extends Screen {
    public static final float BACKGROUND_BLUR = 5f;
    public static final int BACKGROUND_TINT = 0x400B0B0E;

    private static final int WAITING_TINT = 0x8C0D0D0F;
    private static final float WAITING_BLUR = 12f;
    private static final int WAITING_OVERLAY_DELAY_TICKS = 10;

    private static final float MIN_MODEL_SCALE = 0.5f;
    private static final float MAX_MODEL_SCALE = 2.5f;
    private static final float MODEL_ZOOM_STEP = 0.2f;
    private static final float MODEL_WHEEL_ZOOM_STEP = 0.1f;

    private GunModifyContext context;
    private boolean isDraggingModel = false;
    private boolean isRollingModel = false;
    private boolean needUpdate = false;
    private int waitingTicks = 0;
    private int rejectedMutateId = -1;
    private GunModifyContext.Selection pendingSelection = null;
    private float modelRX;
    private float modelRY;
    private float modelX;
    private float modelY;
    private float modelScale = 1;
    private float tempModelRX;
    private float tempModelRY;
    private float dragStartX;
    private float dragStartY;
    private boolean inspectionMode = false;
    private boolean inspectionInteracted = false;
    private int inspectionHintTicks = 0;
    private int inspectionMessageTicks = 0;
    private boolean inspectionRightDown = false;
    private boolean inspectionLeftDown = false;
    private final List<Component> issues = new ArrayList<>();

    private final List<ItemStack> allSuitableModules = new ArrayList<>();

    public ModularCountManager countManager = new ModularCountManager();

    private int page = 1;

    private SlotInstance lastModularSlot = null;

    protected GunModifyScreen(Component title) {
        super(title);
        Minecraft instance = Minecraft.getInstance();
        if (instance.player != null) {
            countManager.onTick(instance.player);
        }
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        if (allSuitableModules.isEmpty()) {
            return;
        }
        this.page = Mth.clamp(page, 1, Math.max(1, getMaxPage()));
    }

    public int getMaxPage() {
        int pageSize = GunModifyUI.getPageSize();
        if (pageSize == 0) {
            return 0;
        }
        return (allSuitableModules.size() + pageSize - 1) / pageSize;
    }

    public void changePage(int delta) {
        setPage(page + delta);
        updatePageContent();
    }

    @Override
    public void init() {
        super.init();
        Client.getGunRenderer().setHideFPRender(true);
        lastModularSlot = null;
        GunModifyUI.create(this);
        if (inspectionMode) {
            GunModifyUI.setVisible(false);
        }
        checkAndCreateContext();
        if (context != null) {
            onSelectionChange(context.getSelectedNode(), context.getSelectedSlot());
        }
    }

    public void onServerResp(boolean success, String msg) {
        needUpdate = false;
        rejectedMutateId = -1;
        context = null;
        countManager = new ModularCountManager();
        if (this.minecraft != null && this.minecraft.player != null) {
            countManager.onTick(this.minecraft.player);
        }
        checkAndCreateContext();
        lastModularSlot = null;
        onSelectionChange(null, null);
        if (context != null && pendingSelection != null) {
            context.restoreSelection(pendingSelection);
        }
        pendingSelection = null;
        if (!success && msg != null && !msg.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal(msg));
        }
    }

    public boolean debugSelectSlot(String slotName) {
        return context != null && context.debugSelectSlot(slotName);
    }

    public <T extends AbstractWidget> T addUiWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public void onClose() {
        super.onClose();
        Client.getGunRenderer().setHideFPRender(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.player != null) {
            countManager.onTick(this.minecraft.player);
        }
        waitingTicks = needUpdate ? waitingTicks + 1 : 0;
        updateInspectionMode();
        updatePageContent();
        collectIssues();
        if (context != null) {
            context.onTick();
            if (context.consumeRailScrollResync()) {
                syncScrollerToSelectedNode();
            }
            Node selectedNode = context.getSelectedNode();
            if (selectedNode != null && selectedNode.getBelongsTo() != null) {
                if (selectedNode.getBelongsTo().allow(OperationType.REMOVE)) {
                    GunModifyUI.btnRemoveModule.enable("tooltip.btn.remove_module");
                } else {
                    GunModifyUI.btnRemoveModule.disable("tooltip.btn.opt_not_allow");
                }
            } else if (GunModifyUI.btnRemoveModule.selfVisible) {
                GunModifyUI.btnRemoveModule.setVisible(false);
            }
            tryAutoCommit();
        }
    }

    private void tryAutoCommit() {
        if (needUpdate || !context.treeModified() || !context.allowSave()) {
            return;
        }
        if (this.minecraft != null && GLFW.glfwGetMouseButton(
                this.minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            return;
        }
        int mutateId = context.builder.getWorkspace().getMutateId();
        if (mutateId == rejectedMutateId) {
            return;
        }
        GunModifyContext.Selection selection = context.saveSelection();
        if (context.commitAndSync()) {
            pendingSelection = selection;
            needUpdate = true;
            waitingTicks = 0;
        } else {
            rejectedMutateId = mutateId;
        }
    }

    private void collectIssues() {
        issues.clear();
        if (context == null) {
            return;
        }
        ValidateResult validateResult = context.getValidateResult();
        if (validateResult != null) {
            for (ValidationError issue : validateResult.getIssues()) {
                issues.add(issue.getComponentMsg());
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderGunModel();
        GuiPanelRender.captureFrame();
        if (context != null && !inspectionMode) {
            context.renderComponents(guiGraphics);
        }
        GunModifyUI.refreshVisibility();
        GunModifyUI.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        GunModifyUI.renderForeground(guiGraphics, mouseX, mouseY, issues);
        if (needUpdate && waitingTicks > WAITING_OVERLAY_DELAY_TICKS) {
            renderWaitingOverlay(guiGraphics);
        }
        renderInspectionOverlay(guiGraphics);
    }

    private void renderWaitingOverlay(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        GuiPanelRender.glass(g, 0, 0, this.width, this.height, 0f, WAITING_BLUR, WAITING_TINT);
        float time = (System.currentTimeMillis() % 1200L) / 1200f;
        float cx = this.width / 2f;
        float cy = this.height / 2f - 12;
        for (int i = 0; i < 3; i++) {
            float phase = (time + i / 3f) % 1f;
            float pulse = 0.5f + 0.5f * Mth.sin(phase * Mth.TWO_PI);
            GuiPanelRender.circle(g, cx - 10 + i * 10, cy, 2f + pulse, GuiPanelRender.alpha(GuiPanelRender.ACCENT, 0.4f + 0.6f * pulse));
        }
        List<net.minecraft.util.FormattedCharSequence> lines =
                this.font.split(Component.translatable("screen.need_update"), Math.min(260, this.width - 40));
        for (int i = 0; i < lines.size(); i++) {
            g.drawCenteredString(this.font, lines.get(i), this.width / 2, this.height / 2 + 2 + i * 10, GuiPanelRender.TEXT);
        }
        g.pose().popPose();
    }

    private void renderGunModel() {
        if (context != null) {
            Client.getGunRenderer().renderGunModifyScreen(
                    context.itemStack,
                    context.gun,
                    context.renderRoot,
                    -modelX * 0.01f,
                    modelY * 0.01f,
                    modelRX,
                    modelRY,
                    modelScale,
                    this::updateRenderContext);
        }
    }

    public void onAttachmentSlotClick(DisplaySlot slot) {
        if (context == null || needUpdate || slot.getValue().isEmpty() || slot.getCount() <= 0 || slot.isInstalled()) {
            return;
        }
        if (!(slot.getValue().getItem() instanceof ModuleItem<?> moduleItem)) {
            return;
        }
        IModular module = moduleItem.getModule();
        if (module instanceof IGun || countManager.getModuleLeftCount(module) <= 0) {
            return;
        }
        SlotInstance selectedSlot = context.getSelectedSlot();
        if (context.getSelectedNode() != null) {
            replaceSelectedModule(module);
        } else if (selectedSlot != null && selectedSlot.accepts(module)) {
            if (context.addUnit(module)) {
                countManager.onModuleUsed(module);
            }
        }
    }

    public void onDragModel(double mouseX, double mouseY) {
        dragStartX = (float) mouseX;
        dragStartY = (float) mouseY;
        isDraggingModel = true;
    }

    public void stopDragModel() {
        dragStartX = 0;
        dragStartY = 0;
        isDraggingModel = false;
    }

    public void zoomInModel() {
        modelScale = Mth.clamp(modelScale + MODEL_ZOOM_STEP, MIN_MODEL_SCALE, MAX_MODEL_SCALE);
    }

    public void zoomOutModel() {
        modelScale = Mth.clamp(modelScale - MODEL_ZOOM_STEP, MIN_MODEL_SCALE, MAX_MODEL_SCALE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (inspectionMode) {
            if (scrollY != 0) {
                markInspectionInteraction();
                modelScale = Mth.clamp(modelScale + (scrollY > 0 ? MODEL_WHEEL_ZOOM_STEP : -MODEL_WHEEL_ZOOM_STEP), MIN_MODEL_SCALE, MAX_MODEL_SCALE);
            }
            return true;
        }
        if (needUpdate) {
            return false;
        }
        if (super.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (GunModifyUI.isOverPanel(mouseX, mouseY)) {
            if (scrollY != 0 && getMaxPage() > 1) {
                changePage(scrollY > 0 ? -1 : 1);
            }
            return true;
        }
        if (scrollY == 0) {
            return false;
        }
        modelScale = Mth.clamp(
                modelScale + (scrollY > 0 ? MODEL_WHEEL_ZOOM_STEP : -MODEL_WHEEL_ZOOM_STEP),
                MIN_MODEL_SCALE, MAX_MODEL_SCALE);
        return true;
    }

    public void removeSelectedModule() {
        if (context == null || needUpdate) {
            return;
        }
        List<IModular> removed = context.removeSelectedNode();
        for (IModular modular : removed) {
            countManager.onModuleTakeOffFromGun(modular);
        }
    }

    private void replaceSelectedModule(IModular module) {
        Node selectedNode = context.getSelectedNode();
        SlotInstance belongsTo = selectedNode.getBelongsTo();
        if (belongsTo == null || !belongsTo.allow(OperationType.REPLACE)) {
            return;
        }
        if (module == selectedNode.getModule() || !belongsTo.accepts(module)) {
            return;
        }
        Pair<Unit, List<Unit>> unitListPair = context.replaceSelectNode(module);
        Unit installed = unitListPair.getLeft();
        if (installed != null) {
            for (Unit unit : unitListPair.getRight()) {
                countManager.onModuleTakeOffFromGun(unit.getModule());
            }
            countManager.onModuleUsed(installed.getModule());
        }
    }

    public void enterInspectionMode() {
        inspectionMode = true;
        inspectionInteracted = false;
        inspectionHintTicks = 0;
        inspectionMessageTicks = 60;
        resetModelPose();
        GunModifyUI.setVisible(false);
    }

    private void exitInspectionMode() {
        inspectionMode = false;
        inspectionRightDown = false;
        inspectionLeftDown = false;
        isRollingModel = false;
        isDraggingModel = false;
        GunModifyUI.setVisible(true);
    }

    private void markInspectionInteraction() {
        if (!inspectionInteracted) {
            inspectionInteracted = true;
            inspectionHintTicks = 60;
        }
    }

    private void updateInspectionMode() {
        inspectionMessageTicks = Math.max(0, inspectionMessageTicks - 1);
        inspectionHintTicks = Math.max(0, inspectionHintTicks - 1);
        if (inspectionMode && !inspectionInteracted && !inspectionLeftDown && !inspectionRightDown) {
            modelRY += 0.01f;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inspectionMode) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                exitInspectionMode();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                resetModelPose();
                inspectionInteracted = false;
                inspectionHintTicks = 0;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderInspectionOverlay(GuiGraphics g) {
        if (!inspectionMode) {
            return;
        }
        if (inspectionMessageTicks > 0) {
            renderPill(g, Component.translatable("tooltip.inspect.exit"), this.width / 2, 16, inspectionMessageTicks);
        }
        if (inspectionHintTicks > 0) {
            renderPill(g, Component.translatable("tooltip.inspect.reset"), this.width / 2, this.height - 30, inspectionHintTicks);
        }
    }

    private void renderPill(GuiGraphics g, Component text, int cx, int y, int ticksLeft) {
        float alpha = Math.min(1f, ticksLeft / 10f);
        int w = this.font.width(text) + 16;
        GuiPanelRender.glass(g, cx - w / 2f, y - 5, w, 18, 9f, GuiPanelRender.PANEL_BLUR,
                GuiPanelRender.alpha(GuiPanelRender.PANEL_TINT, alpha));
        GuiPanelRender.stroke(g, cx - w / 2f, y - 5, w, 18, 9f, 0.5f, GuiPanelRender.alpha(GuiPanelRender.PANEL_BORDER, alpha));
        g.drawCenteredString(this.font, text, cx, y, GuiPanelRender.alpha(GuiPanelRender.TEXT, Math.max(0.05f, alpha)));
    }

    public void resetModelPose() {
        modelRX = 0;
        modelRY = 0;
        tempModelRX = 0;
        tempModelRY = 0;
        modelX = 0;
        modelY = 0;
        modelScale = 1;
        dragStartX = 0;
        dragStartY = 0;
    }

    public void trySetNodePos(float pos) {
        if (context != null) {
            context.trySetSelectedNodePos(pos);
        }
    }

    private void updateRenderContext(ModifyScreenRenderContext moduleRenderContext) {
        if (context != null) {
            context.onModuleTreeRender(moduleRenderContext);
        }
    }

    private void checkAndCreateContext() {
        if (this.context != null) {
            return;
        }
        ItemStack itemStack = checkAndGet();
        if (itemStack != null) {
            this.context = new GunModifyContext(itemStack);
            this.context.setOnSelectionChange(this::onSelectionChange);
        }
    }

    private void onSelectionChange(Node node, SlotInstance slot) {
        if (node == null && slot == null) {
            GunModifyUI.clearItemSlots();
            GunModifyUI.setStripVisible(false);
            GunModifyUI.setScrollComponentsVisible(false);
            lastModularSlot = null;
            updateLabels();
            return;
        }
        SlotInstance selectedSlot = node == null ? slot : node.getBelongsTo();
        if (node != null && node.getBelongsToSlot() instanceof IRail rail && !node.isFixedPosition()) {
            GunModifyUI.setScrollComponentsVisible(true);
            GunModifyUI.setScrollPos(context.railPosToScrollValue(rail.getNormalizedChildPosition(node.getUnit())));
        } else {
            GunModifyUI.setScrollComponentsVisible(false);
        }
        if (node == null) {
            GunModifyUI.btnRemoveModule.setVisible(false);
        } else {
            GunModifyUI.btnRemoveModule.setVisible(true);
        }
        GunModifyUI.setStripVisible(true);
        if (selectedSlot != lastModularSlot) {
            GunModifyUI.clearItemSlots();
            lastModularSlot = selectedSlot;
            updateAllSuitableModules(selectedSlot);
            page = 1;
        }
        updatePageContent();
    }

    private void updateAllSuitableModules(SlotInstance selectedSlot) {
        allSuitableModules.clear();
        if (selectedSlot == null) {
            return;
        }
        Map<String, IModular> all = ModuleRegister.all();
        for (IModular module : all.values()) {
            if (selectedSlot.accepts(module) && module.getBindItem() != null) {
                allSuitableModules.add(new ItemStack(module.getBindItem()));
            }
        }
    }

    public void resetScroller() {
        if (context != null && context.getSelectedNode() != null) {
            Node selectedNode = context.getSelectedNode();
            if (selectedNode.getBelongsToSlot() instanceof IRail rail) {
                GunModifyUI.setScrollPos(context.railPosToScrollValue(rail.getNormalizedOriginOffest()));
            }
        }
    }

    private void syncScrollerToSelectedNode() {
        if (context == null) {
            return;
        }
        Node selectedNode = context.getSelectedNode();
        if (selectedNode != null
                && selectedNode.getBelongsToSlot() instanceof IRail rail
                && !selectedNode.isFixedPosition()) {
            GunModifyUI.setScrollComponentsVisible(true);
            GunModifyUI.setScrollPos(context.railPosToScrollValue(rail.getNormalizedChildPosition(selectedNode.getUnit())));
        }
    }

    private void updatePageContent() {
        List<DisplaySlot> itemSlots = GunModifyUI.getItemSlots();
        int pageSize = itemSlots.size();
        int pageStart = (page - 1) * pageSize;
        IModular installedModule = context != null && context.getSelectedNode() != null
                ? context.getSelectedNode().getModule() : null;
        for (int indexInPage = 0; indexInPage < pageSize; indexInPage++) {
            DisplaySlot itemSlot = itemSlots.get(indexInPage);
            int i = pageStart + indexInPage;
            if (i >= allSuitableModules.size()) {
                itemSlot.setItem(ItemStack.EMPTY);
                itemSlot.setInstalled(false);
                continue;
            }
            ItemStack itemStack = allSuitableModules.get(i);
            itemSlot.setItem(itemStack);
            if (itemStack.getItem() instanceof ModuleItem<?> item) {
                IModular module = item.getModule();
                itemSlot.setCount(countManager.getModuleLeftCount(module));
                itemSlot.setInstalled(module == installedModule);
            }
        }
        updateLabels();
        GunModifyUI.syncPageInfo(this);
        GunModifyUI.layoutStrip(getMaxPage() > 1);
    }

    private void updateLabels() {
        if (context == null) {
            GunModifyUI.setLabels(Component.empty(), null, null, 0);
            return;
        }
        Node node = context.getSelectedNode();
        SlotInstance slot = context.getActiveSlot();
        Component module = null;
        if (node != null && node.getModule() != null && node.getModule().getBindItem() != null) {
            module = new ItemStack(node.getModule().getBindItem()).getHoverName();
        }
        GunModifyUI.setLabels(context.itemStack.getHoverName(), slot == null ? null : slotLabel(slot.slotName()),
                module, allSuitableModules.size());
    }

    public static Component slotLabel(String slotName) {
        if (slotName == null || slotName.isEmpty()) {
            return Component.empty();
        }
        String lower = slotName.toLowerCase(Locale.ROOT);
        String pretty = lower.replace('_', ' ');
        pretty = Character.toUpperCase(pretty.charAt(0)) + pretty.substring(1);
        return Component.translatableWithFallback("slot.exe_tacticals." + lower, pretty);
    }

    private ItemStack checkAndGet() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            return player.getMainHandItem();
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inspectionMode) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                inspectionRightDown = true;
                markInspectionInteraction();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                inspectionLeftDown = true;
                markInspectionInteraction();
            }
            dragStartX = (float) mouseX;
            dragStartY = (float) mouseY;
            tempModelRX = modelRX;
            tempModelRY = modelRY;
            return true;
        }
        if (needUpdate) {
            return false;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!handled && GunModifyUI.isOverPanel(mouseX, mouseY)) {
            return true;
        }
        if (context != null && !handled) {
            if (!context.onMouseClick((float) mouseX, (float) mouseY)) {
                if (!isDraggingModel) {
                    isRollingModel = true;
                }
                dragStartX = (float) mouseX;
                dragStartY = (float) mouseY;
            }
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (inspectionMode) {
            float dx = (float) (pMouseX - dragStartX);
            float dy = (float) (pMouseY - dragStartY);
            if (inspectionRightDown) {
                modelX -= dx * 0.5f;
                modelY -= dy * 0.5f;
            } else if (inspectionLeftDown) {
                modelRY += dx * 0.05f;
                modelRX += dy * 0.05f;
            }
            dragStartX = (float) pMouseX;
            dragStartY = (float) pMouseY;
            return true;
        }
        if (isDraggingModel) {
            modelX -= (float) ((pMouseX - dragStartX) * 0.5f);
            modelY -= (float) ((pMouseY - dragStartY) * 0.5f);
            dragStartX = (float) pMouseX;
            dragStartY = (float) pMouseY;
            isRollingModel = false;
        } else if (isRollingModel) {
            modelRY = tempModelRY + (float) (pMouseX - dragStartX) * 0.05f;
            modelRX = tempModelRX + (float) (pMouseY - dragStartY) * 0.05f;
        } else {
            for (Renderable renderable : this.renderables) {
                if (renderable instanceof AbstractWidget widget) {
                    widget.mouseMoved(pMouseX, pMouseY);
                }
            }
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (inspectionMode) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                inspectionRightDown = false;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                inspectionLeftDown = false;
            }
            tempModelRY = modelRY % 360;
            tempModelRX = modelRX % 360;
            return true;
        }
        if (isRollingModel) {
            isRollingModel = false;
            tempModelRY = modelRY % 360;
            tempModelRX = modelRX % 360;
        }
        isDraggingModel = false;
        dragStartX = 0;
        dragStartY = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics) {
    }
}
