package pl.skyrise.skyRiseJobs;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseJobs.command.BoostCommand;
import pl.skyrise.skyRiseJobs.command.JobsCommand;
import pl.skyrise.skyRiseJobs.command.SkyRiseJobsCommand;
import pl.skyrise.skyRiseJobs.core.CitizensHook;
import pl.skyrise.skyRiseJobs.core.CoreTabCompleter;
import pl.skyrise.skyRiseJobs.core.boost.BoostManager;
import pl.skyrise.skyRiseJobs.core.level.LevelManager;
import pl.skyrise.skyRiseJobs.core.ModuleSupport;
import pl.skyrise.skyRiseJobs.core.TabRegistry;
import pl.skyrise.skyRiseJobs.core.VaultHook;
import pl.skyrise.skyRiseJobs.core.npc.NpcRegistry;
import pl.skyrise.skyRiseJobs.api.ModuleManager;
import pl.skyrise.skyRiseJobs.api.job.JobDataStore;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.gui.GuiListener;
import pl.skyrise.skyRiseJobs.core.placeholder.SkyRiseJobsExpansion;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SkyRiseJobs extends JavaPlugin {

    private static SkyRiseJobs instance;

    private ModuleManager moduleManager;
    private TabRegistry tabRegistry;
    private NpcRegistry npcRegistry;
    private BoostManager boostManager;
    private LevelManager levelManager;
    private java.sql.Connection jobDatabase;

    private File mainConfigFile;
    private FileConfiguration mainConfig;

    @Override
    public void onEnable() {
        instance = this;
        getDataFolder().mkdirs();
        new File(getDataFolder(), "features").mkdirs();
        new File(getDataFolder(), "config").mkdirs();
        saveDefaultConfig();

        this.tabRegistry = new TabRegistry();
        this.moduleManager = new ModuleManager(this);
        this.npcRegistry = new NpcRegistry(this);

        VaultHook.setup(this);
        CitizensHook.setup(this, npcRegistry);
        this.boostManager = new BoostManager(this);
        this.boostManager.load();
        this.levelManager = new LevelManager(this);
        this.levelManager.load();

        // Jedna współdzielona baza danych prac (mniej plików, brak narzutu wielu połączeń).
        this.jobDatabase = JobDataStore.openSharedConnection(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SkyRiseJobsExpansion(this).register();
            getLogger().info("PlaceholderAPI połączone — placeholdery SkyRiseJobs dostępne.");
        }

        getServer().getPluginManager().registerEvents(npcRegistry, this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        SkyRiseJobsCommand adminCommand = new SkyRiseJobsCommand(this);
        JobsCommand jobsCommand = new JobsCommand(this);
        BoostCommand boostCommand = new BoostCommand(this);
        pl.skyrise.skyRiseJobs.command.LevelCommand levelCommand = new pl.skyrise.skyRiseJobs.command.LevelCommand(this);
        ModuleSupport.bindExecutor(this, adminCommand, "skyrisejobs");
        ModuleSupport.bindExecutor(this, jobsCommand, "jobs");
        ModuleSupport.bindExecutor(this, boostCommand, "jobboost");
        ModuleSupport.bindExecutor(this, levelCommand, "joblevel");

        tabRegistry.register("skyrisejobs", adminCommand::tab);
        tabRegistry.register("jobs", jobsCommand::tab);
        tabRegistry.register("jobboost", boostCommand::tab);
        tabRegistry.register("joblevel", levelCommand::tab);

        registerModules();
        installTabCompleters();

        getLogger().info("SkyRiseJobs włączony. Moduły: " + moduleManager.getModuleCount()
                + ", Vault: " + VaultHook.isEnabled()
                + ", Citizens: " + CitizensHook.isEnabled());
    }

    @Override
    public FileConfiguration getConfig() {
        if (mainConfig == null) reloadConfig();
        return mainConfig;
    }

    @Override
    public void reloadConfig() {
        if (mainConfigFile == null) {
            mainConfigFile = new File(getDataFolder(), "config" + File.separator + "main" + File.separator + "config.yml");
        }
        if (!mainConfigFile.exists()) {
            saveDefaultConfig();
        }
        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
    }

    @Override
    public void saveConfig() {
        if (mainConfig == null || mainConfigFile == null) return;
        try {
            File parent = mainConfigFile.getParentFile();
            if (parent != null) parent.mkdirs();
            mainConfig.save(mainConfigFile);
        } catch (IOException exception) {
            getLogger().severe("Nie można zapisać głównego configu: " + exception.getMessage());
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (mainConfigFile == null) {
            mainConfigFile = new File(getDataFolder(), "config" + File.separator + "main" + File.separator + "config.yml");
        }
        migrateLegacyMainConfigIfNeeded();
        if (mainConfigFile.exists()) return;
        try {
            File parent = mainConfigFile.getParentFile();
            if (parent != null) parent.mkdirs();
            try (InputStream resource = getResource("config.yml")) {
                if (resource != null) Files.copy(resource, mainConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                else mainConfigFile.createNewFile();
            }
        } catch (IOException exception) {
            getLogger().severe("Nie można utworzyć głównego configu: " + exception.getMessage());
        }
    }

    private void migrateLegacyMainConfigIfNeeded() {
        File legacyRootConfig = new File(getDataFolder(), "config.yml");
        if (mainConfigFile.exists() || !legacyRootConfig.exists()) return;
        try {
            File parent = mainConfigFile.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.move(legacyRootConfig.toPath(), mainConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLogger().info("Przeniesiono główny config do config/main/config.yml");
        } catch (IOException exception) {
            getLogger().warning("Nie udało się przenieść starego głównego configu: " + exception.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (levelManager != null) {
            levelManager.shutdown();
        }
        if (boostManager != null) {
            boostManager.shutdown();
        }
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        JobDataStore.closeSharedConnection(jobDatabase);
        getLogger().info("SkyRiseJobs wyłączony.");
    }

    /**
     * Rejestruj przyszłe moduły prac tutaj, np.:
     * moduleManager.register(new MinerJobModule(this));
     */
    private void registerModules() {
        moduleManager.register(new WindowCleaningJobModule(this));
    }

    private void installTabCompleters() {
        CoreTabCompleter completer = new CoreTabCompleter(tabRegistry);
        for (String commandName : tabRegistry.getRegisteredCommands()) {
            PluginCommand command = getCommand(commandName);
            if (command != null) command.setTabCompleter(completer);
        }
    }

    public String prefix() {
        return getConfig().getString("prefix", "<#e36c1e><bold>PRACA</bold> <gray>» <white>");
    }

    public String highlight() {
        return getConfig().getString("highlight-color", "<#36d1ff>");
    }

    public void sendPrefixed(org.bukkit.command.CommandSender sender, String messagePathOrText) {
        String raw = getConfig().getString("messages." + messagePathOrText, messagePathOrText);
        raw = raw.replace("{prefix}", prefix());
        sender.sendMessage(ColorUtil.mini(raw));
    }

    public static SkyRiseJobs getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public TabRegistry getTabRegistry() {
        return tabRegistry;
    }

    public NpcRegistry getNpcRegistry() {
        return npcRegistry;
    }

    public BoostManager getBoostManager() {
        return boostManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    /** Współdzielone połączenie do bazy danych prac (data.db). */
    public java.sql.Connection getJobDatabase() {
        return jobDatabase;
    }

    public String applyPlaceholders(org.bukkit.entity.Player player, String text) {
        return levelManager != null ? levelManager.applyPlaceholders(player, text) : text;
    }
}
