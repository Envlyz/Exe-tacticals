package com.exetacticals;

import com.google.gson.JsonObject;

public interface IJsonSync {
    void writeToJson(JsonObject jsonObject);
    void loadFromJson(JsonObject jsonObject);
}
