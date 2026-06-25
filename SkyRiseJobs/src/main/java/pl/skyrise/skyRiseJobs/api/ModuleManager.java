package pl.skyrise.skyRiseJobs.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ModuleManager {

    private final JavaPlugin plugin;
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Set<String> enabled = new HashSet<>();

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Module module) {
        String key = key(module.getName());
        modules.put(key, module);
        enable(module.getName());
    }

    public boolean enable(String name) {
        String key = key(name);
        Module module = modules.get(key);
        if (module == null || enabled.contains(key)) return false;
        try {
            module.onEnable();
            enabled.add(key);
            plugin.getLogger().info("✔ Moduł " + module.getName() + " włączony.");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Błąd przy włączaniu modułu " + module.getName(), exception);
            return false;
        }
    }

    public boolean disable(String name) {
        String key = key(name);
        Module module = modules.get(key);
        if (module == null || !enabled.contains(key)) return false;
        try {
            module.onDisable();
            enabled.remove(key);
            plugin.getLogger().info("✘ Moduł " + module.getName() + " wyłączony.");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Błąd przy wyłączaniu modułu " + module.getName(), exception);
            return false;
        }
    }

    public void disableAll() {
        for (String key : new HashSet<>(enabled)) disable(key);
    }

    public boolean reload(String name) {
        String key = key(name);
        if (!enabled.contains(key)) return false;
        Module module = modules.get(key);
        if (module == null) return false;
        try {
            module.onReload();
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Błąd przy reloadzie modułu " + module.getName(), exception);
            return false;
        }
    }

    public void reloadAll() {
        for (String key : new HashSet<>(enabled)) reload(key);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(String name) {
        return (T) modules.get(key(name));
    }

    public boolean isEnabled(String name) {
        return enabled.contains(key(name));
    }

    public int getModuleCount() {
        return modules.size();
    }

    public Set<String> getModuleNames() {
        return Collections.unmodifiableSet(modules.keySet());
    }

    public Map<String, Module> getModules() {
        return Collections.unmodifiableMap(modules);
    }

    public List<JobModule> getJobModules() {
        List<JobModule> jobs = new ArrayList<>();
        for (Module module : modules.values()) {
            if (module instanceof JobModule jobModule && isEnabled(module.getName())) jobs.add(jobModule);
        }
        return jobs;
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
