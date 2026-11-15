package com.exetacticals.mixin;

import com.exetacticals.mixinUtils.DualHandItemAccessor;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin  implements DualHandItemAccessor {
    @Shadow
    private ItemStack mainHandItem;

    @Override
    public ItemStack getMainHandItem() {
        return mainHandItem;
    }

    @Override
    public void setMainHandItem(ItemStack stack) {
        this.mainHandItem = stack;
    }

}