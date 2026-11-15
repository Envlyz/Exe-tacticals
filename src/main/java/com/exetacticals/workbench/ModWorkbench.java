package com.exetacticals.workbench;

import com.exetacticals.GCR;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorkbench {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GCR.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, GCR.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, GCR.MODID);

    public static final RegistryObject<Block> BLOCK = block("gun_workbench", WorkbenchType.GUN);
    public static final RegistryObject<Item> ITEM = item("gun_workbench", BLOCK);
    public static final RegistryObject<Block> MAGAZINE_BLOCK = block("magazine_workbench", WorkbenchType.MAGAZINE);
    public static final RegistryObject<Item> MAGAZINE_ITEM = item("magazine_workbench", MAGAZINE_BLOCK);
    public static final RegistryObject<Block> ATTACHMENT_BLOCK = block("attachment_workbench", WorkbenchType.ATTACHMENT);
    public static final RegistryObject<Item> ATTACHMENT_ITEM = item("attachment_workbench", ATTACHMENT_BLOCK);
    public static final RegistryObject<Block> AMMO_BLOCK = block("ammo_workbench", WorkbenchType.AMMO);
    public static final RegistryObject<Item> AMMO_ITEM = item("ammo_workbench", AMMO_BLOCK);

    public static final java.util.List<RegistryObject<Item>> ALL_ITEMS = java.util.List.of(ITEM, MAGAZINE_ITEM, ATTACHMENT_ITEM, AMMO_ITEM);

    private static RegistryObject<Block> block(String name, WorkbenchType type) {
        return GCR.BLOCKS.register(name, () -> new GunWorkbenchBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5f, 6f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(), type));
    }

    private static RegistryObject<Item> item(String name, RegistryObject<Block> block) {
        return GCR.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static final RegistryObject<MenuType<GunWorkbenchMenu>> MENU = MENUS.register("gun_workbench",
            () -> IForgeMenuType.create(GunWorkbenchMenu::new));

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GCR.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<GunWorkbenchBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("gun_workbench", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(GunWorkbenchBlockEntity::new, BLOCK.get()).build(null));

    public static final RegistryObject<RecipeType<GunCraftingRecipe>> GUN_CRAFTING_TYPE = RECIPE_TYPES.register("gun_crafting",
            () -> RecipeType.simple(GCR.RL(GCR.MODID, "gun_crafting")));

    public static final RegistryObject<RecipeSerializer<GunCraftingRecipe>> GUN_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("gun_crafting", GunCraftingRecipe.Serializer::new);

    private ModWorkbench() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
