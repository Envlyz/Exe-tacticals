package com.exetacticals.client.events;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.client.PhysicalMagazineInput;
import com.exetacticals.client.ClientWeaponLooper;
import com.exetacticals.client.DrawHolsterHandler;
import com.exetacticals.client.KeyBinds;
import com.exetacticals.client.SprintingHandler;
import com.exetacticals.client.screen.DebugDisplayDataAdjustScreen;
import com.exetacticals.client.screen.modify.GunModifyUI;
import com.exetacticals.client.stuck.ClientGunStuckCache;
import com.exetacticals.items.GunItem;
import com.exetacticals.items.MagazineItem;
import com.exetacticals.modularSys.builder.Node;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.IInteractiveModular;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.modules.guns.ISlottedGun;
import com.exetacticals.modularSys.task.GunTaskHandler;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.modularSys.task.other.CheckingTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ControllerEvents {
    public static boolean debug_i = false;
    public static int adsDelay = 0;

    @SubscribeEvent
    public static void onKeyBoardEvent(InputEvent.Key event) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack itemStack = player.getMainHandItem();
            if (itemStack.getItem() instanceof GunItem gunItem) {
                IGun gunModule = gunItem.getGun();
                if (GCR.IS_DEVELOPMENT && KeyBinds.OPEN_DISPLAY_ADJUST_SCREEN.isDown()) {
                    Minecraft.getInstance().setScreen(new DebugDisplayDataAdjustScreen());
                }
                if (KeyBinds.OPEN_GUN_MODIFY_SCREEN.isDown()) {
                    GunModifyUI.open();
                }
                handleGunFunctionalInput(gunModule, itemStack, event);
            }
    }

    private static void handleGunFunctionalInput(IGun gunModule, ItemStack itemStack, InputEvent.Key event) {
        if (DrawHolsterHandler.get().getEquipProgress() < 1 || Minecraft.getInstance().screen != null) {
            return;
        }
        if (event.getAction() == 1) {
            if (KeyBinds.CHECK_MAG.isDown() && !SprintingHandler.INSTANCE.isSprinting()) {
                IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.CHECKING, Map.of("type", CheckingTask.CHECK_MAG));
                if (task != null) {
                    GunTaskHandler.INSTANCE.setTask(task);
                }
            }
            if (KeyBinds.CHECK_CHAMBER.isDown()) {
                IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.CHECKING, Map.of("type", CheckingTask.CHECK_CHAMBER));
                if (task != null) {
                    GunTaskHandler.INSTANCE.setTask(task);
                }
            }
            if (KeyBinds.SWITCH_FIRE_MODE.isDown()) {
                IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.SWITCH_FIRE_MODE, Map.of());
                if (task != null) {
                    GunTaskHandler.INSTANCE.setTask(task);
                }
            }
            if (KeyBinds.SWITCH_EFFECTIVE_SIGHT.isDown()) {
                if (gunModule instanceof ISlottedGun) {
                    IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.SWITCH_USING_SIGHT, Map.of());
                    if (task != null) {
                        task.start();
                    }
                }
            }
        }

        Map<String, Node> idToNodes = Client.WEAPON_STATUS.getIDToNodes();
        for (Map.Entry<String, Node> entry : idToNodes.entrySet()) {
            String id = entry.getKey();
            Node node = entry.getValue();
            Unit unit = node.getUnit();
            if (unit.getModule() instanceof IInteractiveModular iInteractiveModular) {
                iInteractiveModular.onKeyPressed(event.getKey(), event.getAction(), id, unit, gunModule, itemStack);
            }
        }
    }

    @SubscribeEvent
    public static void onTickPre(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (Client.LEFT_BUTTON_PRESSED.get()) {
            if (minecraft.screen != null) {
                Client.LEFT_BUTTON_PRESSED.set(false);
            }
        }
    }

    @SubscribeEvent
    public static void onTickPost(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        adsDelay = Math.max(0, adsDelay - 1);
        PhysicalMagazineInput.tick();
        if (!shouldHandleMouseEvent() || !Client.WEAPON_STATUS.isHoldingGun()) {
            Client.RIGHT_BUTTON_PRESSED.set(false);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (shouldHandleMouseEvent() && Client.WEAPON_STATUS.isHoldingGun()) {
            ItemStack itemStack = Client.WEAPON_STATUS.getItemStack();
            IGun gun = Client.WEAPON_STATUS.getGun();
            if (gun != null && itemStack != null) {
                double mouseX = event.getMouseX();
                double mouseY = event.getMouseY();
                double scrollDeltaX = 0.0;
                double scrollDeltaY = event.getScrollDelta();
                Map<String, Node> idToNodes = Client.WEAPON_STATUS.getIDToNodes();
                for (Map.Entry<String, Node> entry : idToNodes.entrySet()) {
                    String id = entry.getKey();
                    Node node = entry.getValue();
                    Unit unit = node.getUnit();
                    if (unit.getModule() instanceof IInteractiveModular iInteractiveModular) {
                        boolean cancel = iInteractiveModular.onMouseScroll(mouseX, mouseY, scrollDeltaX, scrollDeltaY, id, unit, gun, itemStack);
                        if (cancel) {
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMagazineUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isUseItem() && mc.player != null && mc.screen == null
                && mc.player.getMainHandItem().getItem() instanceof MagazineItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMousePress(InputEvent.MouseButton.Pre event) {
        if (!shouldHandleMouseEvent()) {
            return;
        }
        int action = event.getAction();
        int btn = event.getButton();

        if (Client.WEAPON_STATUS.isHoldingGun()) {
            if (btn >= 2 && action == 1) {
                dispatchRemappedMouseAction(btn);
            }
            if (action == 1) {
                if (btn == 0) {
                    Client.LEFT_BUTTON_PRESSED.set(true);
                    ClientWeaponLooper.wake();
                } else if (btn == 1) {
                    if (adsDelay <= 0) {
                        Client.RIGHT_BUTTON_PRESSED.set(true);
                    }
                }
                if (btn == 0 || btn == 1) event.setCanceled(true);
            } else if (action == 0) {
                if (btn == 0) {
                    Client.LEFT_BUTTON_PRESSED.set(false);
                    Client.WEAPON_STATUS.fireCount = 0;
                } else if (btn == 1) {
                    Client.RIGHT_BUTTON_PRESSED.set(false);
                }
            }
            IGun gun = Client.WEAPON_STATUS.getGun();
            ItemStack itemStack = Client.WEAPON_STATUS.getItemStack();
            if (gun == null || itemStack == null) {
                return;
            }
            Map<String, Node> idToNodes = Client.WEAPON_STATUS.getIDToNodes();
            for (Map.Entry<String, Node> entry : idToNodes.entrySet()) {
                String id = entry.getKey();
                Node node = entry.getValue();
                Unit unit = node.getUnit();
                if (unit.getModule() instanceof IInteractiveModular iInteractiveModular) {
                    iInteractiveModular.onMousePressed(btn, action, id, unit, gun, itemStack);
                }
            }
        }
    }

    private static void dispatchRemappedMouseAction(int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem weapon)) return;
        IGun gun = weapon.getGun();
        if (GCR.IS_DEVELOPMENT && KeyBinds.OPEN_DISPLAY_ADJUST_SCREEN.matchesMouse(button)) {
            mc.setScreen(new DebugDisplayDataAdjustScreen());
            return;
        }
        if (KeyBinds.OPEN_GUN_MODIFY_SCREEN.matchesMouse(button)) {
            GunModifyUI.open();
            return;
        }
        if (DrawHolsterHandler.get().getEquipProgress() < 1) return;
        if (KeyBinds.CHECK_MAG.matchesMouse(button) && !SprintingHandler.INSTANCE.isSprinting()) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.CHECKING,
                    Map.of("type", CheckingTask.CHECK_MAG));
            if (task != null) GunTaskHandler.INSTANCE.setTask(task);
        }
        if (KeyBinds.CHECK_CHAMBER.matchesMouse(button)) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.CHECKING,
                    Map.of("type", CheckingTask.CHECK_CHAMBER));
            if (task != null) GunTaskHandler.INSTANCE.setTask(task);
        }
        if (KeyBinds.SWITCH_FIRE_MODE.matchesMouse(button)) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.SWITCH_FIRE_MODE, Map.of());
            if (task != null) GunTaskHandler.INSTANCE.setTask(task);
        }
        if (KeyBinds.SWITCH_EFFECTIVE_SIGHT.matchesMouse(button)
                && gun instanceof ISlottedGun) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.SWITCH_USING_SIGHT, Map.of());
            if (task != null) task.start();
        }
    }

    private static boolean shouldHandleMouseEvent() {
        return Minecraft.getInstance().isWindowActive() && !Minecraft.getInstance().isPaused() && Minecraft.getInstance().screen == null;
    }
}
