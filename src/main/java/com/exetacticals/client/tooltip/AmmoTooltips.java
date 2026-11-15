package com.exetacticals.client.tooltip;

import com.exetacticals.GCR;
import com.exetacticals.items.GunItem;
import com.exetacticals.items.MagazineItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Подсказки, связанные с зарядкой магазинов в инвентаре:
 * <ul>
 *   <li>патроны — в какие магазины заряжаются и как (магазин на курсоре → ПКМ по патронам);</li>
 *   <li>оружие — какие магазины к нему подходят и каким патроном.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class AmmoTooltips {
    private static final int MAX_LISTED = 5;

    private AmmoTooltips() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof GunItem) {
            appendGun(stack, event.getToolTip());
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !GCR.MODID.equals(id.getNamespace()) || stack.getItem() instanceof MagazineItem) {
            return;
        }
        List<MagazineItem> magazines = new ArrayList<>();
        for (MagazineItem mag : magazines()) {
            if (mag.acceptsCartridge(stack)) {
                magazines.add(mag);
            }
        }
        if (magazines.isEmpty()) {
            return;
        }
        List<Component> tip = event.getToolTip();
        tip.add(Component.translatable("tooltip.exe_tacticals.ammo_fits").withStyle(ChatFormatting.GRAY));
        listNames(tip, magazines.stream().map(m -> new ItemStack(m).getHoverName()).toList());
        tip.add(Component.translatable("tooltip.exe_tacticals.ammo_load_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendGun(ItemStack gun, List<Component> tip) {
        ResourceLocation gunId = ForgeRegistries.ITEMS.getKey(gun.getItem());
        if (gunId == null) {
            return;
        }
        List<Component> mags = new ArrayList<>();
        Set<Item> cartridges = new LinkedHashSet<>();
        for (MagazineItem mag : magazines()) {
            if (mag.compatibleGuns().contains(gunId.toString())) {
                mags.add(new ItemStack(mag).getHoverName());
                cartridges.addAll(mag.cartridges());
            }
        }
        if (mags.isEmpty()) {
            return;
        }
        // вставляем перед блоком управления магазином, если он есть
        int at = tip.size();
        for (int i = 0; i < tip.size(); i++) {
            if (tip.get(i).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                    && "tooltip.exe_tacticals.gun_reload_header".equals(tc.getKey())) {
                at = i;
                break;
            }
        }
        List<Component> block = new ArrayList<>();
        for (Item cartridge : cartridges) {
            block.add(Component.translatable("tooltip.exe_tacticals.gun_ammo",
                    new ItemStack(cartridge).getHoverName().copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
        }
        block.add(Component.translatable("tooltip.exe_tacticals.gun_magazines").withStyle(ChatFormatting.GRAY));
        listNames(block, mags);
        tip.addAll(at, block);
    }

    private static void listNames(List<Component> tip, List<Component> names) {
        int shown = Math.min(names.size(), MAX_LISTED);
        for (int i = 0; i < shown; i++) {
            tip.add(Component.literal("  \u2022 ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(names.get(i).copy().withStyle(ChatFormatting.GRAY)));
        }
        if (names.size() > shown) {
            tip.add(Component.translatable("tooltip.exe_tacticals.and_more", names.size() - shown)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static List<MagazineItem> magazines() {
        List<MagazineItem> list = new ArrayList<>();
        for (RegistryObject<Item> entry : GCR.ITEMS.getEntries()) {
            if (entry.get() instanceof MagazineItem mag) {
                list.add(mag);
            }
        }
        return list;
    }
}
