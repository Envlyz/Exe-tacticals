package com.exetacticals.client.stuck;

import com.exetacticals.items.GunItem;
import com.exetacticals.modularSys.modules.guns.IGun;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientGunStuckCache {

    public static final long PREDICTION_TIMEOUT_MS = 3000L;

    private static final int MAX_ENTRIES = 256;

    private static final ClientGunStuckCache INSTANCE = new ClientGunStuckCache();

    public static ClientGunStuckCache get() {
        return INSTANCE;
    }

    private static final class Entry {
        volatile boolean stuck;
        volatile boolean confirmed;
        volatile int shootId;
        volatile long stampMs;
        volatile boolean nbtWritten;
        volatile boolean needsNbtClear;
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private ClientGunStuckCache() {
    }

    public boolean isStuck(String gunId) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return false;
        }
        Entry entry = entries.get(gunId);
        return entry != null && entry.stuck;
    }

    public boolean isStuck(ItemStack stack, IGun gun) {
        if (gun.isStuck(stack)) {
            return true;
        }
        return isStuck(gun.getIdentityID(stack));
    }

    public void onLocalJam(String gunId, int shootId) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        long now = System.currentTimeMillis();
        entries.compute(gunId, (key, old) -> {
            Entry entry = old == null ? new Entry() : old;
            if (entry.stuck && entry.confirmed) {
                entry.shootId = Math.max(entry.shootId, shootId);
                return entry;
            }
            entry.stuck = true;
            entry.confirmed = false;
            entry.shootId = shootId;
            entry.stampMs = now;
            entry.nbtWritten = true;
            entry.needsNbtClear = false;
            return entry;
        });
        trimIfNeeded();
    }

    public void onLocalClearRequested(String gunId) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        entries.computeIfPresent(gunId, (key, entry) -> {
            if (entry.stuck && !entry.confirmed) {
                markCleared(entry);
            }
            return entry;
        });
    }

    public void onServerAck(String gunId, int shootId, boolean serverStuck, boolean fired) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (serverStuck) {
            entries.compute(gunId, (key, old) -> {
                Entry entry = old == null ? new Entry() : old;
                if (entry.stuck && entry.confirmed && entry.shootId > shootId) {
                    return entry;
                }
                entry.stuck = true;
                entry.confirmed = true;
                entry.shootId = shootId;
                entry.stampMs = now;
                entry.needsNbtClear = false;
                return entry;
            });
        } else {
            entries.computeIfPresent(gunId, (key, entry) -> {
                if (entry.shootId > shootId) {
                    return entry;
                }
                markCleared(entry);
                return entry;
            });
        }
        trimIfNeeded();
    }

    public void onServerSync(String gunId, boolean stuck) {
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        if (stuck) {
            long now = System.currentTimeMillis();
            entries.compute(gunId, (key, old) -> {
                Entry entry = old == null ? new Entry() : old;
                entry.stuck = true;
                entry.confirmed = true;
                entry.stampMs = now;
                entry.needsNbtClear = false;
                return entry;
            });
        } else {
            entries.computeIfPresent(gunId, (key, entry) -> {
                markCleared(entry);
                return entry;
            });
        }
        trimIfNeeded();
    }

    public void projectToNbt(ItemStack stack, IGun gun) {
        Entry entry = entryOf(stack, gun);
        if (entry == null) {
            return;
        }
        if (entry.stuck) {
            if (!gun.isStuck(stack)) {
                gun.setStuck(true, gun.rootNodeTag(stack));
            }
            entry.nbtWritten = true;
            return;
        }
        if (entry.needsNbtClear) {
            if (gun.isStuck(stack)) {
                gun.setStuck(false, gun.rootNodeTag(stack));
            }
            entry.needsNbtClear = false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tick(LocalPlayer player) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            if (entry.stuck && !entry.confirmed && now - entry.stampMs > PREDICTION_TIMEOUT_MS) {
                markCleared(entry);
            }
        }

        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            return;
        }
        IGun gun = gunItem.getGun();
        String gunId = gun.getIdentityID(stack);
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return;
        }
        Entry entry = entries.get(gunId);
        if (entry == null) {
            return;
        }
        projectToNbt(stack, gun);
        if (!entry.stuck && !entry.needsNbtClear) {
            entries.remove(gunId, entry);
        }
    }

    public void clearAll() {
        entries.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private Entry entryOf(ItemStack stack, IGun gun) {
        if (stack == null || stack.isEmpty() || gun == null) {
            return null;
        }
        String gunId = gun.getIdentityID(stack);
        if (gunId == null || IGun.NONE.equals(gunId)) {
            return null;
        }
        return entries.get(gunId);
    }

    private static void markCleared(Entry entry) {
        entry.stuck = false;
        entry.confirmed = false;
        entry.stampMs = System.currentTimeMillis();
        entry.needsNbtClear = entry.nbtWritten;
    }

    private void trimIfNeeded() {
        if (entries.size() <= MAX_ENTRIES) {
            return;
        }
        for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            if (!entry.stuck && !entry.needsNbtClear) {
                entries.remove(mapEntry.getKey(), entry);
                if (entries.size() <= MAX_ENTRIES) {
                    return;
                }
            }
        }
    }
}
