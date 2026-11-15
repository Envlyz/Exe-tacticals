package com.exetacticals.workbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Рецепт оружейного верстака: результат + список «ингредиент × количество» (без сетки).
 * Лежит в датапаке {@code data/<ns>/recipes/...json} с {@code "type": "exe_tacticals:gun_crafting"}:
 * <pre>
 * { "type": "exe_tacticals:gun_crafting", "station": "gun", "category": "rifle",
 *   "result": { "item": "exe_tacticals:m4a1" },
 *   "ingredients": [ { "item": "minecraft:iron_ingot", "count": 20 }, { "tag": "forge:dusts/redstone", "count": 6 } ] }
 * </pre>
 * {@code station} — верстак ({@link WorkbenchType#station}: gun, magazine, attachment, ammo), по умолчанию gun.
 * Рецепты синхронизируются на клиент обычным менеджером рецептов.
 */
public class GunCraftingRecipe implements Recipe<Container> {
    public record Entry(Ingredient ingredient, int count) {}

    private final ResourceLocation id;
    private final String category;
    /** на каком верстаке крафтится: {@link WorkbenchType#station} ({@code gun} по умолчанию) */
    private final String station;
    private final ItemStack result;
    private final List<Entry> entries;

    public GunCraftingRecipe(ResourceLocation id, String station, String category, ItemStack result, List<Entry> entries) {
        this.id = id;
        this.station = station;
        this.category = category;
        this.result = result;
        this.entries = List.copyOf(entries);
    }

    public String category() {
        return category;
    }

    public String station() {
        return station;
    }

    public ItemStack result() {
        return result;
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Сколько предметов, подходящих под {@code ingredient}, есть в основном инвентаре и второй руке. */
    public static int count(Inventory inventory, Ingredient ingredient) {
        int n = 0;
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                n += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    public boolean canCraft(Inventory inventory) {
        for (Entry entry : entries) {
            if (count(inventory, entry.ingredient()) < entry.count()) {
                return false;
            }
        }
        return true;
    }

    /** Забирает ингредиенты; вызывать только после {@link #canCraft}. */
    public void consume(Inventory inventory) {
        for (Entry entry : entries) {
            int left = entry.count();
            left = take(inventory.items, entry.ingredient(), left);
            take(inventory.offhand, entry.ingredient(), left);
        }
        inventory.setChanged();
    }

    private static int take(List<ItemStack> stacks, Ingredient ingredient, int left) {
        for (ItemStack stack : stacks) {
            if (left <= 0) {
                break;
            }
            if (!stack.isEmpty() && ingredient.test(stack)) {
                int n = Math.min(left, stack.getCount());
                stack.shrink(n);
                left -= n;
            }
        }
        return left;
    }

    // ---------------------------------------------------------------- Recipe

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return false; // крафтится только из меню верстака
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) {
        return result;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean isSpecial() {
        return true; // не показывать в книге рецептов
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModWorkbench.GUN_CRAFTING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModWorkbench.GUN_CRAFTING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<GunCraftingRecipe> {
        @Override
        public @NotNull GunCraftingRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            String station = GsonHelper.getAsString(json, "station", WorkbenchType.GUN.station);
            String category = GsonHelper.getAsString(json, "category", "rifle");
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            List<Entry> entries = new ArrayList<>();
            JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                int count = Math.max(1, GsonHelper.getAsInt(obj, "count", 1));
                JsonObject ingredientJson = obj.deepCopy();
                ingredientJson.remove("count");
                entries.add(new Entry(Ingredient.fromJson(ingredientJson), count));
            }
            return new GunCraftingRecipe(id, station, category, result, entries);
        }

        @Override
        public GunCraftingRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf buf) {
            String station = buf.readUtf();
            String category = buf.readUtf();
            ItemStack result = buf.readItem();
            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(new Entry(Ingredient.fromNetwork(buf), buf.readVarInt()));
            }
            return new GunCraftingRecipe(id, station, category, result, entries);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, GunCraftingRecipe recipe) {
            buf.writeUtf(recipe.station);
            buf.writeUtf(recipe.category);
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.entries.size());
            for (Entry entry : recipe.entries) {
                entry.ingredient().toNetwork(buf);
                buf.writeVarInt(entry.count());
            }
        }
    }
}
