package pl.skyrise.skyRiseJobs.core;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;

import java.io.File;
import java.util.Locale;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseJobs.utils.CustomConfig;

public final class ModuleSupport {
    private ModuleSupport() {}

    public static void bindExecutor(JavaPlugin plugin, CommandExecutor executor, String... commands) {
        if (plugin == null || executor == null || commands == null) return;
        for (String name : commands) {
            PluginCommand command = plugin.getCommand(name);
            if (command != null) command.setExecutor(executor);
        }
    }

    public static void bindDisabled(JavaPlugin plugin, String module, String... commands) {
        bindExecutor(plugin, new DisabledCommandExecutor(module), commands);
    }

    public static <T extends Listener> T registerListener(JavaPlugin plugin, T listener) {
        if (plugin != null && listener != null) plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        return listener;
    }

    public static void unregisterListener(Listener listener) {
        if (listener != null) HandlerList.unregisterAll(listener);
    }

    public static void unregisterTabs(TabRegistry registry, String... commands) {
        if (registry == null || commands == null) return;
        for (String command : commands) registry.unregister(command);
    }

    public static void saveConfig(CustomConfig config) {
        if (config != null) config.save();
    }

    /**
     * Folder runtime dla plików pracy/modułu innych niż główny config.
     * Docelowo trzymamy wszystko modułu w jednym miejscu:
     * plugins/SkyRiseJobs/config/<feature>/
     */
    public static File featureFolder(JavaPlugin plugin, String featureName) {
        String sanitized = sanitizeFeatureName(featureName);
        File folder = new File(plugin.getDataFolder(), "config" + File.separator + sanitized);
        folder.mkdirs();
        return folder;
    }

    public static File featureFile(JavaPlugin plugin, String featureName, String fileName) {
        File folder = featureFolder(plugin, featureName);
        String safeFileName = fileName == null || fileName.isBlank() || fileName.contains("..")
                ? "data.yml"
                : fileName.replace("\\", "/");
        return new File(folder, safeFileName);
    }

    public static File configFolder(JavaPlugin plugin, String moduleName) {
        String sanitized = sanitizeFeatureName(moduleName);
        File folder = new File(plugin.getDataFolder(), "config" + File.separator + sanitized);
        folder.mkdirs();
        return folder;
    }

    private static String sanitizeFeatureName(String value) {
        String sanitized = value == null ? "module" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return sanitized.isBlank() ? "module" : sanitized;
    }
}
