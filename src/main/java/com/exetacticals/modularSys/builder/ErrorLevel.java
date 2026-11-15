package com.exetacticals.modularSys.builder;

public enum ErrorLevel {
    ERROR("error"),
    WARNING("warning");

    private final String name;

    ErrorLevel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
