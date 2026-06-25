package pl.skyrise.skyRiseJobs.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Config pojedynczego modułu/pracy.
 *
 * Docelowy runtime layout:
 * plugins/SkyRiseJobs/config/<module>/config.yml
 * plugins/SkyRiseJobs/config/<module>/<inne-configi>.yml
 */
public class CustomConfig {
    private final JavaPlugin plugin;
    private final String module;
    private final String fileName;
    private final File file;
    private YamlConfiguration config;

    public CustomConfig(JavaPlugin plugin, String module, String fileName) {
        this.plugin = plugin;
        this.module = sanitize(module);
        this.fileName = sanitizeFileName(fileName == null ? "config.yml" : fileName);
        this.file = new File(plugin.getDataFolder(), "config" + File.separator + this.module + File.separator + this.fileName);
    }

    public CustomConfig(JavaPlugin plugin, String moduleFileName) {
        this(plugin, moduleFrom(moduleFileName), "config.yml");
    }

    public void load() {
        migrateLegacyFilesIfNeeded();
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();
                try (InputStream resource = findResource()) {
                    if (resource != null) Files.copy(resource, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    else file.createNewFile();
                }
            } catch (IOException exception) {
                plugin.getLogger().severe("Nie można utworzyć configu " + file.getPath() + ": " + exception.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        if (!file.exists()) load();
        else config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (config == null) return;
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Nie można zapisać configu " + file.getPath() + ": " + exception.getMessage());
        }
    }

    public YamlConfiguration getConfig() { return config; }
    public File getFile() { return file; }
    public String getModule() { return module; }
    public String getFileName() { return fileName; }

    private InputStream findResource() {
        // Nowy rekomendowany układ zasobów w jar:
        // src/main/resources/config/<module>/<fileName>
        InputStream resource = plugin.getResource("config/" + module + "/" + fileName);
        if (resource != null) return resource;

        // Alternatywnie pliki modułu mogą być bundlowane przy kodzie feature:
        // src/main/resources/features/<module>/<fileName>
        resource = plugin.getResource("features/" + module + "/" + fileName);
        if (resource != null) return resource;

        // Kompatybilność wsteczna z wcześniejszym szkieletem.
        resource = plugin.getResource("modules/" + module + "/" + fileName);
        if (resource != null) return resource;
        resource = plugin.getResource("config/" + module + ".yml");
        if (resource != null) return resource;
        return plugin.getResource(fileName);
    }

    private void migrateLegacyFilesIfNeeded() {
        if (file.exists()) return;

        // Stary układ: plugins/SkyRiseJobs/<module>/<fileName>
        File oldModuleFile = new File(plugin.getDataFolder(), module + File.separator + fileName);
        if (oldModuleFile.exists()) {
            moveLegacy(oldModuleFile);
            cleanupEmptyDirectory(oldModuleFile.getParentFile());
            return;
        }

        // Stary config modułu jako jeden plik: plugins/SkyRiseJobs/config/<module>.yml
        if (fileName.equalsIgnoreCase("config.yml")) {
            File oldFlatConfig = new File(plugin.getDataFolder(), "config" + File.separator + module + ".yml");
            if (oldFlatConfig.exists()) {
                moveLegacy(oldFlatConfig);
                cleanupEmptyDirectory(oldFlatConfig.getParentFile());
                return;
            }
        }

        // Bardzo stary fallback: plugins/SkyRiseJobs/<fileName>
        File oldRootFile = new File(plugin.getDataFolder(), fileName);
        if (oldRootFile.exists()) moveLegacy(oldRootFile);
    }

    private void moveLegacy(File legacyFile) {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.move(legacyFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Przeniesiono plik modułu " + module + " do config/" + module + "/" + fileName);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się przenieść starego pliku " + legacyFile.getPath() + ": " + e.getMessage());
        }
    }

    private void cleanupEmptyDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        String[] children = dir.list();
        if (children == null || children.length == 0) dir.delete();
    }

    private static String moduleFrom(String fileName) {
        if (fileName == null || fileName.isBlank()) return "module";
        String name = fileName.toLowerCase(Locale.ROOT).trim();
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
        if (name.endsWith(".yaml")) name = name.substring(0, name.length() - 5);
        return sanitize(name);
    }

    private static String sanitize(String value) {
        String sanitized = value == null ? "module" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return sanitized.isBlank() ? "module" : sanitized;
    }

    private static String sanitizeFileName(String value) {
        String sanitized = value == null ? "config.yml" : value.replace("\\", "/");
        while (sanitized.startsWith("/")) sanitized = sanitized.substring(1);
        if (sanitized.contains("..")) sanitized = "config.yml";
        return sanitized.isBlank() ? "config.yml" : sanitized;
    }
}
