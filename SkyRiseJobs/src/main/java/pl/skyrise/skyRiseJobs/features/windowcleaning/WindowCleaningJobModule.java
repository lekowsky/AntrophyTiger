package pl.skyrise.skyRiseJobs.features.windowcleaning;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.api.job.BaseJobModule;
import pl.skyrise.skyRiseJobs.api.job.JobData;
import pl.skyrise.skyRiseJobs.api.job.JobSkill;
import pl.skyrise.skyRiseJobs.core.ModuleSupport;
import pl.skyrise.skyRiseJobs.core.VaultHook;
import pl.skyrise.skyRiseJobs.features.windowcleaning.commands.WindowCleaningCommand;
import pl.skyrise.skyRiseJobs.features.windowcleaning.commands.WindowCleaningTabCompleter;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.InventoryListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.ItemProtectionListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.NPCClickListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.PlayerStateListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.RegionWandListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.listeners.WindowCleaningListener;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.ConfigManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.JobManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.NPCManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.managers.ParticleManager;
import pl.skyrise.skyRiseJobs.features.windowcleaning.session.JobSession;
import pl.skyrise.skyRiseJobs.utils.ItemBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Praca "Mycie Okien".
 *
 * Cała powtarzalna mechanika (GUI pracy, drzewko umiejętności, punkty, poziomy, dane)
 * pochodzi z {@link BaseJobModule}. Ta klasa dostarcza tylko to, co specyficzne dla
 * mycia okien: perki, listenery interakcji, minigrę, NPC i logikę zlecenia.
 */
public class WindowCleaningJobModule extends BaseJobModule {

    private static WindowCleaningJobModule instance;

    private ConfigManager configManager;
    private JobManager jobManager;
    private NPCManager npcManager;
    private ParticleManager particleManager;

    private final List<Listener> listeners = new ArrayList<>();

    public WindowCleaningJobModule(SkyRiseJobs plugin) {
        super(plugin, "windowcleaning");
    }

    @Override
    public void onEnable() {
        super.onEnable(); // framework: config, dataStore, levels, skillService, gui, perki
        instance = this;

        if (!VaultHook.hasEconomy()) {
            throw new IllegalStateException("Vault economy nie znaleziony — WindowCleaning wymaga ekonomii.");
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            throw new IllegalStateException("Citizens nie znaleziony — WindowCleaning wymaga Citizens NPC.");
        }

        configManager = new ConfigManager(this);
        jobManager = new JobManager(this);
        npcManager = new NPCManager(this);
        particleManager = new ParticleManager(this);

        register(new NPCClickListener(this));
        register(new InventoryListener(this));
        register(new ItemProtectionListener(this));
        register(new WindowCleaningListener(this));
        register(new RegionWandListener(this));
        register(new PlayerStateListener(this));

        WindowCleaningCommand command = new WindowCleaningCommand(this);
        ModuleSupport.bindExecutor(plugin, command, "windowcleaning");
        plugin.getTabRegistry().register("windowcleaning", (sender, args) ->
                new WindowCleaningTabCompleter(this).onTabComplete(sender, null, "windowcleaning", args));

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
    }

    @Override
    protected void onJobReload() {
        configManager.reload();
        if (npcManager != null) npcManager.createNPCs();
    }

    private void register(Listener listener) {
        listeners.add(ModuleSupport.registerListener(plugin, listener));
    }

    // ---------- Metadane pracy ----------

    @Override
    public String getDisplayName() {
        return getConfig().getString("job-display-name", "Mycie Okien");
    }

