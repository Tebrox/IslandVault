package de.tebrox.islandVault.Utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tebrox.islandVault.IslandVault;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class BentoBoxRanks {

    private static final Map<String, Integer> nameToId = new HashMap<>();
    private static final Map<Integer, String> idToName = new TreeMap<>();

    public static void loadRanks() {
        File file = new File("plugins/BentoBox/database/Ranks/BentoBox-Ranks.json");
        if (!file.exists()) {
            System.out.println("[BentoBoxRanks] BentoBox-Ranks.json nicht gefunden!");
            return;
        }

        List<String> blackListRanks = IslandVault.getPlugin().getConfig().getStringList("rank_blacklist");

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject ranks = json.getAsJsonObject("rankReference");

            for (String key : ranks.keySet()) {
                if(blackListRanks.contains(key.replace("ranks.", ""))) {
                    continue;
                }
                int value = ranks.get(key).getAsInt();
                String roleName = key.replace("ranks.", "").replace("-", " ");

                nameToId.put(roleName.toLowerCase(), value);
                idToName.put(value, roleName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getId(String roleName) {
        return nameToId.getOrDefault(roleName.toLowerCase(), -1);
    }

    public static String getName(int roleId) {
        return idToName.getOrDefault(roleId, "Unbekannt");
    }

    public static List<Integer> getSortedRoleIds() {
        return new ArrayList<>(idToName.keySet());
    }

    public static List<Integer> getSortedRoleIdsReversed() {
        List<Integer> reversed = new ArrayList<>(getSortedRoleIds());
        Collections.reverse(reversed);
        return reversed;
    }

    public static Map<Integer, String> getAllRoles() {
        return Collections.unmodifiableMap(idToName);
    }
}