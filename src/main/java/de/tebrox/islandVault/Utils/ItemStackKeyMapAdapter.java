package de.tebrox.islandVault.Utils;


import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ItemStackKeyMapAdapter implements JsonSerializer<Map<ItemStackKey, Integer>>, JsonDeserializer<Map<ItemStackKey, Integer>> {

    @Override
    public JsonElement serialize(Map<ItemStackKey, Integer> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<ItemStackKey, Integer> entry : src.entrySet()) {
            // ItemStackKey als String (Base64) als Key
            obj.addProperty(entry.getKey().toString(), entry.getValue());
        }
        return obj;
    }

    @Override
    public Map<ItemStackKey, Integer> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<ItemStackKey, Integer> map = new HashMap<>();
        JsonObject obj = json.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String keyString = entry.getKey();
            ItemStackKey key = ItemStackKey.fromString(keyString); // Konstruktor ist privat, wir machen statisch:

            // Statt privatem Konstruktor => besser statische Factory-Methode hinzufügen:
            // ItemStackKey key = ItemStackKey.fromString(keyString);

            int value = entry.getValue().getAsInt();
            map.put(key, value);
        }
        return map;
    }
}