    @Override
    public List<String> getDescription() {
        List<String> desc = getConfig().getStringList("job-description");
        return desc.isEmpty() ? List.of("&7Myj okna na wieżowcu i odbieraj wypłatę.") : desc;
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

    // ---------- Perki (osobne dla tej pracy) ----------

    @Override
    protected List<JobSkill> defineSkills() {
        return List.of(
                JobSkill.of("szybsze_szorowanie", Material.DIAMOND_PICKAXE)
                        .slot(11).name("&b&lSzybsze szorowanie")
                        .description("Zwiększa siłę szorowania.")
                        .maxLevel(3).cost(1, 2, 3).requiredJobLevel(0).build(),
                JobSkill.of("wytrzymalosc", Material.IRON_CHESTPLATE)
                        .slot(13).name("&7&lWytrzymałość")
                        .description("Zmniejsza karę za chybienie.")
                        .maxLevel(3).cost(2, 3, 4).requiredJobLevel(2).build(),
                JobSkill.of("wieksza_strefa", Material.TARGET)
                        .slot(15).name("&e&lWiększa strefa")
                        .description("Poszerza zieloną strefę.")
                        .maxLevel(2).cost(3, 5).requiredJobLevel(3).build(),
                JobSkill.of("czysta_robota", Material.NETHER_STAR)
                        .slot(20).name("&d&lCzysta robota")
                        .description("Premia za każde perfekcyjne okno.")
                        .maxLevel(2).cost(3, 5).requiredJobLevel(4).build(),
                JobSkill.of("darmowe_namaczanie", Material.WATER_BUCKET)
                        .slot(22).name("&3&lDarmowe namaczanie")
                        .description("Szansa na darmowe namoczenie.")
                        .maxLevel(2).cost(3, 5).requiredJobLevel(4).build(),
                JobSkill.of("bonus_za_komplet", Material.DIAMOND)
                        .slot(24).name("&6&lBonus za komplet")
                        .description("Mnożnik za 100% perfekcyjnych okien.")
                        .maxLevel(2).cost(4, 6).requiredJobLevel(5).build()
        );
    }

    // ---------- Dodatkowe statystyki + instrukcja ----------

    @Override
    public List<String> getStatsLines(Player player, JobData data) {
        double earningsBonus = configManager.getEarningsPerLevel() * data.level * 100.0;
        return List.of("&7Premia do zarobku: &a+" + String.format("%.0f", earningsBonus) + "%");
    }

    @Override
    public List<String> getHowToWork() {
        return List.of(
                "&71. Rozpocznij pracę.",
                "&72. Zamocz szczotkę (PPM w powietrze).",
                "&73. Wjedź windą ▲.",
                "&74. Myj szare szyby.",
                "&75. Wróć windą ▼ i odbierz wypłatę."
        );
    }

    // ---------- Logika pracy (hooki frameworku) ----------

    @Override
    public WorkState getWorkState(Player player) {
        if (!jobManager.hasActiveSession(player)) return WorkState.IDLE;
        JobSession session = jobManager.getSession(player);
        return session != null && session.isJobCompleted() ? WorkState.READY_TO_FINISH : WorkState.IN_PROGRESS;
    }

    @Override
    public void startWork(Player player) {
        jobManager.startJob(player);
    }

    @Override
    public void completeWork(Player player) {
        JobSession session = jobManager.getSession(player);
        if (session != null && session.isJobCompleted()) {
            jobManager.completeJob(player);
        } else {
            sendJobMessage(player, "&cNie umyłeś jeszcze wszystkich okien!");
        }
    }

    @Override
    public void cancelWork(Player player) {
        jobManager.forceEndJob(player);
        sendJobMessage(player, "&cZrezygnowałeś z pracy. Straciłeś wypłatę i doświadczenie.");
    }

    // ---------- Adaptery oczekiwane przez przenoszony kod pracy ----------

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
    public JobManager getJobManager() { return jobManager; }
    public NPCManager getNPCManager() { return npcManager; }
    public ParticleManager getParticleManager() { return particleManager; }
    public GuiBridge getGUIManager() { return guiBridge; }

    /**
     * Pomost zgodności: stary kod woła plugin.getGUIManager().openMainJobGUI(...)
     * / openSkillTreeGUI(...). Kierujemy to do wspólnego GUI frameworku.
     */
    private final GuiBridge guiBridge = new GuiBridge();
    public final class GuiBridge {
        public void openMainJobGUI(Player player) { getGui().openMain(player); }
        public void openSkillTreeGUI(Player player) { getGui().openSkillTree(player); }
    }
}
