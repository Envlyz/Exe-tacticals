package com.exetacticals.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/** Меню оружейного верстака: слотов нет, ингредиенты берутся прямо из инвентаря игрока. */
public class GunWorkbenchMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Level level;

    public GunWorkbenchMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModWorkbench.MENU.get(), id);
        this.pos = pos;
        this.level = inventory.player.level();
    }

    /** Клиентская сторона: позиция приходит от сервера. */
    public GunWorkbenchMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    /** Вид верстака по блоку в {@link #pos} (меню одно на все верстаки). */
    public WorkbenchType type() {
        return level.getBlockState(pos).getBlock() instanceof GunWorkbenchBlock block ? block.type() : WorkbenchType.GUN;
    }

    /** Рецепты этого верстака, по категории и названию результата. */
    public List<GunCraftingRecipe> recipes() {
        String station = type().station;
        return level.getRecipeManager().getAllRecipesFor(ModWorkbench.GUN_CRAFTING_TYPE.get()).stream()
                .filter(r -> r.station().equals(station))
                .sorted(Comparator.comparing(GunCraftingRecipe::category)
                        .thenComparing(r -> r.result().getHoverName().getString()))
                .toList();
    }

    /** На столе лежит не забранное оружие — крафт запрещён (клиент: для кнопки). */
    public boolean tableOccupied() {
        GunWorkbenchBlockEntity table = GunWorkbenchBlock.blockEntity(level, pos);
        return table != null && table.isOccupied();
    }

    /**
     * Сервер: создать предмет по рецепту, если хватает ресурсов (в творческом — бесплатно) и стол свободен.
     * Результат кладётся на стол ({@link GunWorkbenchBlockEntity}), а не в инвентарь.
     */
    public void craft(Player player, net.minecraft.resources.ResourceLocation recipeId) {
        if (!stillValid(player)) {
            return;
        }
        Recipe<?> recipe = level.getRecipeManager().byKey(recipeId).orElse(null);
        WorkbenchType type = type();
        if (!(recipe instanceof GunCraftingRecipe gunRecipe) || !gunRecipe.station().equals(type.station)) {
            return;
        }
        GunWorkbenchBlockEntity table = GunWorkbenchBlock.blockEntity(level, pos);
        if (type.keepsOnTable && table == null) {
            return;
        }
        if (table != null && table.isOccupied()) {
            player.displayClientMessage(Component.translatable("gui.exe_tacticals.workbench.occupied_msg"), true);
            return;
        }
        Inventory inventory = player.getInventory();
        if (!player.isCreative()) {
            if (!gunRecipe.canCraft(inventory)) {
                return;
            }
            gunRecipe.consume(inventory);
        }
        // чистый новый предмет: результат рецепта мог получить данные оружия от предпросмотра в меню
        ItemStack result = new ItemStack(gunRecipe.result().getItem(), Math.max(1, gunRecipe.result().getCount()));
        if (type.keepsOnTable && table != null) {
            // оружие не выдаётся в инвентарь: ложится на стол, забирают клавишей F (TakeCraftedGunPacket)
            table.setCrafted(result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 1f);
        broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return level.getBlockState(pos).getBlock() instanceof GunWorkbenchBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) <= 64.0;
    }
}
