package com.exetacticals.workbench;

/**
 * Вид верстака: какие рецепты он показывает ({@link GunCraftingRecipe#station()}), его ширина и куда идёт результат.
 *
 * <ul>
 *     <li>{@link #GUN} — оружейный, 2×2: оружие ложится на стол, забирают клавишей F;</li>
 *     <li>{@link #MAGAZINE} — для магазинов, 2×2;</li>
 *     <li>{@link #ATTACHMENT} — для обвесов, 2×2;</li>
 *     <li>{@link #AMMO} — для патронов, 1 в ширину × 2 в высоту.</li>
 * </ul>
 * У трёх последних результат сразу в инвентарь (не влезло — выпадает под ноги).
 */
public enum WorkbenchType {
    GUN("gun", 2, true),
    MAGAZINE("magazine", 2, false),
    ATTACHMENT("attachment", 2, false),
    AMMO("ammo", 1, false);

    /** значение поля {@code "station"} в рецепте */
    public final String station;
    /** ширина в блоках (вправо от главной части, если смотреть на верстак); высота у всех 2 */
    public final int width;
    /** результат кладётся на стол ({@link GunWorkbenchBlockEntity}), а не в инвентарь */
    public final boolean keepsOnTable;

    WorkbenchType(String station, int width, boolean keepsOnTable) {
        this.station = station;
        this.width = width;
        this.keepsOnTable = keepsOnTable;
    }
}
