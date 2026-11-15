package com.exetacticals.modularSys.modules.gunProperties;

import net.minecraft.network.chat.Component;

public abstract class Prop implements IProp{
    protected String fullName = "";

    @Override
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String getFullNameKey() {
        return fullName;
    }

    @Override
    public String getFullName() {
        return Component.translatable(getFullNameKey()).getString();
    }
}
