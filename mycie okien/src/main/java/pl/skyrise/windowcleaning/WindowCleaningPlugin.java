package pl.skyrise.windowcleaning;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.windowcleaning.commands.WindowCleaningCommand;
import pl.skyrise.windowcleaning.commands.WindowCleaningTabCompleter;
import pl.skyrise.windowcleaning.managers.*;
import pl.skyrise.windowcleaning.listeners.*;

public class WindowCleaningPlugin extends JavaPlugin {

    private static WindowCleaningPlugin instance;
    private Economy economy;
    private ConfigManager configManager;
    private DataManager dataManager;
    private LevelManager levelManager;
    private SkillManager skillManager;
    private JobManager jobManager;
    private NPCManager npcManager;
    private GUIManager guiManager;
    private ParticleManager particleManager;

    @Override
    public void onEnable() {
        instance = this;
        if (!setupEconomy()) {
            getLogger().severe("Vault nie znaleziony! Wyłączanie pluginu.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        dataManager.init();
        levelManager = new LevelManager(this);
        skillManager = new SkillManager(this);
        jobManager = new JobManager(this);
        npcManager = new NPCManager(this);
        guiManager = new GUIManager(this);
        particleManager = new ParticleManager(this);

        getServer().getPluginManager().registerEvents(new NPCClickListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new WindowCleaningListener(this), this);
        getServer().getPluginManager().registerEvents(new RegionWandListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this);

        PluginCommand command = getCommand("windowcleaning");
        if (command != null) {
            command.setExecutor(new WindowCleaningCommand(this));
            command.setTabCompleter(new WindowCleaningTabCompleter(this));
        }
        getServer().getScheduler().runTask(this, () -> npcManager.createNPCs());
        getLogger().info("WindowCleaning dla SkyRise włączony!");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) npcManager.removeNPCs();
        if (particleManager != null) particleManager.stopAll();
        if (dataManager != null) dataManager.close();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static WindowCleaningPlugin getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public JobManager getJobManager() { return jobManager; }
    public NPCManager getNPCManager() { return npcManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public ParticleManager getParticleManager() { return particleManager; }
}