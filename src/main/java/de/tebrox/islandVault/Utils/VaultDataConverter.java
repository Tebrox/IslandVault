package de.tebrox.islandVault.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tebrox.islandVault.VaultData;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;
import java.util.UUID;

public class VaultDataConverter {

    private final File vaultFolder;

    public VaultDataConverter(JavaPlugin plugin) {
        this.vaultFolder = new File(plugin.getDataFolder(), "vaults");
    }

    public boolean convertIfOldFormat(String ownerName, String islandId, File yamlFile) {
        if (!yamlFile.exists()) return true;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(yamlFile);
            String key = "items";
            if (!config.contains(key)) return false;

            Map<String, Object> rawItems = config.getConfigurationSection(key).getValues(false);
            if (rawItems == null) return false;

            VaultData data = new VaultData(islandId, ownerName);
            for (Map.Entry<String, Object> entry : rawItems.entrySet()) {
                Material material = Material.matchMaterial(entry.getKey());
                if (material == null) continue;

                int amount = ((Number) entry.getValue()).intValue();
                if (amount <= 0) continue;

                ItemStack stack = new ItemStack(material);
                data.add(stack, amount);
            }

            File jsonFile = new File(vaultFolder, ownerName + "_" + islandId + ".json");
            try (Writer writer = new FileWriter(jsonFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(data, writer);
            }

            // Alte Datei löschen
            if (!yamlFile.delete()) {
                yamlFile.renameTo(new File(yamlFile.getParentFile(), yamlFile.getName() + ".old"));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}