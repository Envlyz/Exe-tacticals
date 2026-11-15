package com.exetacticals.items;

import com.exetacticals.GCR;
import com.exetacticals.modularSys.IModular;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class ModuleItem<T extends IModular> extends NoRepairNoEnchantmentItem {
    private final T module;

    public ModuleItem(T module) {
        super(new Properties().stacksTo(1));
        this.module = module;
        boolean bind = this.module.bindItem(this);
        if (!bind) {
            if (GCR.IS_DEVELOPMENT) {
                throw new RuntimeException("Module " + module.getID() + " failed to bind to item " + this.getDescription().getString());
            } else {
                GCR.LOGGER.warn("Module {} failed to bind to item {}", module.getID(), this.getDescription().getString());
            }
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.Level context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        module.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // Specific receiver/stock/handguard/module compatibility only;
        // generic rail attachments intentionally have no firearm list.
        AttachmentCompatibilityTooltip.append(module, tooltipComponents);
    }

    /** Обвесы и магазины рисуются 3D-моделью модуля (GunItem переопределяет это своим рендером). */
    @Override
    public void initializeClient(@NotNull java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(ModuleItemClientExtensions.INSTANCE);
    }

    /** Характеристики полосками — отдельный блок тултипа сразу под названием. */
    @Override
    public @NotNull java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        List<com.exetacticals.items.tooltip.StatRow> rows = new java.util.ArrayList<>();
        try {
            module.appendStatRows(stack, rows);
        } catch (RuntimeException e) {
            GCR.LOGGER.debug("stat rows failed for {}", module.getID(), e);
        }
        return rows.isEmpty() ? java.util.Optional.empty()
                : java.util.Optional.of(new com.exetacticals.items.tooltip.StatTooltip(List.copyOf(rows)));
    }

    public T getModule() {
        return module;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return module.getID();
    }
}
