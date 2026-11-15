package com.exetacticals.items.tooltip;

import net.minecraft.network.chat.Component;

import java.util.Set;

public record StatRow(String group, String key, Component name, float value, String text, Kind kind) {
    public enum Kind { ABSOLUTE, DELTA, FILL }

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
