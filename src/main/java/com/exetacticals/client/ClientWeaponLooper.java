package com.exetacticals.client;

import com.exetacticals.Client;
import com.exetacticals.GCR;
import com.exetacticals.client.recoil.IRecoilCameraHandler;
import com.exetacticals.client.recoil.RecoilCameraHandler;
import com.exetacticals.client.recoil.RecoilHandler;
import com.exetacticals.client.render.HardCodeAnimationHandler;
import com.exetacticals.client.render.IGlobalAnimationHandler;
import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

@OnlyIn(Dist.CLIENT)
public final class ClientWeaponLooper implements Runnable {

    private static final long UPDATE_PERIOD_NANOS = 10_000_000L;
    private static final float UPDATE_DELTA_SECONDS = 0.01f;

    private static final long SPIN_START_NANOS = 500_000L;
    private static final long SPIN_MIN_NANOS = 200_000L;
    private static final long SPIN_MAX_NANOS = 2_000_000L;
    private static final long SPIN_GROW_NANOS = 250_000L;
    private static final long SPIN_SHRINK_NANOS = 50_000L;

    private static final long RETRY_WAIT_NANOS = 5_000_000L;

    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private static volatile Thread worker;

    private long nextFireNanos = -1L;
    private boolean firePrecise;
    private long nextRecoilNanos;
    private long nextAnimationNanos;
    private long spinNanos = SPIN_START_NANOS;

    public static void wake() {
        LockSupport.unpark(worker);
    }

    @Override
    public void run() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread self = Thread.currentThread();
        self.setName("GCR-Weapon-Thread");
        try {
            self.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));
        } catch (Throwable ignored) {
        }
        worker = self;

        long now = System.nanoTime();
        nextRecoilNanos = now;
        nextAnimationNanos = now;

        while (true) {
            try {
                step();
            } catch (Throwable t) {
                GCR.LOGGER.error("[GCR] weapon thread step failed", t);
                LockSupport.parkNanos(RETRY_WAIT_NANOS);
            }
        }
    }

    private void step() {
        long now = System.nanoTime();

        updateFire(now);

        now = System.nanoTime();
        if (now - nextRecoilNanos >= 0L) {
            updateRecoil();
            nextRecoilNanos = advanceDeadline(nextRecoilNanos, UPDATE_PERIOD_NANOS, now);
        }
        if (now - nextAnimationNanos >= 0L) {
            updateAnimation();
            nextAnimationNanos = advanceDeadline(nextAnimationNanos, UPDATE_PERIOD_NANOS, now);
        }

        sleepUntil(nextDeadline(now));
    }

    private void updateFire(long now) {
        if (!fireWanted()) {
            nextFireNanos = -1L;
            firePrecise = false;
            return;
        }

        if (nextFireNanos < 0L) {
            nextFireNanos = now;
            firePrecise = true;
        }

        long remaining = nextFireNanos - now;

        if (remaining > (firePrecise ? spinNanos : 0L)) {
            return;
        }

        if (firePrecise) {
            spinUntil(nextFireNanos);
        }

        fireOnce();
    }

    private boolean fireWanted() {
        return !Client.CANCEL_WEAPON_LOOPER.get()
                && Client.LEFT_BUTTON_PRESSED.get()
                && !Client.WEAPON_STATUS.isFireBlocked();
    }

    private void fireOnce() {
        long planned = nextFireNanos;
        long interval = attemptShoot();
        long shootEnd = System.nanoTime();
        if (interval <= 0L) {
            nextFireNanos = shootEnd + RETRY_WAIT_NANOS;
            firePrecise = false;
            return;
        }
        long next = planned + interval;

        if (next <= shootEnd) {
            next = shootEnd + interval;
        }

        nextFireNanos = next;
        firePrecise = true;
    }

    private long attemptShoot() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0L;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            return 0L;
        }
        IGun gun = gunItem.getGun();
        if (gun == null) {
            return 0L;
        }
        return Client.handleClientShoot(stack, gun, player);
    }

    private void updateRecoil() {
        try {
            RecoilHandler.INSTANCE.update(UPDATE_DELTA_SECONDS);
            IRecoilCameraHandler cameraHandler = RecoilCameraHandler.getInstance();
            if (cameraHandler != null) {
                cameraHandler.update(UPDATE_DELTA_SECONDS);
            }
        } catch (Throwable t) {
            GCR.LOGGER.error("[GCR] recoil update failed", t);
        }
    }

    private void updateAnimation() {
        try {
            IGlobalAnimationHandler handler = HardCodeAnimationHandler.getInstance();
            if (handler != null) {
                handler.update(UPDATE_DELTA_SECONDS);
            }
        } catch (Throwable t) {
            GCR.LOGGER.error("[GCR] animation update failed", t);
        }
    }

    private static long advanceDeadline(long plannedNanos, long periodNanos, long now) {
        long next = plannedNanos + periodNanos;
        return next <= now ? now + periodNanos : next;
    }

    private long nextDeadline(long now) {
        long deadline = Math.min(nextRecoilNanos, nextAnimationNanos);
        if (nextFireNanos >= 0L) {
            long fireWake = firePrecise ? nextFireNanos - spinNanos : nextFireNanos;
            if (fireWake < deadline) {
                deadline = fireWake;
            }
        }
        return Math.max(deadline, now);
    }

    private static void spinUntil(long deadlineNanos) {
        while (System.nanoTime() - deadlineNanos < 0L) {
            Thread.onSpinWait();
        }
    }

    private void sleepUntil(long wakeAtNanos) {
        long wait = wakeAtNanos - System.nanoTime();
        if (wait <= 0L) {
            return;
        }
        LockSupport.parkNanos(wait);
        long overshoot = System.nanoTime() - wakeAtNanos;
        if (overshoot > 0L) {
            spinNanos = Math.min(SPIN_MAX_NANOS, Math.max(spinNanos, overshoot + SPIN_GROW_NANOS));
        } else if (overshoot < -SPIN_SHRINK_NANOS) {
            spinNanos = Math.max(SPIN_MIN_NANOS, spinNanos - SPIN_SHRINK_NANOS);
        }
    }
}
