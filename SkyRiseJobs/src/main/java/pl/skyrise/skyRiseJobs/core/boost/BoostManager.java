package pl.skyrise.skyRiseJobs.core.boost;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.core.ModuleSupport;
import pl.skyrise.skyRiseJobs.core.VaultHook;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;
import pl.skyrise.skyRiseJobs.utils.CustomConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BoostManager {

    private final SkyRiseJobs plugin;
    private final CustomConfig config;
    private final File dataFile;

    private boolean enabled;
    private String prefix;
    private String permission;
    private String adminPermission;
    private double cost;
    private String currencySymbol;
    private long durationMillis;
    private long cooldownMillis;
    private boolean allowPurchaseWhileActive;
    private double moneyMultiplier;
    private double expMultiplier;
    private boolean bossBarEnabled;
    private String bossBarTitle;
    private BarColor bossBarColor;
    private BarStyle bossBarStyle;
    private BossBar bossBar;
    private BukkitTask bossBarTask;
    private Map<String, String> messages = new HashMap<>();

    private long activeUntil;
    private UUID activatedBy;
    private String activatedByName;
    private final Map<UUID, Long> lastPurchases = new HashMap<>();

    public BoostManager(SkyRiseJobs plugin) {
        this.plugin = plugin;
        this.config = new CustomConfig(plugin, "boosts", "config.yml");
        this.dataFile = ModuleSupport.featureFile(plugin, "boosts", "data.yml");
    }

    public void load() {
        config.load();
        cacheConfig();
        loadData();
        startBossBarIfActive();
    }

    public void reload() {
        saveData();
        stopBossBar();
        config.reload();
        cacheConfig();
        loadData();
        startBossBarIfActive();
    }

    public void shutdown() {
        stopBossBar();
        saveData();
    }

    private void cacheConfig() {
        FileConfiguration c = config.getConfig();
        enabled = c.getBoolean("enabled", true);
        prefix = c.getString("prefix", "<#36d1ff><bold>BOOST</bold> <gray>» <white>");
        permission = c.getString("permission", "skyrisejobs.boost.elite");
        adminPermission = c.getString("admin-permission", "skyrisejobs.boost.admin");
        cost = Math.max(0.0, c.getDouble("cost", 10000.0));
        currencySymbol = c.getString("currency-symbol", "$");
        durationMillis = Math.max(1L, c.getLong("duration-minutes", 60L)) * 60L * 1000L;
        cooldownMillis = Math.max(0L, c.getLong("cooldown-minutes", 720L)) * 60L * 1000L;
        allowPurchaseWhileActive = c.getBoolean("allow-purchase-while-active", false);
        moneyMultiplier = Math.max(1.0, c.getDouble("money-multiplier", 1.5));
        expMultiplier = Math.max(1.0, c.getDouble("exp-multiplier", 1.5));
        bossBarEnabled = c.getBoolean("bossbar.enabled", true);
        bossBarTitle = c.getString("bossbar.title", "<#36d1ff><bold>BOOST PRAC</bold> <gray>» <white>{time} <gray>| <white>Zarobki x{money_multiplier} EXP x{exp_multiplier}");
        bossBarColor = parseBarColor(c.getString("bossbar.color", "BLUE"));
        bossBarStyle = parseBarStyle(c.getString("bossbar.style", "SOLID"));

        messages = new HashMap<>();
        ConfigurationSection section = c.getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false)) messages.put(key, section.getString(key, ""));
        }
    }

    private void loadData() {
        ensureDataFile();
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        activeUntil = data.getLong("active.until", 0L);
        String uuid = data.getString("active.by-uuid", null);
        activatedBy = null;
        if (uuid != null) {
            try { activatedBy = UUID.fromString(uuid); } catch (IllegalArgumentException ignored) {}
        }
        activatedByName = data.getString("active.by-name", "System");
        lastPurchases.clear();
        ConfigurationSection cooldowns = data.getConfigurationSection("cooldowns");
        if (cooldowns != null) {
            for (String key : cooldowns.getKeys(false)) {
                try { lastPurchases.put(UUID.fromString(key), cooldowns.getLong(key)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void saveData() {
        ensureDataFile();
        FileConfiguration data = new YamlConfiguration();
        data.set("active.until", activeUntil);
        data.set("active.by-uuid", activatedBy != null ? activatedBy.toString() : null);
        data.set("active.by-name", activatedByName);
        for (Map.Entry<UUID, Long> entry : lastPurchases.entrySet()) {
            data.set("cooldowns." + entry.getKey(), entry.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać danych boostów: " + e.getMessage());
        }
    }

    private void ensureDataFile() {
        if (dataFile.exists()) return;
        try {
            File parent = dataFile.getParentFile();
            if (parent != null) parent.mkdirs();
            dataFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można utworzyć danych boostów: " + e.getMessage());
        }
    }

    public boolean purchase(Player player) {
        if (!enabled) {
            send(player, "disabled");
            return false;
        }
        if (!player.hasPermission(permission)) {
            send(player, "no-permission");
            return false;
        }
        if (!VaultHook.hasEconomy()) {
            send(player, "no-economy");
            return false;
        }
        if (isActive() && !allowPurchaseWhileActive) {
            send(player, "already-active", placeholders().replace("{time}", formatDuration(getRemainingMillis())));
            return false;
        }
        long cooldown = getCooldownRemaining(player.getUniqueId());
        if (cooldown > 0L) {
            send(player, "cooldown", placeholders().replace("{time}", formatDuration(cooldown)));
            return false;
        }
        if (!VaultHook.has(player, cost)) {
            send(player, "no-money");
            return false;
        }
        if (!VaultHook.withdraw(player, cost)) {
            send(player, "no-money");
            return false;
        }

        activate(player.getUniqueId(), player.getName(), durationMillis);
        lastPurchases.put(player.getUniqueId(), System.currentTimeMillis());
        saveData();
        send(player, "purchased");
        broadcastActivation();
        return true;
    }

    public void forceStart(CommandSender sender, long minutes) {
        if (!sender.hasPermission(adminPermission)) {
            send(sender, "admin-no-permission");
            return;
        }
        activate(null, sender.getName(), Math.max(1L, minutes) * 60L * 1000L);
        saveData();
        send(sender, "forced-start");
        broadcastActivation();
    }

    public void forceStop(CommandSender sender) {
        if (!sender.hasPermission(adminPermission)) {
            send(sender, "admin-no-permission");
            return;
        }
        activeUntil = 0L;
        activatedBy = null;
        activatedByName = null;
        stopBossBar();
        saveData();
        send(sender, "forced-stop");
    }

    private void activate(UUID uuid, String name, long duration) {
        activeUntil = System.currentTimeMillis() + Math.max(1L, duration);
        activatedBy = uuid;
        activatedByName = name != null ? name : "System";
        startBossBarIfActive();
    }

    private void broadcastActivation() {
        Bukkit.broadcast(ColorUtil.mini(replace(messages.getOrDefault("broadcast", "{prefix}<white>Globalny boost aktywowany.</white>"), placeholders())));
    }

    private void startBossBarIfActive() {
        stopBossBar();
        if (!bossBarEnabled || !isActive()) return;
        bossBar = Bukkit.createBossBar("", bossBarColor, bossBarStyle);
        bossBar.setVisible(true);
        bossBarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBar, 0L, 20L);
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        if (!isActive()) {
            stopBossBar();
            return;
        }
        String title = replace(bossBarTitle, placeholders());
        bossBar.setTitle(ColorUtil.legacyColor(title));
        long total = Math.max(1L, durationMillis);
        double progress = Math.max(0.0, Math.min(1.0, (double) getRemainingMillis() / total));
        bossBar.setProgress(progress);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!bossBar.getPlayers().contains(player)) bossBar.addPlayer(player);
        }
    }

    private void stopBossBar() {
        if (bossBarTask != null) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }

    public boolean isActive() {
        return enabled && activeUntil > System.currentTimeMillis();
    }

    public double applyMoney(double base) {
        return isActive() ? base * moneyMultiplier : base;
    }

    public double applyExp(double base) {
        return isActive() ? base * expMultiplier : base;
    }

    public double getMoneyMultiplier() { return isActive() ? moneyMultiplier : 1.0; }
    public double getExpMultiplier() { return isActive() ? expMultiplier : 1.0; }
    public long getRemainingMillis() { return Math.max(0L, activeUntil - System.currentTimeMillis()); }
    public String getPermission() { return permission; }
    public String getAdminPermission() { return adminPermission; }

    public long getCooldownRemaining(UUID uuid) {
        Long last = lastPurchases.get(uuid);
        if (last == null || cooldownMillis <= 0L) return 0L;
        return Math.max(0L, cooldownMillis - (System.currentTimeMillis() - last));
    }

    public void sendStatus(CommandSender sender) {
        send(sender, isActive() ? "status-active" : "status-inactive");
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, placeholders());
    }

    private void send(CommandSender sender, String key, String placeholderString) {
        String raw = messages.getOrDefault(key, key);
        sender.sendMessage(ColorUtil.mini(replace(raw, placeholderString)));
    }

    private String placeholders() {
        return "{prefix}=" + prefix
                + "\n{cost}=" + String.format(Locale.US, "%.2f", cost)
                + "\n{currency}=" + currencySymbol
                + "\n{player}=" + (activatedByName != null ? activatedByName : "System")
                + "\n{duration}=" + formatDuration(durationMillis)
                + "\n{time}=" + formatDuration(getRemainingMillis())
                + "\n{money_multiplier}=" + formatDouble(moneyMultiplier)
                + "\n{exp_multiplier}=" + formatDouble(expMultiplier);
    }

    private String replace(String raw, String placeholders) {
        String result = raw == null ? "" : raw;
        for (String line : placeholders.split("\n")) {
            int index = line.indexOf('=');
            if (index > 0) result = result.replace(line.substring(0, index), line.substring(index + 1));
        }
        return result;
    }

    private BarColor parseBarColor(String raw) {
        try { return BarColor.valueOf(raw == null ? "BLUE" : raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return BarColor.BLUE; }
    }

    private BarStyle parseBarStyle(String raw) {
        try { return BarStyle.valueOf(raw == null ? "SOLID" : raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return BarStyle.SOLID; }
    }

    private String formatDouble(double value) {
        return String.format(Locale.US, value == Math.rint(value) ? "%.0f" : "%.2f", value);
    }

    public String formatDuration(long millis) {
        if (millis <= 0) return "0m";
        long minutes = (long) Math.ceil(millis / 60000.0);
        long days = minutes / 1440;
        long hours = (minutes % 1440) / 60;
        long mins = minutes % 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }
}
