package de.tebrox.islandVault.Utils;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ConfigUpdater {

    private final File configFile;
    private final ConfigurationLoader<CommentedConfigurationNode> loader;

    public ConfigUpdater(File configFile) {
        this.configFile = configFile;
        this.loader = YamlConfigurationLoader.builder()
                .path(configFile.toPath())
                .build();
    }

    public void update(InputStream defaultConfigStream) throws IOException {
        if (!configFile.exists()) {
            // Config existiert noch nicht → Default einfach kopieren (inkl. Kommentare)
            Files.createDirectories(configFile.getParentFile().toPath());
            Files.copy(defaultConfigStream, configFile.toPath());
            return;
        }

        // Bestehende Config laden
        CommentedConfigurationNode current = loader.load();

        // Default Config laden
        ConfigurationLoader<CommentedConfigurationNode> defaultLoader = YamlConfigurationLoader.builder()
                .source(() -> new BufferedReader(new InputStreamReader(defaultConfigStream)))
                .build();
        CommentedConfigurationNode defaults = defaultLoader.load();

        // Vorherige Kopie für Vergleich
        CommentedConfigurationNode beforeMerge = current.copy();

        // Fehlende Keys + Kommentare ergänzen
        mergeDefaults(current, defaults);

        // Prüfen, ob sich was geändert hat
        if (!nodesEqual(beforeMerge, current)) {
            createBackup();
            loader.save(current);
        }
    }

    private void mergeDefaults(ConfigurationNode current, ConfigurationNode defaults) {
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : defaults.childrenMap().entrySet()) {
            Object key = entry.getKey();
            ConfigurationNode defaultNode = entry.getValue();
            ConfigurationNode currentNode = current.node(key);

            if (currentNode.virtual()) {
                copyNode(currentNode, defaultNode);
            } else {
                if (defaultNode instanceof CommentedConfigurationNode defC && currentNode instanceof CommentedConfigurationNode curC) {
                    if (curC.comment() == null && defC.comment() != null) {
                        curC.comment(defC.comment());
                    }
                }
                if (defaultNode.isMap() || defaultNode.isList()) {
                    mergeDefaults(currentNode, defaultNode);
                }
            }
        }
    }

    private void copyNode(ConfigurationNode target, ConfigurationNode source) {
        if (source.isMap() || source.isList()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : source.childrenMap().entrySet()) {
                Object key = entry.getKey();
                ConfigurationNode sourceChild = entry.getValue();
                ConfigurationNode targetChild = target.node(key);
                copyNode(targetChild, sourceChild);
            }
        } else {
            try {
                target.set(source.raw());
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        if (source instanceof CommentedConfigurationNode srcC &&
                target instanceof CommentedConfigurationNode tgtC) {
            if (tgtC.comment() == null && srcC.comment() != null) {
                tgtC.comment(srcC.comment());
            }
        }
    }

    private boolean nodesEqual(ConfigurationNode a, ConfigurationNode b) {
        if (!Objects.equals(a.raw(), b.raw())) {
            return false;
        }

        if (a.childrenMap().size() != b.childrenMap().size()) {
            return false;
        }

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : a.childrenMap().entrySet()) {
            Object key = entry.getKey();
            ConfigurationNode childA = entry.getValue();
            ConfigurationNode childB = b.node(key);
            if (!nodesEqual(childA, childB)) {
                return false;
            }
        }
        return true;
    }

    private void createBackup() throws IOException {
        File backupFile = new File(configFile.getParentFile(), configFile.getName() + ".backup");
        int counter = 1;
        while (backupFile.exists()) {
            backupFile = new File(configFile.getParentFile(), configFile.getName() + ".backup" + counter);
            counter++;
        }
        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        //System.out.println("Config-Backup erstellt: " + backupFile.getName());
    }
}