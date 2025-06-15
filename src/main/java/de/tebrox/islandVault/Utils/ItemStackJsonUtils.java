package de.tebrox.islandVault.Utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemStackJsonUtils {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JsonElement serializeItemStack(ItemStack item) {
        Map<String, Object> serialized = item.serialize();
        return GSON.toJsonTree(serialized);
    }

    public static ItemStack deserializeItemStack(JsonElement json) {
        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        return ItemStack.deserialize(map);
    }

    public static String toJsonString(JsonElement element) {
        return GSON.toJson(element);
    }

    public static JsonElement fromJsonString(String json) {
        return JsonParser.parseString(json);
    }
}