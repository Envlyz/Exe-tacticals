package com.exetacticals.client;

import com.exetacticals.Client;
import com.exetacticals.client.events.ControllerEvents;
import com.exetacticals.client.screen.MagazineWheelScreen;
import com.exetacticals.client.model.modular.animation.eventSys.EventType;
import com.exetacticals.client.stuck.ClientGunStuckCache;
import com.exetacticals.items.GunItem;
import com.exetacticals.items.MagazineItem;
import com.exetacticals.modularSys.builder.Builder;
import com.exetacticals.modularSys.builder.Node;
import com.exetacticals.modularSys.builder.WorkSpace;
import com.exetacticals.modularSys.modules.Mag;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.modules.guns.ISlottedGun;
import com.exetacticals.modularSys.task.GunTaskHandler;
import com.exetacticals.modularSys.task.IGunTask;
import com.exetacticals.network.GCRNetwork;
import com.exetacticals.network.c2s.PhysicalMagazinePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public final class PhysicalMagazineInput {
    private static final int DOUBLE_TAP = 8;
    private static final int HOLD = 10;
    private static final int RADIAL_HOLD = 24;
    private static final int INSERT_ANIMATION_LEAD = 12;

    private static boolean wasDown;
    private static int heldTicks;
    private static int unloadHeldTicks;
    private static int tapWindow;
    private static boolean secondTap;
    private static boolean consumedHold;
    private static boolean clearingJam;
    private static int pendingAction = -1;
    private static int pendingDelay;
    private static String pendingGunId = "";
    private static String trackedGunId = "";
    private static int pendingSlot = -1;
    private static ItemStack pendingSnapshot = ItemStack.EMPTY;
    private static boolean suppressUntilRelease;

    private PhysicalMagazineInput() {}

    private static void stopJam() {
        if (!clearingJam) return;
        clearingJam = false;
        GCRNetwork.sendToServer(new PhysicalMagazinePacket(PhysicalMagazinePacket.STOP_UNJAM));
        if (GunTaskHandler.INSTANCE.hasTask()) GunTaskHandler.INSTANCE.cancelTask();
    }

    private static void reset() {
        stopJam();
        wasDown = false;
        heldTicks = 0;
        unloadHeldTicks = 0;
        tapWindow = 0;
        secondTap = false;
        consumedHold = false;
        pendingAction = -1;
        pendingGunId = "";
        pendingSlot = -1;
        pendingSnapshot = ItemStack.EMPTY;
        suppressUntilRelease = false;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MagazineWheelScreen wheel) {
            if (!mc.isWindowActive() || mc.player == null) wheel.onClose();
            return;
        }
        if (mc.player == null || mc.screen != null || !mc.isWindowActive()) {
            reset();
            return;
        }
        ItemStack stack = mc.player.getMainHandItem();
        boolean down = KeyBinds.RELOAD.isDown();
        if (stack.getItem() instanceof MagazineItem magazine) {
            stopJam();
            pendingAction = -1;
            tapWindow = 0;
            trackedGunId = "";
            heldTicks = 0;
            unloadHeldTicks = 0;
            wasDown = down;
            return;
        }
        unloadHeldTicks = 0;
        if (!(stack.getItem() instanceof GunItem weapon)) {
            reset();
            trackedGunId = "";
            return;
        }
        IGun gun = weapon.getGun();
        String identity = gun.getIdentityID(stack);
        if (!Objects.equals(identity, trackedGunId)) {
            reset();
            trackedGunId = identity;
        }
        if (pendingAction != -1) {
            if (!Objects.equals(identity, pendingGunId)) {
                pendingAction = -1;
            } else if (--pendingDelay <= 0) {
                if (pendingAction == PhysicalMagazinePacket.SELECTED_INSERT) {
                    GCRNetwork.sendToServer(new PhysicalMagazinePacket(
                            pendingAction, pendingSlot, pendingGunId, pendingSnapshot));
                } else {
                    GCRNetwork.sendToServer(new PhysicalMagazinePacket(pendingAction));
                }
                pendingAction = -1;
                pendingSlot = -1;
                pendingSnapshot = ItemStack.EMPTY;
            }
        }

        if (suppressUntilRelease) {
            if (!down) {
                suppressUntilRelease = false;
                wasDown = false;
            }
            return;
        }
        if (clearingJam && !GunTaskHandler.INSTANCE.hasTask()) {
            clearingJam = false;
        }
        if (tapWindow > 0) tapWindow--;
        if (down && !wasDown) {
            secondTap = tapWindow > 0;
            heldTicks = 0;
            consumedHold = false;
            if (ClientGunStuckCache.get().isStuck(stack, gun)) {
                consumedHold = true;
                secondTap = false;
                tapWindow = 0;
                if (!clearingJam && !GunTaskHandler.INSTANCE.hasTask()) {
                    ClientGunStuckCache.get().projectToNbt(stack, gun);
                    IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.REMOVE_STUCK, Map.of());
                    if (task != null) {
                        clearingJam = true;
                        GCRNetwork.sendToServer(new PhysicalMagazinePacket(PhysicalMagazinePacket.START_UNJAM));
                        GunTaskHandler.INSTANCE.setTask(task);
                    }
                }
            }
        }
        if (down) {
            heldTicks++;
            if (!clearingJam && !consumedHold && heldTicks >= RADIAL_HOLD && pendingAction == -1) {
                List<MagazineChoice> choices = availableMagazines(gun, stack);
                consumedHold = true;
                secondTap = false;
                tapWindow = 0;
                if (!choices.isEmpty()) {
                    String preferred = stack.hasTag()
                            ? stack.getTag().getString(PhysicalMagazinePacket.PREFERRED_MAG_KEY) : "";
                    Client.RIGHT_BUTTON_PRESSED.set(false);
                    Client.LEFT_BUTTON_PRESSED.set(false);
                    mc.setScreen(new MagazineWheelScreen(choices, identity, preferred));
                    wasDown = true;
                    return;
                }
            }
        }

        if (!down && wasDown) {
            if (!clearingJam && !consumedHold) {
                if (secondTap && heldTicks < HOLD) {
                    tapWindow = 0;
                    beginMagazine(gun, stack, PhysicalMagazinePacket.REMOVE);
                } else if (heldTicks >= HOLD) {
                    tapWindow = 0;
                    beginMagazine(gun, stack, PhysicalMagazinePacket.INSERT);
                } else {
                    tapWindow = DOUBLE_TAP;
                }
            }
            secondTap = false;
        }
        wasDown = down;
    }

    private static void beginMagazine(IGun gun, ItemStack stack, int action) {
        beginMagazine(gun, stack, action, null);
    }

    private static void beginMagazine(IGun gun, ItemStack stack, int action, MagazineChoice selected) {
        if (Client.WEAPON_STATUS == null || !Client.WEAPON_STATUS.isHoldingGun()) return;
        if (pendingAction != -1) return;
        String identity = gun.getIdentityID(stack);
        if (identity == null || IGun.NONE.equals(identity)) return;

        if (action == PhysicalMagazinePacket.INSERT && availableMagazines(gun, stack).isEmpty()) return;
        if (action == PhysicalMagazinePacket.SELECTED_INSERT && selected == null) return;

        boolean ejectChamber = false;
        if (action == PhysicalMagazinePacket.REMOVE && gun instanceof ISlottedGun slotted) {
            ejectChamber = slotted.getMagAttachment(stack) == null;
            if (ejectChamber && slotted.getGunAmmoLeft(stack) <= 0) return;
        }
        boolean replacing = gun instanceof ISlottedGun slotted && slotted.getMagAttachment(stack) != null;
        String animation = action == PhysicalMagazinePacket.REMOVE
                ? (ejectChamber ? "check_chamber" : "mag_reload")
                : (replacing ? "mag_reload" : "mag_reload_empty");
        Client.getGunRenderer().dispatchAnimationEvent(
                EventType.RELOAD, Map.of("animation_name", animation));
        pendingGunId = identity;
        pendingAction = action;
        pendingDelay = INSERT_ANIMATION_LEAD;
        pendingSlot = selected == null ? -1 : selected.slot();
        pendingSnapshot = selected == null ? ItemStack.EMPTY : selected.snapshot().copy();
    }

    public record MagazineChoice(int slot, ItemStack snapshot, String magazineType, int rounds, int capacity) {}

    public static List<MagazineChoice> availableMagazines(IGun gun, ItemStack gunStack) {
        ArrayList<MagazineChoice> result = new ArrayList<>();
        if (!(gun instanceof ISlottedGun)) return result;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return result;
        try {
            Builder builder = new Builder();
            builder.initAndClear(gun.getModulesTag(gunStack).copy());
            var root = ((WorkSpace) builder.getWorkspace()).getRoot();
            if (root == null) return result;
            var magSlot = root.getSlotByName("MAG");
            if (magSlot == null) return result;
            List<ItemStack> inventory = mc.player.getInventory().items;
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack candidate = inventory.get(slot);
                if (!(candidate.getItem() instanceof MagazineItem magazine)) continue;
                Mag mag = magazine.getModule();
                if (!magSlot.accepts(mag)) continue;
                int rounds = magazine.getRounds(candidate);
                if (rounds <= 0) continue;
                result.add(new MagazineChoice(slot, candidate.copy(), mag.getID(), rounds, magazine.capacity()));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static void onWheelClosed(String gunIdentity, MagazineChoice choice) {
        net.minecraft.client.KeyMapping.set(KeyBinds.RELOAD.getKey(), false);
        tapWindow = 0;
        secondTap = false;
        consumedHold = true;
        heldTicks = 0;
        wasDown = false;
        suppressUntilRelease = true;
        if (choice == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem weapon)) return;
        ItemStack gunStack = mc.player.getMainHandItem();
        IGun gun = weapon.getGun();
        if (!Objects.equals(gun.getIdentityID(gunStack), gunIdentity)) return;
        beginMagazine(gun, gunStack, PhysicalMagazinePacket.SELECTED_INSERT, choice);
    }
}
