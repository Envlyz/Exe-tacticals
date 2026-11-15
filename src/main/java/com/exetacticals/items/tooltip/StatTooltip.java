package com.exetacticals.items.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record StatTooltip(List<StatRow> rows) implements TooltipComponent {
}
