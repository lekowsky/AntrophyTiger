package pl.skyrise.skyRiseJobs.features.windowcleaning;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.api.AbstractJobModule;
import pl.skyrise.skyRiseJobs.core.ModuleSupport;
import pl.skyrise.skyRiseJobs.core.VaultHook;
import pl.skyrise.skyRiseJobs.features.windowcleaning.commands.WindowCleaningCommand;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.InventoryListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.ItemProtectionListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.NPCClickListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.PlayerStateListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.RegionWandListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.WindowCleaningListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.ConfigManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.DataManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.GUIManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.JobManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.LevelManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.NPCManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.ParticleManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.SkillManager;
import pl.skyrise.skyRiseJobs.utils.ItemBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class WindowCleaningJobModule extends AbstractJobModule {

    private static WindowCleaningJobModule instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private LevelManager levelManager;
    private SkillManager skillManager;
    private JobManager jobManager;
    private NPCManager npcManager;
    private GUIManager guiManager;
    private ParticleManager particleManager;

    private final List<Listener> listeners = new ArrayList<>();

    public WindowCleaningJobModule(SkyRiseJobs plugin) {
        super(plugin, "windowcleaning");
    }

    @Override
    protected void onJobEnable() {
        instance = this;

        if (!VaultHook.hasEconomy()) {
            throw new IllegalStateException("Vault economy nie znaleziony — WindowCleaning wymaga ekonomii.");
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            throw new IllegalStateException("Citizens nie znaleziony — WindowCleaning wymaga Citizens NPC.");
        }

        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        dataManager.init();
        levelManager = new LevelManager(this);
        skillManager = new SkillManager(this);
        jobManager = new JobManager(this);
        npcManager = new NPCManager(this);
        guiManager = new GUIManager(this);
        particleManager = new ParticleManager(this);

        register(new NPCClickListener(this));
        register(new InventoryListener(this));
        register(new ItemProtectionListener(this));
        register(new WindowCleaningListener(this));
        register(new RegionWandListener(this));
        register(new PlayerStateListener(this));

        WindowCleaningCommand command = new WindowCleaningCommand(this);
        ModuleSupport.bindExecutor(plugin, command, "windowcleaning");
        plugin.getTabRegistry().register("windowcleaning", (sender, args) -> new pl.skyrise.skyRiseJobs.features.windowcleaning.commands.WindowCleaningTabCompleter(this)
                .onTabComplete(sender, null, "windowcleaning", args));

        plugin.getServer().getScheduler().runTask(plugin, () -> npcManager.createNPCs());
        plugin.getLogger().info("  → WindowCleaning: praca Mycie Okien załadowana.");
    }

    @Override
    protected void onJobDisable() {
        ModuleSupport.bindDisabled(plugin, getName(), "windowcleaning");
        ModuleSupport.unregisterTabs(plugin.getTabRegistry(), "windowcleaning");
        for (Listener listener : listeners) ModuleSupport.unregisterListener(listener);
        listeners.clear();
        if (npcManager != null) npcManager.removeNPCs();
        if (particleManager != null) particleManager.stopAll();
        if (dataManager != null) dataManager.close();
    }

    @Override
    protected void onJobReload() {
        configManager.reload();
        if (npcManager != null) npcManager.createNPCs();
    }

    private void register(Listener listener) {
        listeners.add(ModuleSupport.registerListener(plugin, listener));
    }

    @Override
    public String getDisplayName() {
        return getConfig().getString("job-display-name", "Mycie Okien");
    }

    @Override
    public List<String> getDescription() {
        return getConfig().getStringList("job-description").isEmpty()
                ? List.of("&7Myj okna na wieżowcu i odbieraj wypłatę.")
                : getConfig().getStringList("job-description");
    }

    @Override
    public ItemStack getIcon(Player player) {
        return new ItemBuilder(Material.GLASS_PANE)
                .name(getMenuColor() + "&l" + getDisplayName())
                .build();
    }

    @Override
    public String getMenuColor() {
        return getConfig().getString("job-menu-color", "&1");
    }

    @Override
    protected boolean hasJobAccess(Player player) {
        return player != null && player.hasPermission("skyrisejobs.job.windowcleaning");
    }

    @Override
    public void openMenu(Player player) {
        guiManager.openMainJobGUI(player);
    }

    public void addGlobalCharacterExp(Player player, double baseExp) {
        addCharacterExp(player, baseExp);
    }

    public double applyGlobalMoneyBoost(double baseMoney) {
        return applyMoneyBoost(baseMoney);
    }

    public net.kyori.adventure.text.Component formatJobMessage(String legacyMessage) {
        return pl.skyrise.skyRiseJobs.utils.ColorUtil.mini(plugin.prefix() + pl.skyrise.skyRiseJobs.utils.ColorUtil.legacyToMini(legacyMessage));
    }

    public void sendJobMessage(org.bukkit.command.CommandSender sender, String legacyMessage) {
        sender.sendMessage(formatJobMessage(legacyMessage));
    }

    // Adapter metod oczekiwanych przez przenoszony kod.
    public static WindowCleaningJobModule getInstance() { return instance; }
    public SkyRiseJobs getPlugin() { return plugin; }
    public Server getServer() { return plugin.getServer(); }
    public Logger getLogger() { return plugin.getLogger(); }
    public Economy getEconomy() { return VaultHook.economy(); }
    public File getDataFolder() { return getFeatureFolder(); }
    public FileConfiguration getConfig() { return config.getConfig(); }
    public void saveConfig() { config.save(); }
    public void reloadConfig() { config.reload(); }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public JobManager getJobManager() { return jobManager; }
    public NPCManager getNPCManager() { return npcManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public ParticleManager getParticleManager() { return particleManager; }
}
