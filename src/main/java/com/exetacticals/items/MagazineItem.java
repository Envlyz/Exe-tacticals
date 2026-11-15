package com.exetacticals.items;

import com.exetacticals.GCR;
import com.exetacticals.modularSys.modules.Mag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * A physical magazine inventory item. The backing Mag module is retained
 * ONLY as GCR's internal attachment/render and ammo-source adapter.
 * Players load this item with real cartridges rather than configuring it
 * through the attachment UI. Each item owns its persistent round count.
 */
public final class MagazineItem extends ModuleItem<Mag> {
    public static final String ROUNDS_TAG = "aftermatch_mag_ammo";
    // Tracks round item IDs; old weapon-specific keys are normalized to
    // canonical caliber keys so shared magazines use just one ammo type.
    public static final String ROUND_TYPES_TAG = "aftermatch_mag_round_types";
    private final Mag magazine;

    public MagazineItem(Mag mag) {
        super(mag);
        this.magazine = mag;
    }

    public int getRounds(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains(ROUNDS_TAG, 3)
                ? Mth.clamp(nbt.getInt(ROUNDS_TAG), 0, magazine.getMaxCapacity()) : 0;
    }

    public int capacity() { return magazine.getMaxCapacity(); }
    public boolean addOne(ItemStack stack) {
        int count = getRounds(stack);
        if (count >= capacity()) return false;
        stack.getOrCreateTag().putInt(ROUNDS_TAG, count + 1);
        return true;
    }

    public void setRounds(ItemStack stack, int rounds) {
        stack.getOrCreateTag().putInt(ROUNDS_TAG, Mth.clamp(rounds, 0, capacity()));
    }


    /**
     * Legacy names survive in saved ItemStacks and magazine NBT.
     * Normalize every old round ID to its physical caliber.
     */
    public static String canonicalAmmoId(String id) {
        return switch (id) {
            case "exe_tacticals:m4a1_cartridge", "exe_tacticals:ak24_cartridge" -> "exe_tacticals:ammo_5_56x45";
            case "exe_tacticals:ak74m_cartridge" -> "exe_tacticals:ammo_5_45x39";
            case "exe_tacticals:mcx_spear_cartridge" -> "exe_tacticals:ammo_6_8x51";
            case "exe_tacticals:akmn_cartridge" -> "exe_tacticals:ammo_7_62x39";
            default -> id;
        };
    }

    public static CompoundTag normalizeRoundTypes(CompoundTag source) {
        CompoundTag normalized = new CompoundTag();
        for (String id : source.getAllKeys()) {
            int count = Math.max(0, source.getInt(id));
            if (count > 0) {
                String key = canonicalAmmoId(id);
                normalized.putInt(key, normalized.getInt(key) + count);
            }
        }
        return normalized;
    }

    /** Magazine contents are persisted under canonical caliber IDs. */
    public CompoundTag getRoundTypes(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag()
                : normalizeRoundTypes(tag.getCompound(ROUND_TYPES_TAG));
    }

    public void setRoundTypes(ItemStack stack, CompoundTag types) {
        stack.getOrCreateTag().put(ROUND_TYPES_TAG, normalizeRoundTypes(types));
    }

