package com.exetacticals.items.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/** Данные блока характеристик в тултипе; рисует его {@code client.tooltip.ClientStatTooltip}. */
public record StatTooltip(List<StatRow> rows) implements TooltipComponent {
}
