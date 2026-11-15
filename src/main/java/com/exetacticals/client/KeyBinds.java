package com.exetacticals.client;

import com.exetacticals.GCR;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;


@OnlyIn(Dist.CLIENT)
public class KeyBinds {
    public static final KeyMapping SWITCH_FIRE_MODE = new KeyMapping("key.exe_tacticals.switch_fire_mode", 86, "keys.categories.exe_tacticals");
    public static final KeyMapping RELOAD = new KeyMapping("key.exe_tacticals.reload", 82, "keys.categories.exe_tacticals");
    public static final KeyMapping OPEN_GUN_MODIFY_SCREEN = new KeyMapping("key.exe_tacticals.open_gun_modify_screen", 71, "keys.categories.exe_tacticals");

    public static final KeyMapping CHECK_MAG = new KeyMapping("key.exe_tacticals.check_mag", 78, "keys.categories.exe_tacticals");
    public static final KeyMapping CHECK_CHAMBER = new KeyMapping("key.exe_tacticals.check_chamber", 77, "keys.categories.exe_tacticals");


    public static final KeyMapping SWITCH_EFFECTIVE_SIGHT = new KeyMapping("key.exe_tacticals.switch_effective_sight", 89, "keys.categories.exe_tacticals");
    public static final KeyMapping USE_GRENADE_LAUNCHER = new KeyMapping("key.exe_tacticals.use_grenade_launcher", 342, "keys.categories.exe_tacticals");
    public static final KeyMapping TURN_FLASHLIGHT = new KeyMapping("key.exe_tacticals.turn_flashlight", 75, "keys.categories.exe_tacticals");

    public static final KeyMapping OPEN_DISPLAY_ADJUST_SCREEN = new KeyMapping("key.exe_tacticals.open_display_adjust_screen", 72, "keys.categories.exe_tacticals");


    public static final KeyMapping REMOVE_STUCK = new KeyMapping("key.exe_tacticals.remove_stuck", 85, "keys.categories.exe_tacticals");


    public static final KeyMapping CHECK_SUB_WEAPON = new KeyMapping("key.exe_tacticals.check_sub_weapon", 73, "keys.categories.exe_tacticals");

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SWITCH_FIRE_MODE);
        event.register(RELOAD);
        event.register(OPEN_GUN_MODIFY_SCREEN);
        event.register(CHECK_MAG);
        event.register(CHECK_CHAMBER);
        event.register(SWITCH_EFFECTIVE_SIGHT);
        event.register(USE_GRENADE_LAUNCHER);
        event.register(CHECK_SUB_WEAPON);
        event.register(TURN_FLASHLIGHT);

        if (GCR.IS_DEVELOPMENT) {
            event.register(OPEN_DISPLAY_ADJUST_SCREEN);
            event.register(REMOVE_STUCK);
        }
    }
}
