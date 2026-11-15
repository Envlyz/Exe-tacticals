package com.exetacticals.items.tooltip;

import net.minecraft.network.chat.Component;

import java.util.Set;

/**
 * Одна характеристика для тултипа-полосок.
 *
 * <ul>
 *   <li>{@link Kind#ABSOLUTE} — значение (у оружия: скорострельность, урон…); полоса — доля от лучшего среди того же {@code group}.</li>
 *   <li>{@link Kind#DELTA} — изменение в процентах (у обвесов); полоса от центра, зелёная если это улучшение.</li>
 * </ul>
 *
 * @param group  с чем сравнивать абсолютные значения: {@code "gun"} или {@code "module"}
 * @param key    ключ свойства ({@code rpm}, {@code spread}…), по нему решается, что лучше — больше или меньше
 * @param value  абсолютное значение или изменение в процентах (12.5 = +12.5%)
 * @param text   готовый текст значения справа
 */
public record StatRow(String group, String key, Component name, float value, String text, Kind kind) {
    /** FILL — доля 0..1 (патроны в магазине), полоса цвета «как прочность». */
    public enum Kind { ABSOLUTE, DELTA, FILL }

    /** Свойства, у которых меньше — лучше. */
    private static final Set<String> LOWER_IS_BETTER = Set.of(
            "weight", "spread", "impulse", "stuck_rate", "max_stuck_rate", "shoot_heat_inc", "sound_range");

    public static StatRow absolute(String group, String key, String name, float value, String text) {
        return new StatRow(group, key, Component.literal(name), value, text, Kind.ABSOLUTE);
    }

    public static StatRow delta(String key, Component name, float percent) {
        String text = (percent > 0 ? "+" : "") + format(percent) + "%";
        return new StatRow("module", key, name, percent, text, Kind.DELTA);
    }

    public boolean higherIsBetter() {
        return !LOWER_IS_BETTER.contains(key);
    }

    /** Для изменения: улучшает ли оно оружие. */
    public boolean isImprovement() {
        return (value > 0) == higherIsBetter();
    }

    private static String format(float v) {
        float abs = Math.abs(v);
        if (Math.abs(abs - Math.round(abs)) < 0.05f) {
            return String.valueOf(Math.round(v));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
