package com.exetacticals.workbench;

public enum WorkbenchType {
    GUN("gun", 2, true),
    MAGAZINE("magazine", 2, false),
    ATTACHMENT("attachment", 2, false),
    AMMO("ammo", 1, false);

    public final String station;
    public final int width;
    public final boolean keepsOnTable;

    WorkbenchType(String station, int width, boolean keepsOnTable) {
        this.station = station;
        this.width = width;
        this.keepsOnTable = keepsOnTable;
    }
}
