package pl.skyrise.skyRiseJobs.core.level;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.api.JobModule;
import pl.skyrise.skyRiseJobs.core.ModuleSupport;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;
import pl.skyrise.skyRiseJobs.utils.CustomConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelManager {

    private final SkyRiseJobs plugin;
    private final CustomConfig config;
    private final File dataFile;
    private final Map<UUID, LevelProfile> profiles = new ConcurrentHashMap<>();
    private final Map<Integer, Double> expOverrides = new HashMap<>();
    private final Map<String, Integer> jobRequirements = new HashMap<>();
    private BukkitTask queuedSaveTask;

    private boolean enabled;
    private String prefix;
    private int startLevel;
    private int maxLevel;
    private double baseExp;
    private double multiplier;
    private int defaultJobRequiredLevel;
    private Map<String, String> messages = new HashMap<>();

    public LevelManager(SkyRiseJobs plugin) {
        this.plugin = plugin;
        this.config = new CustomConfig(plugin, "levels", "config.yml");
        this.dataFile = ModuleSupport.featureFile(plugin, "levels", "data.yml");
    }

    public void load() {
        config.load();
        cacheConfig();
        loadData();
    }

    public void reload() {
        flushSave();
        config.reload();
        cacheConfig();
        loadData();
    }

    public void shutdown() {
        flushSave();
    }

    private void cacheConfig() {
        FileConfiguration c = config.getConfig();
        enabled = c.getBoolean("enabled", true);
        prefix = c.getString("prefix", "<#8be936><bold>POZIOM</bold> <gray>» <white>");
        startLevel = Math.max(1, c.getInt("start-level", 1));
        maxLevel = Math.max(startLevel, c.getInt("max-level", 100));
        baseExp = Math.max(1.0, c.getDouble("formula.base-exp", 100.0));
        multiplier = Math.max(1.0, c.getDouble("formula.multiplier", 1.25));

        expOverrides.clear();
        ConfigurationSection levels = c.getConfigurationSection("levels");
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                try {
                    expOverrides.put(Integer.parseInt(key), Math.max(1.0, levels.getDouble(key)));
                } catch (NumberFormatException ignored) {}
            }
        }

        defaultJobRequiredLevel = Math.max(1, c.getInt("job-requirements.default", 1));
        jobRequirements.clear();
        ConfigurationSection jobs = c.getConfigurationSection("job-requirements.jobs");
        if (jobs != null) {
            for (String key : jobs.getKeys(false)) {
                jobRequirements.put(key.toLowerCase(Locale.ROOT), Math.max(1, jobs.getInt(key)));
            }
        }

        messages = new HashMap<>();
        ConfigurationSection msg = c.getConfigurationSection("messages");
        if (msg != null) {
            for (String key : msg.getKeys(false)) messages.put(key, msg.getString(key, ""));
        }
    }

    private void loadData() {
        ensureDataFile();
        profiles.clear();
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) return;
        for (String rawUuid : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                int level = clampLevel(section.getInt(rawUuid + ".level", startLevel));
                double exp = Math.max(0.0, section.getDouble(rawUuid + ".exp", 0.0));
                profiles.put(uuid, new LevelProfile(level, exp));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveData() {
        ensureDataFile();
        FileConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, LevelProfile> entry : profiles.entrySet()) {
            String path = "players." + entry.getKey();
            data.set(path + ".level", entry.getValue().getLevel());
            data.set(path + ".exp", entry.getValue().getExp());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać danych poziomów: " + e.getMessage());
        }
    }

    public void queueSave() {
        if (queuedSaveTask != null) return;
        queuedSaveTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, this::flushSave, 20L * 10L);
    }

    public void flushSave() {
        if (queuedSaveTask != null) {
            queuedSaveTask.cancel();
            queuedSaveTask = null;
        }
        saveData();
    }

    private void ensureDataFile() {
        if (dataFile.exists()) return;
        try {
            File parent = dataFile.getParentFile();
            if (parent != null) parent.mkdirs();
            dataFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można utworzyć danych poziomów: " + e.getMessage());
        }
    }

    public LevelProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, ignored -> new LevelProfile(startLevel, 0.0));
    }

    public LevelProfile getProfile(OfflinePlayer player) {
        return getProfile(player.getUniqueId());
    }

    public int getLevel(UUID uuid) {
        return getProfile(uuid).getLevel();
    }

    public double getExp(UUID uuid) {
        return getProfile(uuid).getExp();
    }

    public double getRequiredExp(int level) {
        if (level >= maxLevel) return 0.0;
        Double override = expOverrides.get(level);
        if (override != null) return override;
        return baseExp * Math.pow(multiplier, Math.max(0, level - startLevel));
    }

    public double getProgress(UUID uuid) {
        LevelProfile profile = getProfile(uuid);
        double required = getRequiredExp(profile.getLevel());
        if (required <= 0.0) return 100.0;
        return Math.max(0.0, Math.min(100.0, (profile.getExp() / required) * 100.0));
    }

    public String getLevelProgressPlaceholder(UUID uuid) {
        return getLevel(uuid) + " (" + formatPercent(getProgress(uuid)) + "%)";
    }

    public LevelAddResult addExperience(Player player, double baseAmount) {
        if (player == null || !enabled || baseAmount <= 0.0) return new LevelAddResult(0.0, 0, 0, false);
        double amount = plugin.getBoostManager().applyExp(baseAmount);
        LevelProfile profile = getProfile(player.getUniqueId());
        int oldLevel = profile.getLevel();
        profile.setExp(profile.getExp() + amount);

        while (profile.getLevel() < maxLevel) {
            double required = getRequiredExp(profile.getLevel());
            if (required <= 0.0 || profile.getExp() < required) break;
            profile.setExp(profile.getExp() - required);
            profile.setLevel(profile.getLevel() + 1);
        }
        if (profile.getLevel() >= maxLevel) profile.setExp(0.0);
        queueSave();

        boolean leveled = profile.getLevel() > oldLevel;
        if (leveled) send(player, "level-up");
        return new LevelAddResult(amount, oldLevel, profile.getLevel(), leveled);
    }

    public void setLevel(OfflinePlayer player, int level, double exp) {
        profiles.put(player.getUniqueId(), new LevelProfile(clampLevel(level), Math.max(0.0, exp)));
        queueSave();
    }

    public int getRequiredLevel(String jobId) {
        if (jobId == null) return defaultJobRequiredLevel;
        return jobRequirements.getOrDefault(jobId.toLowerCase(Locale.ROOT), defaultJobRequiredLevel);
    }

    public int getRequiredLevel(JobModule jobModule) {
        return jobModule != null ? getRequiredLevel(jobModule.getJobId()) : defaultJobRequiredLevel;
    }

    public boolean canAccessJob(Player player, String jobId) {
        if (!enabled || player == null) return true;
        return getLevel(player.getUniqueId()) >= getRequiredLevel(jobId);
    }

    public boolean canAccessJob(Player player, JobModule jobModule) {
        if (!enabled || player == null || jobModule == null) return true;
        return canAccessJob(player, jobModule.getJobId());
    }

    public void sendLockedMessage(Player player, JobModule jobModule) {
        if (player == null || jobModule == null) return;
        String raw = messages.getOrDefault("job-locked", "{prefix}<red>Ta praca wymaga poziomu {required_level}.</red>");
        player.sendMessage(ColorUtil.mini(applyPlaceholders(player, raw)
                .replace("{required_level}", String.valueOf(getRequiredLevel(jobModule)))));
    }

    public String applyPlaceholders(Player player, String text) {
        if (text == null) return "";
        UUID uuid = player != null ? player.getUniqueId() : null;
        LevelProfile profile = uuid != null ? getProfile(uuid) : new LevelProfile(startLevel, 0.0);
        double required = getRequiredExp(profile.getLevel());
        return text
                .replace("{prefix}", prefix)
                .replace("{player}", player != null ? player.getName() : "")
                .replace("{character_level}", String.valueOf(profile.getLevel()))
                .replace("{level}", String.valueOf(profile.getLevel()))
                .replace("{character_exp}", formatNumber(profile.getExp()))
                .replace("{exp}", formatNumber(profile.getExp()))
                .replace("{character_exp_required}", formatNumber(required))
                .replace("{exp_required}", formatNumber(required))
                .replace("{character_exp_percent}", formatPercent(getProgress(profile)))
                .replace("{exp_percent}", formatPercent(getProgress(profile)))
                .replace("{character_level_progress}", profile.getLevel() + " (" + formatPercent(getProgress(profile)) + "%)")
                .replace("{level_progress}", profile.getLevel() + " (" + formatPercent(getProgress(profile)) + "%)");
    }

    private double getProgress(LevelProfile profile) {
        double required = getRequiredExp(profile.getLevel());
        if (required <= 0.0) return 100.0;
        return Math.max(0.0, Math.min(100.0, (profile.getExp() / required) * 100.0));
    }

    public void send(CommandSender sender, String key) {
        String raw = messages.getOrDefault(key, key);
        if (sender instanceof Player player) raw = applyPlaceholders(player, raw);
        else raw = raw.replace("{prefix}", plugin.prefix());
        sender.sendMessage(ColorUtil.mini(raw));
    }

    private int clampLevel(int level) {
        return Math.max(startLevel, Math.min(maxLevel, level));
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, value == Math.rint(value) ? "%.0f" : "%.2f", value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, value == Math.rint(value) ? "%.0f" : "%.1f", value);
    }
}
