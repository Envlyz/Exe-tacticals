package com.exetacticals.client.render.fx;

import com.exetacticals.modularSys.ModuleRegister;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.modules.impl.Muzzle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Насколько сильна вспышка выстрела в зависимости от дульного устройства.
 * Свет только визуальный (см. {@link GunfireLights}), в мир ничего не ставится.
 *
 * <ul>
 *   <li>{@code envRadius}/{@code envStrength} — свет на окружении (радиус в блоках, сила);</li>
 *   <li>{@code modelMultiplier} — подсветка самой модели оружия в руках.</li>
 * </ul>
 */
public enum MuzzleFlashLightProfile {
    OPEN(7.0f, 1.4f, 1.0f),
    FLASH_HIDER(3.5f, 0.45f, 0.35f),
    SUPPRESSOR(1.5f, 0.15f, 0.1f);

    private final float envRadius;
    private final float envStrength;
    private final float modelMultiplier;

    MuzzleFlashLightProfile(float envRadius, float envStrength, float modelMultiplier) {
        this.envRadius = envRadius;
        this.envStrength = envStrength;
        this.modelMultiplier = modelMultiplier;
    }

    public float envRadius() {
        return envRadius;
    }

    public float envStrength() {
        return envStrength;
    }

    public float modelMultiplier() {
        return modelMultiplier;
    }

    /**
     * Read equipped muzzle modules rather than sound type: a flash hider
     * normally has the same unsuppressed firing sound as an open barrel.
     */
    public static MuzzleFlashLightProfile forGun(IGun gun, ItemStack stack) {
        if (gun == null || stack == null || stack.isEmpty()) return OPEN;

        boolean flashHider = false;
        boolean suppressor = false;
        ListTag modules = gun.getModulesTag(stack);
        for (int i = 0; i < modules.size(); i++) {
            CompoundTag tag = modules.getCompound(i);
            Muzzle muzzle = ModuleRegister.get(tag.getString(Unit.MODULE_ID), Muzzle.class);
            if (muzzle == null) continue;

            if (muzzle.isSuppressor()) {
                suppressor = true;
            } else {
                String id = muzzle.getID().toLowerCase(Locale.ROOT);
                // Include the source mod's historical "flash_hinder" spelling.
                if (id.contains("flash_hider") || id.contains("flash_hinder")
                        || id.contains("flash_suppressor")) {
                    flashHider = true;
                }
            }
        }

        // пламегаситель гасит вспышку сильно, но не до нуля
        if (flashHider) return FLASH_HIDER;
        if (suppressor) return SUPPRESSOR;
        return OPEN;
    }
}
