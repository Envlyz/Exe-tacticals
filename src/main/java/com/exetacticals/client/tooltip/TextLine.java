package com.exetacticals.client.tooltip;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Строка текста тултипа, которую рисует MSDF-шрифт ({@link ClientTextLine}); {@code title} — название предмета. */
@OnlyIn(Dist.CLIENT)
public record TextLine(FormattedText text, boolean title) implements TooltipComponent {
}
