package pl.skyrise.skyRiseJobs.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.core.ModuleSupport;
import pl.skyrise.skyRiseJobs.utils.CustomConfig;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Wygodna baza dla przyszłych prac.
 *
 * Każda praca dostaje automatycznie:
 * - config: plugins/SkyRiseJobs/config/<jobId>/config.yml
 * - dane:   plugins/SkyRiseJobs/features/<jobId>/
 */
public abstract class AbstractJobModule implements JobModule {

    protected final SkyRiseJobs plugin;
    protected final String jobId;
    protected final CustomConfig config;
    protected final File featureFolder;

    protected AbstractJobModule(SkyRiseJobs plugin, String jobId) {
        this.plugin = plugin;
        this.jobId = sanitize(jobId);
        this.config = new CustomConfig(plugin, this.jobId, "config.yml");
        this.featureFolder = ModuleSupport.featureFolder(plugin, this.jobId);
    }

    @Override
    public final String getName() {
        return jobId;
    }

    @Override
    public final String getJobId() {
        return jobId;
    }

    @Override
    public void onEnable() {
        featureFolder.mkdirs();
        config.load();
        onJobEnable();
    }

    @Override
    public void onDisable() {
        onJobDisable();
        config.save();
    }

    @Override
    public void onReload() {
        config.reload();
        onJobReload();
    }

    protected void onJobEnable() {}
    protected void onJobDisable() {}
    protected void onJobReload() {}

    public CustomConfig getJobConfig() {
        return config;
    }

    public File getFeatureFolder() {
        return featureFolder;
    }

    public File dataFile(String fileName) {
        return ModuleSupport.featureFile(plugin, jobId, fileName);
    }

    @Override
    public abstract String getDisplayName();

    @Override
    public abstract List<String> getDescription();

    @Override
    public abstract ItemStack getIcon(Player player);

    @Override
    public boolean isAvailable(Player player) {
        return plugin.getLevelManager().canAccessJob(player, this) && hasJobAccess(player);
    }

    /**
     * Dodatkowe warunki konkretnej pracy (permisje, stan questa, region itp.).
     * Poziom postaci jest sprawdzany centralnie wyżej.
     */
    protected boolean hasJobAccess(Player player) {
        return true;
    }

    /**
     * Metoda DRY do przyznawania expa postaci z uwzględnieniem globalnego boosta.
     */
    protected pl.skyrise.skyRiseJobs.core.level.LevelAddResult addCharacterExp(Player player, double baseExp) {
        return plugin.getLevelManager().addExperience(player, baseExp);
    }

    /**
     * Metoda DRY do przeliczania wypłaty z uwzględnieniem globalnego boosta.
     */
    protected double applyMoneyBoost(double baseMoney) {
        return plugin.getBoostManager().applyMoney(baseMoney);
    }

    @Override
    public abstract void openMenu(Player player);

    private static String sanitize(String value) {
        String sanitized = value == null ? "job" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return sanitized.isBlank() ? "job" : sanitized;
    }
}
