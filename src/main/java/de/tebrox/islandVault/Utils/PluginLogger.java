package de.tebrox.islandVault.Utils;

import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class PluginLogger {

    private static Logger logger;
    private static boolean debug;
    private static String prefix = "[IslandVault]";

    public static void init(Logger pluginLogger, boolean debugEnabled) {
        logger = pluginLogger;
        debug = debugEnabled;
    }

    public static void setDebugMode(boolean enabled) {
        debug = enabled;
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warning(String message) {
        logger.warning(message);
    }

    public static void error(String message) {
        logger.severe(message);
    }

    public static void debug(String message) {
        if (debug) {
            logger.info("[DEBUG] " + message);
        }
    }
}