    /** Extra ledger entries are discarded after rounds have been fired in a gun. */
    public static CompoundTag trimRoundTypes(CompoundTag source, int remainingRounds) {
        // Merge old M4A1/AK-24 ammo ledgers into the same 5.56 caliber first.
        source = normalizeRoundTypes(source);
        CompoundTag result = new CompoundTag();
        int remaining = Math.max(0, remainingRounds);
        java.util.List<String> keys = new ArrayList<>(source.getAllKeys());
        keys.sort(KEEP_ORDER);
        for (String id : keys) {
            if (remaining <= 0) break;
            int count = Math.min(Math.max(0, source.getInt(id)), remaining);
            if (count > 0) {
                result.putInt(id, count);
                remaining -= count;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ трассеры

    public static final String TRACER_SUFFIX = "_tracer";

    public static boolean isTracer(String roundId) {
        return roundId != null && roundId.endsWith(TRACER_SUFFIX);
    }

    /**
     * Порядок «сверху вниз» в учёте патронов: при выстрелах/обрезке учёт теряет патроны с КОНЦА этого порядка.
     * Трассеры стоят первыми, значит расходуются последними — в смешанном магазине трассеры идут в конце
     * (сигнал, что магазин кончается).
     */
    public static final java.util.Comparator<String> KEEP_ORDER =
            java.util.Comparator.comparing((String id) -> !isTracer(id)).thenComparing(java.util.Comparator.naturalOrder());

    /**
     * Патрон с номером {@code index} в порядке расхода (0 — первый израсходованный) из учёта магазина;
     * номер ограничивается границами, {@code null} — учёт пуст.
     */
    public static String roundInFiringOrder(CompoundTag ledger, int index) {
        CompoundTag types = normalizeRoundTypes(ledger);
        java.util.List<String> keys = new ArrayList<>(types.getAllKeys());
        if (keys.isEmpty()) return null;
        keys.sort(KEEP_ORDER.reversed());
        int total = totalRounds(types);
        int i = Mth.clamp(index, 0, Math.max(0, total - 1));
        for (String k : keys) {
            i -= types.getInt(k);
            if (i < 0) return k;
        }
        return keys.get(keys.size() - 1);
    }

    public static int totalRounds(CompoundTag ledger) {
        int total = 0;
        for (String k : ledger.getAllKeys()) total += Math.max(0, ledger.getInt(k));
        return total;
    }

    /** Loads a real, type-identified cartridge into this physical magazine. */
    public boolean addOne(ItemStack stack, Item cartridge) {
        if (!addOne(stack)) return false;
        net.minecraft.resources.ResourceLocation id = ForgeRegistries.ITEMS.getKey(cartridge);
        if (id != null) {
            CompoundTag types = getRoundTypes(stack);
            String key = canonicalAmmoId(id.toString());
            types.putInt(key, types.getInt(key) + 1);
            setRoundTypes(stack, types);
        }
        return true;
    }

    /** Removes a single physical round, returning it as an inventory item. */
    public ItemStack unloadOne(ItemStack stack) {
        int count = getRounds(stack);
        if (count <= 0) return ItemStack.EMPTY;

        CompoundTag types = trimRoundTypes(getRoundTypes(stack), count);
        Item cartridge = Items.AIR;
        String used = null;
        // достаём верхний патрон — первый в порядке расхода
        java.util.List<String> keys = new ArrayList<>(types.getAllKeys());
        keys.sort(KEEP_ORDER.reversed());
        for (String id : keys) {
            if (types.getInt(id) <= 0) continue;
            Item candidate = resolveCartridge(id);
            if (candidate != Items.AIR && isCompatible(candidate)) {
                cartridge = candidate;
                used = id;
                break;
            }
        }
        if (cartridge == Items.AIR) cartridge = fallbackCartridge();
        if (cartridge == Items.AIR) return ItemStack.EMPTY;

        setRounds(stack, count - 1);
        if (used != null) {
            int next = types.getInt(used) - 1;
            if (next > 0) types.putInt(used, next);
            else types.remove(used);
        }
        setRoundTypes(stack, trimRoundTypes(types, count - 1));
        return new ItemStack(cartridge);
    }

    private Item resolveCartridge(String id) {
        return switch (canonicalAmmoId(id)) {
            case "exe_tacticals:ammo_5_56x45" -> GCR.AMMO_556X45_ITEM.get();
            case "exe_tacticals:ammo_5_45x39" -> GCR.AMMO_545X39_ITEM.get();
            case "exe_tacticals:ammo_6_8x51" -> GCR.AMMO_68X51_ITEM.get();
            case "exe_tacticals:ammo_7_62x39" -> GCR.AMMO_762X39_ITEM.get();
            case "exe_tacticals:ammo_5_56x45_tracer" -> GCR.AMMO_556X45_TRACER_ITEM.get();
            case "exe_tacticals:ammo_5_45x39_tracer" -> GCR.AMMO_545X39_TRACER_ITEM.get();
            case "exe_tacticals:ammo_6_8x51_tracer" -> GCR.AMMO_68X51_TRACER_ITEM.get();
            case "exe_tacticals:ammo_7_62x39_tracer" -> GCR.AMMO_762X39_TRACER_ITEM.get();
            default -> Items.AIR;
        };
    }

    private boolean isCompatible(Item item) {
        return item != Items.AIR && (item == fallbackCartridge() || item == tracerCartridge());
    }

    /** Трассирующий патрон этого калибра. */
    public Item tracerCartridge() {
        if (magazine.hasTag("5.56x45")) return GCR.AMMO_556X45_TRACER_ITEM.get();
        if (magazine.hasTag("5.45x39")) return GCR.AMMO_545X39_TRACER_ITEM.get();
        if (magazine.hasTag("6.8x51")) return GCR.AMMO_68X51_TRACER_ITEM.get();
        if (magazine.hasTag("7.62x39")) return GCR.AMMO_762X39_TRACER_ITEM.get();
        return Items.AIR;
    }

    /** Все патроны, которыми снаряжается магазин: обычный и трассирующий. */
    public List<Item> cartridges() {
        List<Item> out = new ArrayList<>();
        if (fallbackCartridge() != Items.AIR) out.add(fallbackCartridge());
        if (tracerCartridge() != Items.AIR) out.add(tracerCartridge());
        return out;
    }

    private Item fallbackCartridge() {
        if (magazine.hasTag("5.56x45")) return GCR.AMMO_556X45_ITEM.get();
        if (magazine.hasTag("5.45x39")) return GCR.AMMO_545X39_ITEM.get();
        if (magazine.hasTag("6.8x51")) return GCR.AMMO_68X51_ITEM.get();
        if (magazine.hasTag("7.62x39")) return GCR.AMMO_762X39_ITEM.get();
        return Items.AIR;
    }

    @Override
    public @NotNull java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        int rounds = getRounds(stack);
        return java.util.Optional.of(new com.exetacticals.items.tooltip.StatTooltip(List.of(
                new com.exetacticals.items.tooltip.StatRow("magazine", "rounds",
                        Component.translatable("tooltip.exe_tacticals.magazine_rounds_label"),
                        rounds / (float) Math.max(1, capacity()), rounds + " / " + capacity(),
                        com.exetacticals.items.tooltip.StatRow.Kind.FILL))));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        // Keep the original localized magazine name. Only magazines with at
        // least one round gain (+); empty magazines have no status suffix.
        Component name = super.getName(stack);
        return getRounds(stack) > 0
                ? Component.empty().append(name).append(" (+)")
                : name;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // что заряжено: тип патрона × количество (в порядке расхода)
        int loadedRounds = getRounds(stack);
        if (loadedRounds > 0) {
            CompoundTag loaded = trimRoundTypes(getRoundTypes(stack), loadedRounds);
            java.util.List<String> loadedKeys = new ArrayList<>(loaded.getAllKeys());
            loadedKeys.sort(KEEP_ORDER.reversed());
            int known = 0;
            tooltip.add(Component.translatable("tooltip.exe_tacticals.magazine_loaded").withStyle(ChatFormatting.GRAY));
            for (String id : loadedKeys) {
                int n = loaded.getInt(id);
                Item round = resolveCartridge(id);
                if (n <= 0 || round == Items.AIR) continue;
                known += n;
                tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(new ItemStack(round).getHoverName().copy()
                                .withStyle(isTracer(id) ? ChatFormatting.RED : ChatFormatting.YELLOW))
                        .append(Component.literal(" ×" + n).withStyle(ChatFormatting.WHITE)));
            }
            Item plain = fallbackCartridge();
            if (known < loadedRounds && plain != Items.AIR) {
                // старые магазины без учёта типов — считаем обычными патронами
                tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(new ItemStack(plain).getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" ×" + (loadedRounds - known)).withStyle(ChatFormatting.WHITE)));
            }
        }
        // патроны показываются полоской (ClientStatTooltip) — здесь только какие патроны подходят
        List<Item> cartridgeItems = cartridges();
        if (!cartridgeItems.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.exe_tacticals.magazine_ammo")
                    .withStyle(ChatFormatting.GRAY));
            for (Item cartridge : cartridgeItems)
                tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(new ItemStack(cartridge).getHoverName().copy().withStyle(ChatFormatting.GOLD)));
        }
        // Render each compatible firearm on its OWN line, instead of
        // a comma-separated inline string that clips long translated names.
        List<String> guns = compatibleGuns();
        if (!guns.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.exe_tacticals.compatible_weapons")
                    .withStyle(ChatFormatting.GRAY));
            for (String gunId : guns)
                tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable(gunId).withStyle(ChatFormatting.GRAY)));
        }
        tooltip.add(Component.translatable("tooltip.exe_tacticals.magazine_click_load")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.exe_tacticals.magazine_click_unload")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** id оружия, в которое вставляется этот магазин (по тегам модуля). */
    public List<String> compatibleGuns() {
        List<String> guns = new ArrayList<>();
        if (magazine.hasTag("mag") && magazine.hasTag("ar") && magazine.hasTag("5.56x45")) {
            guns.add("exe_tacticals:m4a1");
            guns.add("exe_tacticals:ak24");
        } else if (magazine.hasTag("mag") && magazine.hasTag("ak") && magazine.hasTag("5.45x39")) {
            guns.add("exe_tacticals:ak74m");
        } else if (magazine.hasTag("mag") && magazine.hasTag("ak") && magazine.hasTag("7.62x39")) {
            guns.add("exe_tacticals:akmn");
        } else if (magazine.hasTag("mag") && magazine.hasTag("mcx_spear") && magazine.hasTag("6.8x51")) {
            guns.add("exe_tacticals:mcx_spear");
        }
        return guns;
    }

    /** Патрон, которым снаряжается магазин (основной, не старые предметы). */
    public Item cartridge() {
        return fallbackCartridge();
    }

    // ------------------------------------------------------------------ полоска заполненности

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13f * getRounds(stack) / Math.max(1, capacity()));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        // как прочность: полный — зелёный, пустеет к красному
        float t = getRounds(stack) / (float) Math.max(1, capacity());
        return Mth.hsvToRgb(t / 3f, 0.85f, 1f);
    }

    // ------------------------------------------------------------------ зарядка в инвентаре

    /** Подходит ли патрон к этому магазину (с учётом старых предметов-патронов). */
    public boolean acceptsCartridge(ItemStack cartridge) {
        return !cartridge.isEmpty()
                && com.exetacticals.network.c2s.PhysicalMagazinePacket.isCompatibleCartridge(cartridge, magazine);
    }

    /**
     * Магазин на курсоре, ПКМ по слоту:
     * по подходящим патронам — магазин «всасывает» сколько влезет;
     * по пустому слоту (или слоту с теми же патронами) — патроны высыпаются туда.
     */
    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack magStack, @NotNull net.minecraft.world.inventory.Slot slot,
                                          @NotNull net.minecraft.world.inventory.ClickAction action,
                                          @NotNull net.minecraft.world.entity.player.Player player) {
        if (action != net.minecraft.world.inventory.ClickAction.SECONDARY || magStack.getCount() != 1) {
            return false;
        }
        ItemStack target = slot.getItem();
        if (acceptsCartridge(target)) {
            if (loadFrom(magStack, target.getCount(), n -> slot.safeTake(n, n, player)) > 0) {
                playLoad(player);
            }
            return true;
        }
        if (getRounds(magStack) <= 0) {
            return false;
        }
        if (target.isEmpty() || isUnloadTarget(magStack, target)) {
            ItemStack out = unloadInto(magStack, target);
            if (!out.isEmpty()) {
                if (target.isEmpty()) {
                    slot.safeInsert(out);
                } else {
                    target.grow(out.getCount());
                    slot.setChanged();
                }
                playUnload(player);
            }
            return true;
        }
        return false;
    }

    /** Патроны на курсоре, ПКМ по магазину в слоте — заряжает его патронами с курсора. */
    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack magStack, @NotNull ItemStack carried,
                                            @NotNull net.minecraft.world.inventory.Slot slot,
                                            @NotNull net.minecraft.world.inventory.ClickAction action,
                                            @NotNull net.minecraft.world.entity.player.Player player,
                                            @NotNull net.minecraft.world.entity.SlotAccess carriedAccess) {
        if (action != net.minecraft.world.inventory.ClickAction.SECONDARY || !slot.allowModification(player)
                || magStack.getCount() != 1 || !acceptsCartridge(carried)) {
            return false;
        }
        int loaded = loadFrom(magStack, carried.getCount(), n -> {
            ItemStack taken = carried.split(n);
            carriedAccess.set(carried);
            return taken;
        });
        if (loaded > 0) {
            slot.setChanged();
            playLoad(player);
        }
        return true;
    }

    /** Берёт до {@code available} патронов через {@code take} и заряжает их; возвращает сколько зарядил. */
    private int loadFrom(ItemStack magStack, int available, java.util.function.IntFunction<ItemStack> take) {
        int space = capacity() - getRounds(magStack);
        int n = Math.min(space, available);
        if (n <= 0) {
            return 0;
        }
        ItemStack taken = take.apply(n);
        int loaded = 0;
        for (int i = 0; i < taken.getCount(); i++) {
            if (addOne(magStack, taken.getItem())) {
                loaded++;
            }
        }
        return loaded;
    }

    private boolean isUnloadTarget(ItemStack magStack, ItemStack target) {
        if (target.getCount() >= target.getMaxStackSize()) {
            return false;
        }
        ItemStack probe = magStack.copy();
        ItemStack next = unloadOne(probe);
        return !next.isEmpty() && ItemStack.isSameItemSameTags(next, target);
    }

    /** Достаёт патроны одного типа, пока помещаются в стопку {@code target} (пустая — до полной стопки). */
    private ItemStack unloadInto(ItemStack magStack, ItemStack target) {
        ItemStack out = ItemStack.EMPTY;
        int room = target.isEmpty() ? 64 : target.getMaxStackSize() - target.getCount();
        while (room > 0 && getRounds(magStack) > 0) {
            ItemStack probe = magStack.copy();
            ItemStack next = unloadOne(probe);
            if (next.isEmpty()) {
                break;
            }
            boolean fits = out.isEmpty()
                    ? target.isEmpty() || ItemStack.isSameItemSameTags(next, target)
                    : ItemStack.isSameItemSameTags(next, out);
            if (!fits) {
                break;
            }
            unloadOne(magStack);
            if (out.isEmpty()) {
                out = next;
                room = Math.min(room, next.getMaxStackSize());
            } else {
                out.grow(1);
            }
            room--;
        }
        return out;
    }

    private static void playLoad(net.minecraft.world.entity.player.Player player) {
        player.playSound(com.exetacticals.sound.ModSounds.MAG_ROUND_LOAD.get(), 0.8f, 1.0f);
    }

    private static void playUnload(net.minecraft.world.entity.player.Player player) {
        player.playSound(com.exetacticals.sound.ModSounds.MAG_ROUND_UNLOAD.get(), 0.9f, 1.0f);
    }
}
