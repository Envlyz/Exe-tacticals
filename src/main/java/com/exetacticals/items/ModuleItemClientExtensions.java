package com.exetacticals.items;

import com.exetacticals.client.render.ModuleItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

@OnlyIn(Dist.CLIENT)
public class ModuleItemClientExtensions implements IClientItemExtensions {
    public static final ModuleItemClientExtensions INSTANCE = new ModuleItemClientExtensions();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return ModuleItemRenderer.get();
    }
}
