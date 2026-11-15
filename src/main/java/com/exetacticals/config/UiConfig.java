package com.exetacticals.config;

import com.exetacticals.GCR;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Locale;

/**
 * Настройки интерфейса мода ({@code config/exe_tacticals-ui-client.toml}).
 * Акцентный цвет пишется как HEX ({@code #009091}, {@code 009091}, {@code #099}) или RGB
 * ({@code 0,144,145}, {@code rgb(0, 144, 145)}).
 */
public final class UiConfig {
    public static final String DEFAULT_ACCENT = "#009091";
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> ACCENT_COLOR;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("ui");
        ACCENT_COLOR = b.comment("Accent color of the mod's UI (menus, tooltips, bars).",
                        "HEX: \"#009091\", \"009091\", \"#099\"; RGB: \"0,144,145\" or \"rgb(0, 144, 145)\".")
                .define("accentColor", DEFAULT_ACCENT);
        b.pop();
        SPEC = b.build();
    }

    private UiConfig() {
    }

    /** Разбор цвета: HEX (3 или 6 знаков, с # или без) или RGB (0–255 через запятую/пробел). Ошибка — {@code fallback}. */
    public static int parseColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (v.startsWith("rgb")) {
                v = v.substring(v.indexOf('(') + 1, v.lastIndexOf(')'));
            }
            if (v.contains(",") || v.trim().contains(" ")) {
                String[] parts = v.split("[,\\s]+");
                if (parts.length != 3) {
                    return fallback;
                }
                int r = clamp(Integer.parseInt(parts[0].trim()));
                int g = clamp(Integer.parseInt(parts[1].trim()));
                int bl = clamp(Integer.parseInt(parts[2].trim()));
                return (r << 16) | (g << 8) | bl;
            }
            if (v.startsWith("#")) {
                v = v.substring(1);
            } else if (v.startsWith("0x")) {
                v = v.substring(2);
            }
            if (v.length() == 3) {
                v = "" + v.charAt(0) + v.charAt(0) + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2);
            }
            if (v.length() != 6) {
                return fallback;
            }
            return Integer.parseInt(v, 16);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clamp(int c) {
        return Math.max(0, Math.min(255, c));
    }

    /** Цвет из конфига в виде RGB; при ошибке — стандартный. */
    public static int accentRgb() {
        int fallback = parseColor(DEFAULT_ACCENT, 0x009091);
        int rgb = parseColor(ACCENT_COLOR.get(), -1);
        if (rgb < 0) {
            GCR.LOGGER.warn("[UiConfig] can't parse accentColor '{}', using {}", ACCENT_COLOR.get(), DEFAULT_ACCENT);
            return fallback;
        }
        return rgb;
    }

    /** Мод-шина: применить цвет при загрузке и изменении файла (только для своего конфига). */
    public static void onConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            AccentHook.apply(accentRgb());
        }
    }

    /** Клиентская часть подключается из {@code GCR}: применяет цвет к интерфейсу. */
    public static final class AccentHook {
        public static java.util.function.IntConsumer target = rgb -> {
        };

        private AccentHook() {
        }

        static void apply(int rgb) {
            target.accept(rgb);
        }
    }
}
